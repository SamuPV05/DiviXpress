package com.samuel.retodivisas.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samuel.retodivisas.data.AppUser
import com.samuel.retodivisas.data.FirestoreTransactionRepository
import com.samuel.retodivisas.data.MediaRepository
import com.samuel.retodivisas.data.ROLE_ADMIN
import com.samuel.retodivisas.data.RatesRepository
import com.samuel.retodivisas.data.SessionManager
import com.samuel.retodivisas.data.TransactionModel
import com.samuel.retodivisas.data.TransactionRepository
import com.samuel.retodivisas.hardware.VibrationHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val CURRENCIES = listOf("COP", "EUR", "GBP", "JPY")

data class OpsUiState(
    val currency: String = "COP",
    val rate: Double? = null,
    val loadingRate: Boolean = false,
    val executing: Boolean = false,
    val amount: String = "",
    val agreedRate: String = "",
    val photoBitmap: Bitmap? = null,
    val error: String? = null,
    val message: String? = null,
    val auditAlert: Boolean = false,
    val isAdmin: Boolean = SessionManager.isAdmin
)

class OperationsViewModel(app: Application) : AndroidViewModel(app) {
    private val firestoreRepo = FirestoreTransactionRepository()
    private val txRepo = TransactionRepository(firestoreRepo)
    private val ratesRepo = RatesRepository()
    private val mediaRepo = MediaRepository()
    private val vibrationHelper = VibrationHelper(app)

    private val _state = MutableStateFlow(OpsUiState(isAdmin = SessionManager.isAdmin))
    val state: StateFlow<OpsUiState> = _state.asStateFlow()

    private val userUid: String? get() = SessionManager.currentUser?.uid

    val history: Flow<List<TransactionModel>> =
        userUid?.let { txRepo.observeByUser(it) } ?: emptyFlow()

    val balance: Flow<Double> =
        userUid?.let { uid ->
            firestoreRepo.observeUser(uid).map { u: AppUser? ->
                _state.update { it.copy(isAdmin = u?.role == ROLE_ADMIN) }
                u?.balanceUsd ?: 0.0
            }
        } ?: emptyFlow()

    private var rateJob: Job? = null
    private var requestId = 0

    init { loadRate() }

    fun selectCurrency(currency: String) {
        _state.update { it.copy(currency = currency, error = null, message = null) }
        loadRate()
    }

    fun onAmountChange(value: String) {
        _state.update { it.copy(amount = value, error = null, message = null) }
    }

    fun onAgreedRateChange(value: String) {
        _state.update { it.copy(agreedRate = value, error = null, message = null) }
    }

    fun useMarketRateAsAgreed() {
        val currentRate = _state.value.rate ?: return
        _state.update { it.copy(agreedRate = currentRate.toString(), error = null, message = null) }
    }

    fun onPhotoCaptured(bitmap: Bitmap) {
        vibrationHelper.vibrateClick()
        _state.update { it.copy(photoBitmap = bitmap) }
    }

    fun onPhotoSelectedFromGallery(uri: Uri) {
        viewModelScope.launch {
            val bmp = withContext(Dispatchers.IO) {
                val ctx = getApplication<Application>()
                ctx.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
            if (bmp != null) {
                vibrationHelper.vibrateClick()
                _state.update { it.copy(photoBitmap = bmp) }
            }
        }
    }

    fun removePhoto() {
        vibrationHelper.vibrateClick()
        _state.update { it.copy(photoBitmap = null) }
    }

    fun dismissAuditAlert() {
        _state.update { it.copy(auditAlert = false) }
    }

    fun loadRate() {
        val id = ++requestId
        val currency = _state.value.currency
        rateJob?.cancel()
        _state.update { it.copy(loadingRate = true, rate = null, error = null) }
        rateJob = viewModelScope.launch {
            try {
                val r = ratesRepo.getRate(currency)
                if (id == requestId) _state.update { it.copy(rate = r, loadingRate = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (id == requestId) _state.update {
                    it.copy(loadingRate = false, error = "No se pudo obtener la tasa desde Finnhub. Revisa tu conexión.")
                }
            }
        }
    }

    fun execute(type: String) {
        val uid = userUid
        if (uid == null) {
            _state.update { it.copy(error = "Sesión no válida") }
            return
        }
        val s = _state.value
        if (s.executing) return

        val amount = s.amount.replace(',', '.').toDoubleOrNull()
        val displayedRate = s.rate
        val agreedInput = s.agreedRate.trim()
        val agreedRate = if (agreedInput.isEmpty()) displayedRate else agreedInput.replace(',', '.').toDoubleOrNull()
        when {
            displayedRate == null -> {
                vibrationHelper.vibrateError()
                _state.update { it.copy(error = "Aún no hay una tasa disponible") }
                return
            }
            amount == null || amount <= 0.0 -> {
                vibrationHelper.vibrateError()
                _state.update { it.copy(error = "Ingresa una cantidad válida en USD") }
                return
            }
            agreedRate == null || agreedRate <= 0.0 -> {
                vibrationHelper.vibrateError()
                _state.update { it.copy(error = "Ingresa una tasa pactada válida") }
                return
            }
        }

        viewModelScope.launch {
            _state.update { it.copy(executing = true, error = null, message = null) }
            val marketRate = try {
                ratesRepo.getRate(s.currency)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                vibrationHelper.vibrateError()
                _state.update {
                    it.copy(
                        executing = false,
                        error = "No se pudo verificar la tasa de mercado en Finnhub. Operación cancelada."
                    )
                }
                return@launch
            }

            val photoPath = s.photoBitmap?.let { bmp ->
                mediaRepo.uploadBitmap(
                    context = getApplication(),
                    bitmap = bmp,
                    folder = "transactions",
                    prefix = "tx_$uid"
                )
            }

            txRepo.execute(uid, s.currency, amount, agreedRate, marketRate, type, photoPath)
                .onSuccess { tx ->
                    if (tx.requiresAudit) {
                        vibrationHelper.vibrateWaveform(
                            longArrayOf(0, 300, 100, 500, 100, 300),
                            intArrayOf(0, 255, 0, 255, 0, 255)
                        )
                    } else {
                        vibrationHelper.vibrateSuccess()
                    }

                    _state.update {
                        it.copy(
                            executing = false,
                            amount = "",
                            agreedRate = "",
                            rate = marketRate,
                            photoBitmap = null,
                            error = null,
                            message = if (tx.requiresAudit) null else "$type registrada con éxito",
                            auditAlert = tx.requiresAudit
                        )
                    }
                }
                .onFailure { e ->
                    vibrationHelper.vibrateError()
                    _state.update {
                        it.copy(
                            executing = false,
                            error = e.message ?: "Error al registrar",
                            message = null
                        )
                    }
                }
        }
    }
}

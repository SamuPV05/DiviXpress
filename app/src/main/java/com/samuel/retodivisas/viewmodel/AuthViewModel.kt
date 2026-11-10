package com.samuel.retodivisas.viewmodel

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samuel.retodivisas.data.AuthRepository
import com.samuel.retodivisas.data.ROLE_USER
import com.samuel.retodivisas.data.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val isCheckingSession: Boolean = false,
    val hasActiveSession: Boolean = SessionManager.currentUser != null
)

class AuthViewModel(app: Application) : AndroidViewModel(app) {
    private val authRepo = AuthRepository()
    private val _state = MutableStateFlow(AuthUiState(hasActiveSession = SessionManager.currentUser != null))
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun clearError() { _state.update { it.copy(error = null) } }

    fun checkActiveSession(onSessionActive: () -> Unit) {
        if (!authRepo.hasFirebaseUser()) {
            _state.update { it.copy(isCheckingSession = false, hasActiveSession = false) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isCheckingSession = true, loading = true) }
            try {
                val user = authRepo.checkAndLoadActiveSession()
                if (user != null) {
                    _state.update { it.copy(isCheckingSession = false, loading = false, hasActiveSession = true) }
                    onSessionActive()
                } else {
                    _state.update { it.copy(isCheckingSession = false, loading = false, hasActiveSession = false) }
                }
            } catch (_: Exception) {
                _state.update { it.copy(isCheckingSession = false, loading = false, hasActiveSession = false) }
            }
        }
    }

    private fun validate(email: String, password: String, confirm: String? = null): String? = when {
        email.isBlank() || password.isBlank() || (confirm != null && confirm.isBlank()) ->
            "Completa todos los campos"
        !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Correo no válido"
        password.length < 6 -> "La contraseña debe tener al menos 6 caracteres"
        confirm != null && confirm != password -> "Las contraseñas no coinciden"
        else -> null
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        validate(email, password)?.let { _state.update { s -> s.copy(error = it) }; return }
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            authRepo.login(email, password)
                .onSuccess {
                    _state.update { it.copy(loading = false, error = null, hasActiveSession = true) }
                    onSuccess()
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = e.message ?: "Correo o contraseña incorrectos") }
                }
        }
    }

    fun register(
        email: String,
        password: String,
        confirm: String,
        role: String = ROLE_USER,
        onSuccess: () -> Unit
    ) {
        validate(email, password, confirm)?.let { _state.update { s -> s.copy(error = it) }; return }
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            authRepo.register(email, password, role)
                .onSuccess {
                    _state.update { it.copy(loading = false, error = null, hasActiveSession = true) }
                    onSuccess()
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = e.message ?: "Error al registrar") }
                }
        }
    }
}

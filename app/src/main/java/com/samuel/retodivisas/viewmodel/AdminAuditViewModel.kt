package com.samuel.retodivisas.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.samuel.retodivisas.data.FirestoreTransactionRepository
import com.samuel.retodivisas.data.SessionManager
import com.samuel.retodivisas.data.TransactionModel
import com.samuel.retodivisas.data.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

enum class AuditFilter {
    ALL,
    FLAGGED_ONLY,
    NORMAL_ONLY
}

data class AdminAuditUiState(
    val filter: AuditFilter = AuditFilter.ALL,
    val isAdmin: Boolean = SessionManager.isAdmin
)

class AdminAuditViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = TransactionRepository(FirestoreTransactionRepository())
    private val allTransactionsFlow: Flow<List<TransactionModel>> = repo.observeAll()

    private val _uiState = MutableStateFlow(AdminAuditUiState(isAdmin = SessionManager.isAdmin))
    val uiState: StateFlow<AdminAuditUiState> = _uiState.asStateFlow()

    val filteredRows: Flow<List<TransactionModel>> = combine(allTransactionsFlow, _uiState) { list, state ->
        when (state.filter) {
            AuditFilter.ALL -> list
            AuditFilter.FLAGGED_ONLY -> list.filter { it.requiresAudit }
            AuditFilter.NORMAL_ONLY -> list.filter { !it.requiresAudit }
        }
    }

    val totalRows: Flow<List<TransactionModel>> = allTransactionsFlow

    fun setFilter(filter: AuditFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    fun logout() {
        SessionManager.logout()
    }
}

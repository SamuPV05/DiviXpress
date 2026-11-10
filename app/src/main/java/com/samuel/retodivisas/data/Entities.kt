package com.samuel.retodivisas.data

const val ROLE_ADMIN = "Admin"
const val ROLE_USER = "User"
const val TYPE_BUY = "Compra"
const val TYPE_SELL = "Venta"
const val AUDIT_THRESHOLD_PERCENT = 2.5

/** Resultado de auditar una operación contra el mercado. */
data class AuditResult(val deviationPercent: Double, val requiresAudit: Boolean)

/**
 * Regla de negocio crítica: una operación requiere auditoría si el valor pactado
 * se desvía más de [AUDIT_THRESHOLD_PERCENT] % del valor de mercado de Finnhub.
 */
object AuditRule {
    fun evaluate(agreedRate: Double, marketRate: Double): AuditResult {
        require(marketRate > 0.0) { "La tasa de mercado debe ser mayor que 0" }
        val raw = kotlin.math.abs(agreedRate - marketRate) / marketRate * 100.0
        return AuditResult(
            deviationPercent = kotlin.math.round(raw * 100.0) / 100.0,
            requiresAudit = raw > AUDIT_THRESHOLD_PERCENT
        )
    }
}

data class AppUser(
    val uid: String = "",
    val email: String = "",
    val role: String = ROLE_USER,
    val balanceUsd: Double = 10000.0,
    val photoPath: String? = null
)

data class TransactionModel(
    val id: String = "",
    val userId: String = "",
    val currencyPair: String = "",
    val amount: Double = 0.0,
    /** Tasa pactada (se mantiene el nombre por compatibilidad con documentos antiguos). */
    val exchangeRate: Double = 0.0,
    /** Tasa pactada por el usuario en la operación. */
    val agreedRate: Double = 0.0,
    /** Tasa de mercado de Finnhub consultada al momento exacto de la operación. */
    val marketRate: Double = 0.0,
    val transactionType: String = "",
    val timestamp: Long = 0L,
    val requiresAudit: Boolean = false,
    val deviationPercent: Double = 0.0,
    val photoPath: String? = null,
    val email: String = ""
)

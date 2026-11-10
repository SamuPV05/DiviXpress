package com.samuel.retodivisas.data

import com.samuel.retodivisas.network.FinnhubApiService
import com.samuel.retodivisas.network.FinnhubClient
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val firestoreRepo: FirestoreTransactionRepository = FirestoreTransactionRepository()
) {
    fun observeByUser(uid: String): Flow<List<TransactionModel>> =
        firestoreRepo.observeTransactionsByUser(uid)

    fun observeAll(): Flow<List<TransactionModel>> =
        firestoreRepo.observeAllTransactions()

    suspend fun execute(
        uid: String,
        currency: String,
        amountUsd: Double,
        agreedRate: Double,
        marketRate: Double,
        type: String,
        photoPath: String? = null
    ): Result<TransactionModel> =
        firestoreRepo.executeTransaction(uid, currency, amountUsd, agreedRate, marketRate, type, photoPath)
}

class RatesRepository(
    private val finnhubApi: FinnhubApiService = FinnhubClient.instance,
) {
    suspend fun getRate(currency: String): Double {
        runCatching {
            val forexResp = finnhubApi.getForexRates(base = "USD")
            val directRate = forexResp.quote?.get(currency)
            if (directRate != null && directRate > 0.0) {
                return kotlin.math.round(directRate * 10000.0) / 10000.0
            }
        }

        val rate = when (currency.uppercase()) {
            "COP" -> {
                val q = finnhubApi.getRealTimeQuote("BINANCE:USDTCOP")
                q.c ?: throw IllegalStateException("Finnhub no devolvió cotización para USD/COP")
            }
            "EUR" -> {
                val q = finnhubApi.getRealTimeQuote("BINANCE:EURUSDT")
                val eurUsd = q.c ?: throw IllegalStateException("Finnhub no devolvió cotización para USD/EUR")
                if (eurUsd <= 0.0) throw IllegalStateException("Cotización inválida de Finnhub para EUR")
                1.0 / eurUsd
            }
            "GBP" -> {
                val q = finnhubApi.getRealTimeQuote("BINANCE:GBPUSDT")
                val gbpUsd = q.c ?: throw IllegalStateException("Finnhub no devolvió cotización para USD/GBP")
                if (gbpUsd <= 0.0) throw IllegalStateException("Cotización inválida de Finnhub para GBP")
                1.0 / gbpUsd
            }
            "JPY" -> {
                val q = finnhubApi.getRealTimeQuote("FXY")
                val fxyPrice = q.c ?: throw IllegalStateException("Finnhub no devolvió cotización para USD/JPY")
                if (fxyPrice <= 0.0) throw IllegalStateException("Cotización inválida de Finnhub para JPY")
                100.0 / fxyPrice
            }
            "BRL" -> {
                val q = finnhubApi.getRealTimeQuote("BINANCE:USDTBRL")
                q.c ?: throw IllegalStateException("Finnhub no devolvió cotización para USD/BRL")
            }
            "MXN" -> {
                val q = finnhubApi.getRealTimeQuote("BINANCE:USDTMXN")
                q.c ?: throw IllegalStateException("Finnhub no devolvió cotización para USD/MXN")
            }
            else -> throw IllegalArgumentException("Divisa no soportada: $currency")
        }

        if (rate <= 0.0) throw IllegalStateException("Tasa no disponible en Finnhub para $currency")
        return kotlin.math.round(rate * 10000.0) / 10000.0
    }
}

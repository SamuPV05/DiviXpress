package com.samuel.retodivisas.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

const val FINNHUB_API_KEY = "db54nfhr01qv1n8sloa0db54nfhr01qv1n8sloag"

data class FinnhubForexResponse(
    val base: String? = null,
    val quote: Map<String, Double>? = null,
)

data class FinnhubQuoteResponse(
    val c: Double? = null,
    val d: Double? = null,
    val dp: Double? = null,
    val h: Double? = null,
    val l: Double? = null,
    val o: Double? = null,
    val pc: Double? = null,
    val t: Long? = null,
)

interface FinnhubApiService {
    @GET("forex/rates")
    suspend fun getForexRates(
        @Query("base") base: String = "USD",
        @Query("token") token: String = FINNHUB_API_KEY,
    ): FinnhubForexResponse

    @GET("quote")
    suspend fun getRealTimeQuote(
        @Query("symbol") symbol: String,
        @Query("token") token: String = FINNHUB_API_KEY,
    ): FinnhubQuoteResponse
}

object FinnhubClient {
    val instance: FinnhubApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://finnhub.io/api/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FinnhubApiService::class.java)
    }
}

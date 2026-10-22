package com.example.stockmarket.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// What the server sends. The names must match the JSON keys exactly.
data class CompanyDto(
    val symbol: String,
    val name: String,
    val sector: String,
    val price: Double,
    val changePct: Double,
    val updatedAt: String,
    val history: List<Double>
)

object ApiConfig {

    const val BASE_URL = "https://raw.githubusercontent.com/singinilumbani/mse-mock-api/refs/heads/main/api/v1/"
}

interface MseApi {
    // The path is added to BASE_URL
    @GET("companies.json")
    suspend fun getCompanies(): List<CompanyDto>
}

object Api {
    private val client = OkHttpClient.Builder()
        // BASIC prints one line per request and one per answer in Logcat
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    // "by lazy" builds Retrofit the first time it's used. A wrong address now throws an
    // exception we can catch, instead of crashing the app while it starts.
    val service: MseApi by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MseApi::class.java)
    }
}

// Turns the server's shape into the shape our app uses
fun CompanyDto.toCompany() = Company(
    symbol = symbol,
    name = name,
    sector = sector,
    price = price,
    changePct = changePct,
    history = history
)


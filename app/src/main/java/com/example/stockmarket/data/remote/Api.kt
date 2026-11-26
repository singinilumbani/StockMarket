package com.example.stockmarket.data.remote

import com.example.stockmarket.data.model.Company
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import com.example.stockmarket.data.repository.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import com.example.stockmarket.BuildConfig
import java.util.concurrent.TimeUnit
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
    // Comes from the build type: debug = your computer over USB, demo = the cloud server
    val BASE_URL: String = BuildConfig.API_BASE_URL
}

interface MseApi {
    // The path is added to BASE_URL
    @GET("companies")
    suspend fun getCompanies(): List<CompanyDto>
}

object Api {

    // Every request passes through here
    private class AuthInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()
            val token = SessionManager.currentToken()

            // 1) Add "Authorization: Bearer <token>" when we are logged in
            val request = if (token != null) {
                original.newBuilder().header("Authorization", "Bearer $token").build()
            } else {
                original
            }

            val response = chain.proceed(request)


            if (response.code == 401 && token != null && !original.url.encodedPath.contains("/auth/")) {
                SessionManager.onSessionExpired()
            }
            return response
        }
    }

    private val client = OkHttpClient.Builder()
        // A free cloud server can take about a minute to wake up, so the app has to be patient
        .connectTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(AuthInterceptor())
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
                redactHeader("Authorization")   // never print the token in Logcat
            }
        )
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val service: MseApi by lazy { retrofit.create(MseApi::class.java) }
    val auth: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
    val account: AccountApi by lazy { retrofit.create(AccountApi::class.java) }
    val kyc: KycApi by lazy { retrofit.create(KycApi::class.java) }
}

fun CompanyDto.toCompany() = Company(
    symbol = symbol,
    name = name,
    sector = sector,
    price = price,
    changePct = changePct,
    history = history
)
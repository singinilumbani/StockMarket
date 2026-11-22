package com.example.stockmarket.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// What the server sends. The names must match the server's JSON.
data class HoldingDto(val symbol: String, val quantity: Int, val avgCost: Double)

data class TransactionDto(
    val id: Int,
    val type: String,
    val symbol: String?,
    val quantity: Int,
    val price: Double,
    val date: String
)

data class OrderDto(
    val id: Int,
    val symbol: String,
    val side: String,
    val quantity: Int,
    val price: Double,
    val fees: Double,
    val status: String,
    val createdAt: String
)

data class AccountDto(
    val cash: Double,
    val holdings: List<HoldingDto>,
    val transactions: List<TransactionDto>,
    val orders: List<OrderDto>
)

data class PlaceOrderResponse(val orderId: Int, val account: AccountDto)

// What the app sends. There is no price and no total: the server decides those.
data class PlaceOrderRequest(val symbol: String, val side: String, val quantity: Int)
data class AmountRequest(val amount: Double)

interface AccountApi {
    @GET("account")
    suspend fun account(): AccountDto

    @POST("orders")
    suspend fun placeOrder(@Body request: PlaceOrderRequest): PlaceOrderResponse

    @POST("wallet/deposit")
    suspend fun deposit(@Body request: AmountRequest): AccountDto

    @POST("wallet/withdraw")
    suspend fun withdraw(@Body request: AmountRequest): AccountDto
}


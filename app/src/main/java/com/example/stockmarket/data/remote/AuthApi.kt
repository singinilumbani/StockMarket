package com.example.stockmarket.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// What the app sends to the server
data class RegisterRequest(val fullName: String, val identifier: String, val password: String)
data class LoginRequest(val identifier: String, val password: String)

// What the server answers. The names must match the server's JSON.
data class UserDto(val id: Int, val fullName: String, val identifier: String)
data class AuthResponse(val token: String, val user: UserDto)
data class ErrorResponse(val error: String)

interface AuthApi {

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    // Needs the token. The interceptor adds it automatically.
    @GET("me")
    suspend fun me(): UserDto
}

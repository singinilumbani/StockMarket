package com.example.stockmarket.data.remote

import retrofit2.HttpException
import java.io.IOException

fun errorMessageOf(error: Throwable): String {
    return when (error) {
        is HttpException -> {
            when (error.code()) {
                400 -> "Invalid request. Check your details."
                401 -> "Session expired. Please log in again."
                403 -> "You are not authorized."
                404 -> "Resource not found."
                500 -> "Server error. Please try again later."
                else -> "Request failed: ${error.code()}"
            }
        }

        is IOException ->
            "Unable to connect to the server. Check your connection."

        else ->
            error.message ?: "An unexpected error occurred."
    }
}


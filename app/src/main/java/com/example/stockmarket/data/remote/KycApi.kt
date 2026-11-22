package com.example.stockmarket.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

// The registration form. Every field has a default, so a missing value never becomes null.
data class KycDraftDto(
    val step: Int = 0,
    val fullName: String = "",
    val dateOfBirth: String = "",
    val address: String = "",
    val idType: String = "National ID",
    val idNumber: String = "",
    val idPhotoTaken: Boolean = false,
    val selfieTaken: Boolean = false,
    val payoutMethod: String = "Mobile money",
    val accountNumber: String = "",
    val riskAccepted: Boolean = false
)

data class KycDto(
    val status: String,
    val rejectionReason: String?,
    val submittedAt: String?,
    val draft: KycDraftDto
)

interface KycApi {
    @GET("kyc")
    suspend fun get(): KycDto

    @PUT("kyc/draft")
    suspend fun saveDraft(@Body draft: KycDraftDto): KycDto

    @POST("kyc/submit")
    suspend fun submit(): KycDto
}


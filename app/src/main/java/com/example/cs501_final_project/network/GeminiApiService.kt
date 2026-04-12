package com.example.cs501_final_project.network

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GeminiApiService {

    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}
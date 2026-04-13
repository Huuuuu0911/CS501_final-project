package com.example.cs501_final_project.data

import com.example.cs501_final_project.BuildConfig
import com.example.cs501_final_project.network.Content
import com.example.cs501_final_project.network.GeminiApiService
import com.example.cs501_final_project.network.GeminiRequest
import com.example.cs501_final_project.network.Part
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.HttpException

class GeminiRepository {

    private val api: GeminiApiService

    init {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        api = Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun askGemini(
        bodyPart: String,
        symptomText: String,
        age: String = "",
        gender: String = "",
        height: String = "",
        weight: String = "",
        address: String = ""
    ): String {
        val prompt = """
            You are a symptom triage assistant for a student demo app.
            Do not give a final diagnosis.
            Keep the answer short and clear.

            The user selected this body part: $bodyPart
            Symptom description: $symptomText
            Age: $age
            Gender: $gender
            Height: $height
            Weight: $weight
            Address: $address

            Please return:
            1. A short summary
            2. An urgency level: Emergency, Urgent Care, Primary Care, or Self Care
            3. 2 to 3 short follow-up questions
            4. A short next-step suggestion
            5. Warning signs that need immediate care
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt)
                    )
                )
            )
        )

        try {
            val response = api.generateContent(
                apiKey = BuildConfig.GEMINI_API_KEY,
                request = request
            )

            return response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?: "No response from Gemini"

        } catch (e: HttpException) {
            val response = e.response()
            val statusCode = response?.code()
            val errorBody = response?.errorBody()?.string()
            e.printStackTrace()
            return "HTTP $statusCode: $errorBody"

        } catch (e: Exception) {
            e.printStackTrace()
            return "Error: ${e.javaClass.simpleName}: ${e.message}"
        }
    }
}
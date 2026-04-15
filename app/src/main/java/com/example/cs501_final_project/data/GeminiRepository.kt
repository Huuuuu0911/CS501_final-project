package com.example.cs501_final_project.data

import com.example.cs501_final_project.BuildConfig
import com.example.cs501_final_project.network.Content
import com.example.cs501_final_project.network.GeminiApiService
import com.example.cs501_final_project.network.GeminiRequest
import com.example.cs501_final_project.network.Part
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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

    suspend fun getFollowUpQuestions(
        bodyPart: String,
        symptomText: String,
        painLevel: Int,
        patient: PatientContext,
        recentHistory: List<String>
    ): List<String> {
        val historyText = if (recentHistory.isEmpty()) {
            "No recent records."
        } else {
            recentHistory.joinToString("\n") { "- $it" }
        }

        val prompt = """
            You are generating intake questions for a polished symptom-support app.
            Goal: ask exactly 3 short, personalized follow-up questions before the final recommendation.
            Do not give the final assessment yet.
            Do not mention being an AI.
            Keep the wording simple, direct, and app-friendly.

            Answer style preferences:
            - yes/no
            - better / same / worse
            - today / 1-3 days / more than a week / not sure
            - mild / moderate / severe

            Patient:
            Name: ${patient.displayName}
            Group: ${patient.group}
            Age: ${patient.age}
            Gender: ${patient.gender}
            Height: ${patient.height}
            Weight: ${patient.weight}
            Address: ${patient.address}
            Allergies: ${patient.allergies}
            Current medications: ${patient.medications}
            Conditions: ${patient.conditions}

            Symptom input:
            Body area: $bodyPart
            Symptom text: $symptomText
            Pain level 0-10: $painLevel

            Recent symptom history:
            $historyText

            Rules:
            - Tailor the 3 questions to the body area, age, conditions, medications, and allergies
            - Prioritize red flags relevant to the case
            - Make the questions easy to answer in a mobile app
            - Avoid repeating the same idea

            Return exactly this format:
            QUESTION_1: ...
            QUESTION_2: ...
            QUESTION_3: ...
        """.trimIndent()

        val text = requestText(prompt)
        if (text.isBlank()) {
            return listOf(
                "Has it been getting worse?",
                "When did it start?",
                "Do you have any other symptoms?"
            )
        }

        val q1 = extractSingleLine(text, "QUESTION_1").ifBlank { "Has it been getting worse?" }
        val q2 = extractSingleLine(text, "QUESTION_2").ifBlank { "When did it start?" }
        val q3 = extractSingleLine(text, "QUESTION_3").ifBlank { "Do you have any other symptoms?" }

        return listOf(q1, q2, q3)
    }

    suspend fun askGeminiFinal(
        bodyPart: String,
        symptomText: String,
        painLevel: Int,
        followUpAnswers: List<String>,
        patient: PatientContext,
        recentHistory: List<String>
    ): String {
        val answersText = followUpAnswers.joinToString("\n") { "- $it" }
        val historyText = if (recentHistory.isEmpty()) {
            "No recent records."
        } else {
            recentHistory.joinToString("\n") { "- $it" }
        }

        val prompt = """
            You are a conservative symptom-support assistant for a mobile demo app.
            This is NOT a diagnosis tool.
            Do not say you are an AI.
            Write in a clear, practical, calm style.
            Keep bullets short.
            Only give general low-risk OTC suggestions. Do not give dosing.
            If there is any chance the person needs urgent evaluation, say so clearly.

            Patient:
            Name: ${patient.displayName}
            Group: ${patient.group}
            Age: ${patient.age}
            Gender: ${patient.gender}
            Height: ${patient.height}
            Weight: ${patient.weight}
            Address: ${patient.address}
            Allergies: ${patient.allergies}
            Current medications: ${patient.medications}
            Conditions: ${patient.conditions}

            Symptom input:
            Body area: $bodyPart
            Symptom description: $symptomText
            Pain level 0-10: $painLevel

            Follow-up answers:
            $answersText

            Recent symptom history:
            $historyText

            Return EXACTLY this format:

            URGENCY: one of [Emergency | Urgent Care | Primary Care | Self Care]
            CARE_LEVEL: one of [ER_NOW | URGENT_CARE | PRIMARY_CARE | BUY_OTC | REST_AT_HOME]
            PLACE_TYPE: one of [hospital | urgent care | primary care | pharmacy | none]
            MAP_QUERY: short Google Maps search query or none
            RECOMMENDATION_SCORE: integer from 1 to 5

            SUMMARY:
            one short sentence

            KEY_POINTS:
            - point 1
            - point 2
            - point 3

            SELF_CARE:
            - step 1
            - step 2
            - step 3

            OTC_OPTIONS:
            - option 1
            - option 2
            - option 3

            NEXT_STEPS:
            - step 1
            - step 2
            - step 3

            WARNING_SIGNS:
            - warning 1
            - warning 2
            - warning 3

            NOTES:
            one short paragraph only

            Rules:
            - Never skip a section
            - Never claim a diagnosis
            - OTC suggestions must be cautious and generic
            - If OTC options are not appropriate, write "Not appropriate without clinician or pharmacist guidance"
            - MAP_QUERY must match PLACE_TYPE
            - Recommendation score 5 means the app is very confident in the place/action category, not a medical certainty
        """.trimIndent()

        val text = requestText(prompt)
        return if (text.isBlank()) {
            structuredFallback(
                summary = "No response was received from the AI service.",
                keyPoints = listOf(
                    "The request completed without usable content",
                    "The service may be busy",
                    "Please try again"
                ),
                selfCare = listOf(
                    "Rest while you retry the request",
                    "Track any change in symptoms",
                    "Seek care sooner if symptoms become severe"
                ),
                otcOptions = listOf(
                    "Not appropriate without clinician or pharmacist guidance",
                    "Not appropriate without clinician or pharmacist guidance",
                    "Not appropriate without clinician or pharmacist guidance"
                ),
                nextSteps = listOf(
                    "Retry in a moment",
                    "Check the API key",
                    "Review network connection"
                ),
                notes = "The response body did not contain usable text."
            )
        } else {
            text
        }
    }

    private suspend fun requestText(prompt: String): String {
        val request = GeminiRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt)
                    )
                )
            )
        )

        repeat(2) { attempt ->
            try {
                val response = api.generateContent(
                    apiKey = BuildConfig.GEMINI_API_KEY,
                    request = request
                )

                val text = response.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text

                if (!text.isNullOrBlank()) {
                    return text
                }
            } catch (e: HttpException) {
                val statusCode = e.code()

                if (statusCode == 503 && attempt == 0) {
                    delay(1500)
                    return@repeat
                }

                return when (statusCode) {
                    400 -> structuredFallback(
                        summary = "The request format was not accepted.",
                        keyPoints = listOf(
                            "The API request may be malformed",
                            "Please review the input or prompt",
                            "Try again after checking setup"
                        ),
                        selfCare = listOf(
                            "Keep your symptom text clear and short",
                            "Retry after editing the input",
                            "Use manual care options if symptoms worsen"
                        ),
                        otcOptions = listOf(
                            "Not appropriate without clinician or pharmacist guidance",
                            "Not appropriate without clinician or pharmacist guidance",
                            "Not appropriate without clinician or pharmacist guidance"
                        ),
                        nextSteps = listOf(
                            "Check request format",
                            "Review API settings",
                            "Try again"
                        ),
                        notes = "The server returned HTTP 400."
                    )

                    401, 403 -> structuredFallback(
                        summary = "The API key or access setting may be incorrect.",
                        keyPoints = listOf(
                            "Authentication failed",
                            "The API key may be invalid",
                            "Access may not be enabled"
                        ),
                        selfCare = listOf(
                            "Do not rely on this result until the app setup is fixed",
                            "Use standard care options if symptoms worsen",
                            "Seek real-world care for urgent symptoms"
                        ),
                        otcOptions = listOf(
                            "Not appropriate without clinician or pharmacist guidance",
                            "Not appropriate without clinician or pharmacist guidance",
                            "Not appropriate without clinician or pharmacist guidance"
                        ),
                        nextSteps = listOf(
                            "Check your Gemini API key",
                            "Confirm the API is enabled",
                            "Try again after updating the key"
                        ),
                        notes = "The server returned HTTP $statusCode."
                    )

                    429 -> structuredFallback(
                        summary = "Too many requests were sent in a short time.",
                        keyPoints = listOf(
                            "Rate limit was reached",
                            "The service asked the app to slow down",
                            "This is usually temporary"
                        ),
                        selfCare = listOf(
                            "Wait a moment before retrying",
                            "Monitor whether symptoms are stable or worsening",
                            "Do not delay urgent care if warning signs appear"
                        ),
                        otcOptions = listOf(
                            "Not appropriate without clinician or pharmacist guidance",
                            "Not appropriate without clinician or pharmacist guidance",
                            "Not appropriate without clinician or pharmacist guidance"
                        ),
                        nextSteps = listOf(
                            "Wait a moment",
                            "Try again later",
                            "Reduce repeated requests"
                        ),
                        notes = "The server returned HTTP 429."
                    )

                    503 -> structuredFallback(
                        summary = "The AI service is temporarily busy.",
                        keyPoints = listOf(
                            "The Gemini service is under high demand",
                            "Your request did reach the server",
                            "This is usually temporary"
                        ),
                        selfCare = listOf(
                            "Keep tracking symptom changes",
                            "Use the map tab if you already know you need care",
                            "Escalate to urgent care if warning signs develop"
                        ),
                        otcOptions = listOf(
                            "Not appropriate without clinician or pharmacist guidance",
                            "Not appropriate without clinician or pharmacist guidance",
                            "Not appropriate without clinician or pharmacist guidance"
                        ),
                        nextSteps = listOf(
                            "Try again in a moment",
                            "Keep symptom text short and clear",
                            "Retry after a short pause"
                        ),
                        notes = "The server returned HTTP 503. This is usually a temporary service issue, not an app UI problem."
                    )

                    else -> structuredFallback(
                        summary = "The request could not be completed.",
                        keyPoints = listOf(
                            "The server returned an unexpected response",
                            "This may be temporary",
                            "Please try again"
                        ),
                        selfCare = listOf(
                            "Keep monitoring your symptoms",
                            "Use standard care escalation if needed",
                            "Do not wait if warning signs appear"
                        ),
                        otcOptions = listOf(
                            "Not appropriate without clinician or pharmacist guidance",
                            "Not appropriate without clinician or pharmacist guidance",
                            "Not appropriate without clinician or pharmacist guidance"
                        ),
                        nextSteps = listOf(
                            "Try again later",
                            "Check network connection",
                            "Review API setup if the issue continues"
                        ),
                        notes = "The server returned HTTP $statusCode."
                    )
                }
            } catch (e: Exception) {
                if (attempt == 0) {
                    delay(1000)
                    return@repeat
                }

                return structuredFallback(
                    summary = "The app could not reach the AI service.",
                    keyPoints = listOf(
                        "A network or runtime error happened",
                        "The request did not complete normally",
                        "Please try again"
                    ),
                    selfCare = listOf(
                        "If symptoms are stable, retry later",
                        "If symptoms are severe, use urgent care or emergency services",
                        "Track any new warning signs"
                    ),
                    otcOptions = listOf(
                        "Not appropriate without clinician or pharmacist guidance",
                        "Not appropriate without clinician or pharmacist guidance",
                        "Not appropriate without clinician or pharmacist guidance"
                    ),
                    nextSteps = listOf(
                        "Check internet connection",
                        "Try again later",
                        "Review app setup if it keeps happening"
                    ),
                    notes = "${e.javaClass.simpleName}: ${e.message}"
                )
            }
        }

        return structuredFallback(
            summary = "No response was received from the AI service.",
            keyPoints = listOf(
                "The request completed without usable content",
                "The service may be busy",
                "Please try again"
            ),
            selfCare = listOf(
                "Rest while you retry the request",
                "Track any change in symptoms",
                "Seek care sooner if symptoms become severe"
            ),
            otcOptions = listOf(
                "Not appropriate without clinician or pharmacist guidance",
                "Not appropriate without clinician or pharmacist guidance",
                "Not appropriate without clinician or pharmacist guidance"
            ),
            nextSteps = listOf(
                "Retry in a moment",
                "Check the API key",
                "Review network connection"
            ),
            notes = "The response body did not contain usable text."
        )
    }

    private fun extractSingleLine(text: String, key: String): String {
        return text.lines()
            .firstOrNull { it.trim().startsWith("$key:") }
            ?.substringAfter("$key:")
            ?.trim()
            .orEmpty()
    }

    private fun structuredFallback(
        urgency: String = "Primary Care",
        careLevel: String = "PRIMARY_CARE",
        placeType: String = "primary care",
        mapQuery: String = "primary care clinic near me",
        recommendationScore: Int = 3,
        summary: String,
        keyPoints: List<String>,
        selfCare: List<String>,
        otcOptions: List<String>,
        nextSteps: List<String>,
        warningSigns: List<String> = listOf(
            "Severe pain",
            "Trouble breathing",
            "Fainting"
        ),
        notes: String
    ): String {
        return """
            URGENCY: $urgency
            CARE_LEVEL: $careLevel
            PLACE_TYPE: $placeType
            MAP_QUERY: $mapQuery
            RECOMMENDATION_SCORE: $recommendationScore

            SUMMARY:
            $summary

            KEY_POINTS:
            - ${keyPoints.getOrElse(0) { "No key point" }}
            - ${keyPoints.getOrElse(1) { "No key point" }}
            - ${keyPoints.getOrElse(2) { "No key point" }}

            SELF_CARE:
            - ${selfCare.getOrElse(0) { "Rest and monitor symptoms" }}
            - ${selfCare.getOrElse(1) { "Use caution with new symptoms" }}
            - ${selfCare.getOrElse(2) { "Seek care if warning signs appear" }}

            OTC_OPTIONS:
            - ${otcOptions.getOrElse(0) { "Not appropriate without clinician or pharmacist guidance" }}
            - ${otcOptions.getOrElse(1) { "Not appropriate without clinician or pharmacist guidance" }}
            - ${otcOptions.getOrElse(2) { "Not appropriate without clinician or pharmacist guidance" }}

            NEXT_STEPS:
            - ${nextSteps.getOrElse(0) { "Try again later" }}
            - ${nextSteps.getOrElse(1) { "Check setup" }}
            - ${nextSteps.getOrElse(2) { "Review connection" }}

            WARNING_SIGNS:
            - ${warningSigns.getOrElse(0) { "Severe pain" }}
            - ${warningSigns.getOrElse(1) { "Trouble breathing" }}
            - ${warningSigns.getOrElse(2) { "Fainting" }}

            NOTES:
            $notes
        """.trimIndent()
    }
}

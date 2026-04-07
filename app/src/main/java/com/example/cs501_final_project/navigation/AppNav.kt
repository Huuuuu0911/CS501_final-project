package com.example.cs501_final_project.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cs501_final_project.ui.HomeScreen
import com.example.cs501_final_project.ui.MapScreen
import com.example.cs501_final_project.ui.ResultScreen
import com.example.cs501_final_project.ui.TriageScreen

// define routes for navigation
object Routes {
    const val HOME = "home"
    const val TRIAGE = "triage"
    const val RESULT = "result"
    const val MAP = "map"
}

@Composable
fun AppNav() {
    val navController = rememberNavController()

    // basic states to store user input
    val symptomState = remember { mutableStateOf("") }
    val painLevelState = remember { mutableStateOf(5f) }
    val durationState = remember { mutableStateOf("") }

    // result states
    val urgencyState = remember { mutableStateOf("Primary Care") }
    val recommendationState = remember { mutableStateOf("You can start with a primary care visit.") }

    // navigation graph
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onStartClick = {
                    navController.navigate(Routes.TRIAGE) // go to input screen
                }
            )
        }

        composable(Routes.TRIAGE) {
            TriageScreen(
                symptom = symptomState.value,
                onSymptomChange = { symptomState.value = it }, // update text input
                painLevel = painLevelState.value,
                onPainLevelChange = { painLevelState.value = it }, // update slider
                duration = durationState.value,
                onDurationChange = { durationState.value = it },
                onSubmitClick = {
                    // simple rule-based logic
                    val result = getTriageResult(
                        symptom = symptomState.value,
                        painLevel = painLevelState.value.toInt(),
                        duration = durationState.value
                    )
                    urgencyState.value = result.first
                    recommendationState.value = result.second

                    navController.navigate(Routes.RESULT)
                }
            )
        }

        composable(Routes.RESULT) {
            ResultScreen(
                urgency = urgencyState.value,
                recommendation = recommendationState.value,
                symptom = symptomState.value,
                painLevel = painLevelState.value.toInt(),
                duration = durationState.value,
                onFindCareClick = {
                    navController.navigate(Routes.MAP) // go to nearby care
                }
            )
        }

        composable(Routes.MAP) {
            MapScreen(
                urgency = urgencyState.value,
                onBackClick = {
                    navController.popBackStack() // go back
                }
            )
        }
    }
}

// simple triage logic (not real medical advice)
fun getTriageResult(
    symptom: String,
    painLevel: Int,
    duration: String
): Pair<String, String> {
    val text = symptom.lowercase()

    return when {
        // more serious symptoms
        text.contains("chest") || text.contains("shortness of breath") || painLevel >= 8 -> {
            Pair(
                "Emergency",
                "Your symptoms may need immediate attention. Please go to the ER or call emergency services if they get worse."
            )
        }

        // medium level
        text.contains("fever") || text.contains("vomit") || text.contains("infection") || painLevel >= 5 -> {
            Pair(
                "Urgent Care",
                "Your symptoms may need same-day care. An urgent care clinic is a good next step."
            )
        }

        // default case
        else -> {
            Pair(
                "Primary Care",
                "Your symptoms seem less urgent. You can start with a primary care visit."
            )
        }
    }
}
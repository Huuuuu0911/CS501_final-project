package com.example.cs501_final_project.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cs501_final_project.ui.components.AppButton
import com.example.cs501_final_project.ui.components.AppCard
import com.example.cs501_final_project.ui.theme.EmergencyRed
import com.example.cs501_final_project.ui.theme.SafeGreen
import com.example.cs501_final_project.ui.theme.UrgentOrange

fun getColorByUrgency(urgency: String): Color {
    return when (urgency) {
        "Emergency" -> EmergencyRed
        "Urgent Care" -> UrgentOrange
        else -> SafeGreen
    }
}

@Composable
fun ResultScreen(
    urgency: String,
    recommendation: String,
    symptom: String,
    painLevel: Int,
    duration: String,
    onFindCareClick: () -> Unit,
    onViewHistoryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Screen title
        Text(
            text = "Your Recommendation",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Summary card
        AppCard {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Care Level",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = urgency,
                    color = getColorByUrgency(urgency),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                )

                Text(
                    text = "Symptom: $symptom",
                    modifier = Modifier.padding(top = 4.dp)
                )

                Text(
                    text = "Pain Level: $painLevel",
                    modifier = Modifier.padding(top = 4.dp)
                )

                Text(
                    text = "Duration: $duration",
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Recommendation card
        AppCard {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Recommendation",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = recommendation,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // Disclaimer card
        AppCard {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Important Note",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "This app does not provide a medical diagnosis. If symptoms get worse, please seek professional care immediately.",
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // Action buttons card
        AppCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppButton(
                    text = "Find Nearby Care",
                    onClick = onFindCareClick
                )

                AppButton(
                    text = "View History",
                    onClick = onViewHistoryClick
                )
            }
        }
    }
}
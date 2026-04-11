package com.example.cs501_final_project.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cs501_final_project.ui.components.AppCard
import com.example.cs501_final_project.ui.components.AppButton
import com.example.cs501_final_project.ui.theme.EmergencyRed

@Composable
fun HomeScreen(
    onStartClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // App title
        Text(
            text = "CareRoute",
            style = MaterialTheme.typography.headlineMedium
        )

        // Subtitle
        Text(
            text = "Check your symptoms and get care suggestions",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Main action card
        AppCard {
            Column(modifier = Modifier.padding(16.dp)) {

                Text("Start Symptom Check")

                AppButton(
                    text = "Begin",
                    onClick = onStartClick
                )
            }
        }

        // History card
        AppCard {
            Column(modifier = Modifier.padding(16.dp)) {

                Text("History")

                AppButton(
                    text = "View History",
                    onClick = onHistoryClick
                )
            }
        }

        // Emergency warning card (very important for demo)
        AppCard {
            Column(modifier = Modifier.padding(16.dp)) {

                Text("Emergency")

                Text(
                    text = "If you have severe symptoms, call 911 immediately.",
                    color = EmergencyRed
                )
            }
        }
    }
}
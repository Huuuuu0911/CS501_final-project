package com.example.cs501_final_project.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement

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
            .padding(16.dp) // basic screen padding
    ) {
        // title
        Text(
            text = "Your Recommendation",
            style = MaterialTheme.typography.headlineSmall
        )

        // summary card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Urgency: $urgency",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Symptom: $symptom",
                    modifier = Modifier.padding(top = 8.dp)
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

        // recommendation text
        Text(
            text = recommendation,
            modifier = Modifier.padding(top = 20.dp)
        )

        // disclaimer
        Text(
            text = "This app does not provide a medical diagnosis. If symptoms get worse, please seek professional care immediately.",
            modifier = Modifier.padding(top = 16.dp)
        )

        Column(
            modifier = Modifier.padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // maps button
            Button(
                onClick = onFindCareClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Find Nearby Care")
            }

            // history button
            Button(
                onClick = onViewHistoryClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View History")
            }
        }
    }
}
package com.example.cs501_final_project.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// simple data model for facility
data class Facility(
    val name: String,
    val type: String
)

@Composable
fun MapScreen(
    urgency: String,
    onBackClick: () -> Unit
) {
    // choose facility list based on urgency
    val facilities = when (urgency) {
        "Emergency" -> listOf(
            Facility("Boston Medical Center", "Emergency Room"),
            Facility("Mass General Hospital", "Emergency Room"),
            Facility("City Hospital ER", "Emergency Room")
        )

        "Urgent Care" -> listOf(
            Facility("Downtown Urgent Care", "Urgent Care"),
            Facility("QuickCare Clinic", "Urgent Care"),
            Facility("Neighborhood Walk-in Clinic", "Urgent Care")
        )

        else -> listOf(
            Facility("Boston Primary Care", "Primary Care"),
            Facility("Community Health Center", "Primary Care"),
            Facility("Telehealth Access", "Virtual Care")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp) // outer padding
    ) {
        // screen title
        Text(
            text = "Nearby Care Options",
            style = MaterialTheme.typography.headlineSmall
        )

        // show current urgency level
        Text(
            text = "Recommended care level: $urgency",
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )

        // placeholder for map
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Map Preview",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "This is a placeholder for map integration.",
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // list of facilities
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(facilities) { facility ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = facility.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = facility.type,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // back button
        Button(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}
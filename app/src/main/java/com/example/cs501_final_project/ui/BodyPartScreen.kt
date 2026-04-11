package com.example.cs501_final_project.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cs501_final_project.ui.components.AppButton
import com.example.cs501_final_project.ui.components.AppCard

@Composable
fun BodyPartScreen(
    onBodyPartSelected: (String) -> Unit,
    onBackClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // Title
        Text(
            text = "Select Body Area",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Body part grid
        AppCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppButton("Head") { onBodyPartSelected("Head") }
                    AppButton("Chest") { onBodyPartSelected("Chest") }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppButton("Throat") { onBodyPartSelected("Throat") }
                    AppButton("Stomach") { onBodyPartSelected("Stomach") }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppButton("Arm") { onBodyPartSelected("Arm") }
                    AppButton("Leg") { onBodyPartSelected("Leg") }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppButton("Back") { onBodyPartSelected("Back") }
                    AppButton("Skin") { onBodyPartSelected("Skin") }
                }
            }
        }

        // Back button
        AppCard {
            Column(modifier = Modifier.padding(16.dp)) {
                AppButton(
                    text = "Back",
                    onClick = onBackClick
                )
            }
        }
    }
}
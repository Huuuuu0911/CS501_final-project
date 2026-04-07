package com.example.cs501_final_project.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TriageScreen(
    symptom: String,
    onSymptomChange: (String) -> Unit,
    painLevel: Float,
    onPainLevelChange: (Float) -> Unit,
    duration: String,
    onDurationChange: (String) -> Unit,
    onSubmitClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp) // basic layout padding
    ) {
        // title
        Text(
            text = "Symptom Check",
            style = MaterialTheme.typography.headlineSmall
        )

        // symptom input
        OutlinedTextField(
            value = symptom,
            onValueChange = onSymptomChange,
            label = { Text("Describe your symptom") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )

        // pain level text
        Text(
            text = "Pain Level: ${painLevel.toInt()}",
            modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
        )

        // pain slider
        Slider(
            value = painLevel,
            onValueChange = onPainLevelChange,
            valueRange = 0f..10f,
            steps = 9,
            modifier = Modifier.fillMaxWidth()
        )

        // duration input
        OutlinedTextField(
            value = duration,
            onValueChange = onDurationChange,
            label = { Text("How long have you had this symptom?") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
        )

        // submit button
        Button(
            onClick = onSubmitClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            Text("See Result")
        }
    }
}
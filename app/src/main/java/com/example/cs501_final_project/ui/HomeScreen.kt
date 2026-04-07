package com.example.cs501_final_project.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// simple home screen
@Composable
fun HomeScreen(onStartClick: () -> Unit) {
    Column(
        modifier = Modifier.Companion
            .fillMaxSize()
            .padding(16.dp), // basic padding
        verticalArrangement = Arrangement.Center
    ) {
        // app title
        Text(
            text = "CareRoute",
            style = MaterialTheme.typography.headlineMedium
        )

        // short description
        Text(
            text = "This app helps users choose the right level of care based on simple symptom information.",
            modifier = Modifier.Companion.padding(vertical = 16.dp)
        )

        // start button
        Button(
            onClick = onStartClick,
            modifier = Modifier.Companion.fillMaxWidth()
        ) {
            Text("Start Symptom Check")
        }
    }
}
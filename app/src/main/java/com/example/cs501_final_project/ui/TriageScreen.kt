package com.example.cs501_final_project.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.cs501_final_project.ui.components.AppButton
import com.example.cs501_final_project.ui.components.AppCard

@Composable
fun TriageScreen(
    symptom: String,
    onSymptomChange: (String) -> Unit,
    painLevel: Float,
    onPainLevelChange: (Float) -> Unit,
    duration: String,
    onDurationChange: (String) -> Unit,
    onVoiceResult: (String) -> Unit,
    onSubmitClick: () -> Unit
) {
    val context = LocalContext.current

    // Local message for permission or recognition result
    var voiceStatusText by remember { mutableStateOf("") }

    // Launcher for Android speech recognition
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.firstOrNull().orEmpty()

            if (spokenText.isNotBlank()) {
                onVoiceResult(spokenText)
                voiceStatusText = "Voice input added."
            } else {
                voiceStatusText = "No speech recognized."
            }
        } else {
            voiceStatusText = "Voice input cancelled."
        }
    }

    // Runtime permission request for microphone
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe your symptom")
            }
            speechLauncher.launch(intent)
        } else {
            voiceStatusText = "Microphone permission is required for voice input."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Screen title
        Text(
            text = "Symptom Check",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Symptom input card
        AppCard {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Describe Your Symptom",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = symptom,
                    onValueChange = onSymptomChange,
                    label = { Text("Type your symptom here") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )

                AppButton(
                    text = "Use Voice Input",
                    onClick = {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                )

                if (voiceStatusText.isNotBlank()) {
                    Text(
                        text = voiceStatusText,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // Pain level card
        AppCard {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Pain Level",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = painLevel.toInt().toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )

                Slider(
                    value = painLevel,
                    onValueChange = onPainLevelChange,
                    valueRange = 0f..10f,
                    steps = 9,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Duration card
        AppCard {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Duration",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = duration,
                    onValueChange = onDurationChange,
                    label = { Text("How long have you had this symptom?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
            }
        }

        // Submit card
        AppCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppButton(
                    text = "See Result",
                    onClick = onSubmitClick
                )
            }
        }
    }
}
package com.example.cs501_final_project.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun MapScreen(
    urgency: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    // choose a simple search keyword
    val query = when (urgency) {
        "Emergency" -> "hospital"
        "Urgent Care" -> "urgent care"
        else -> "primary care clinic"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), // outer padding
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // title
        Text(
            text = "Nearby Care Options",
            style = MaterialTheme.typography.headlineSmall
        )

        // current urgency
        Text(
            text = "Recommended care level: $urgency"
        )

        // simple info card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Google Maps Integration",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Tap the button below to open Google Maps and search for the recommended care type near you.",
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // open maps
        Button(
            onClick = {
                val uri = Uri.parse("geo:0,0?q=$query")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Open in Google Maps")
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
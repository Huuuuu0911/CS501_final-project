package com.example.cs501_final_project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.cs501_final_project.navigation.AppNav

// main entry point of the app
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // set up compose UI
        setContent {
            MaterialTheme {
                Surface {
                    AppNav() // load navigation graph
                }
            }
        }
    }
}
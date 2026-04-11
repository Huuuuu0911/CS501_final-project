package com.example.cs501_final_project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.example.cs501_final_project.navigation.AppNav
import com.example.cs501_final_project.ui.theme.AppTheme

// main entry point
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // load compose UI
        setContent {
            AppTheme {
                Surface {
                    AppNav()
                }
            }
        }
    }
}
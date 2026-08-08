package com.mtb.mtb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mtb.mtb.app.MainApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Draws edge-to-edge with system bar icon contrast following the current theme
        enableEdgeToEdge()

        setContent {
            // MainApp owns the theme (light/dark override lives with its other app-level state)
            MainApp()
        }
    }
}

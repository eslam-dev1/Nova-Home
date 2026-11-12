package com.example.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.ui.theme.NovaHomeTheme

class ReliabilityActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NovaHomeTheme {
                ReliabilityScreen(onBack = { finish() })
            }
        }
    }
}

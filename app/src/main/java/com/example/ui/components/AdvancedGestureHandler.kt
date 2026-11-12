package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun AdvancedGestureHandler(
    context: Context,
    onSwipeDown: () -> Unit,
    onSwipeUp: () -> Unit,
    onDoubleTap: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        onDoubleTap()
                        // Default action: open settings or trigger screen lock simulation
                        try {
                            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
                            context.startActivity(intent)
                        } catch (e: Exception) {}
                    },
                    onLongPress = {
                        // Open home settings
                        val intent = Intent(context, com.example.ui.SettingsActivity::class.java)
                        context.startActivity(intent)
                    }
                )
            }
    ) {
        content()
    }
}

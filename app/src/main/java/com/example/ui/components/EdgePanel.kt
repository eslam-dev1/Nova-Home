package com.example.ui.components

import android.content.Intent
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun EdgePanel(
    isOpen: Boolean,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it }),
        modifier = Modifier.fillMaxHeight().wrapContentWidth(Alignment.End)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(80.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text("Tools", style = MaterialTheme.typography.labelMedium)
                
                IconButton(onClick = {
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    try { context.startActivity(intent) } catch (e: Exception) {}
                }) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Camera")
                }

                IconButton(onClick = {
                    val intent = Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)
                    try { context.startActivity(intent) } catch (e: Exception) {}
                }) {
                    Icon(Icons.Default.Info, contentDescription = "Info / Audio")
                }

                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_WEB_SEARCH)
                    try { context.startActivity(intent) } catch (e: Exception) {}
                }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }

                IconButton(onClick = {
                    val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                    try { context.startActivity(intent) } catch (e: Exception) {}
                }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        }
    }
}

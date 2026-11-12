package com.example.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReliabilityScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    
    Scaffold(
        topBar = { TopAppBar(title = { Text("Reliability Settings") }) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
            item {
                Text("To ensure alarms and notifications work reliably on your device, please adjust these settings:", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                Button(onClick = {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    try { context.startActivity(intent) } catch (e: Exception) {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                }) { Text("1. Battery Usage (Unrestricted)") }
            }
            item {
                Button(onClick = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    try { context.startActivity(intent) } catch (e: Exception) {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                    }
                }) { Text("2. Enable Notifications") }
            }
            item {
                Text("3. Lock in Recents: Open your recent apps screen, find this app, and look for a lock icon or menu option to lock it.", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("4. Auto-launch: Check if your system settings allow 'Auto-launch' for this app.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

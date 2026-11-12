package com.example.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.LauncherApplication
import com.example.R
import com.example.ui.theme.NovaHomeTheme
import com.example.ui.LauncherViewModel
import android.net.Uri
import kotlinx.coroutines.launch
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class SettingsActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels {
        LauncherViewModel.Factory(
            (application as LauncherApplication).repository,
            (application as LauncherApplication).settingsManager
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            NovaHomeTheme {
                SettingsScreen(
                    onBack = { finish() },
                    viewModel = viewModel,
                    settingsManager = (application as LauncherApplication).settingsManager
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: LauncherViewModel, settingsManager: com.example.data.SettingsManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val showLabels by settingsManager.showLabels.collectAsState(initial = true)
    val iconShape by settingsManager.iconShape.collectAsState(initial = "circle")
    val gridSize by settingsManager.gridSize.collectAsState(initial = "5x5")
    val deviceProfile by settingsManager.deviceProfile.collectAsState(initial = "Pixel 9 Style")
    val contextualMode by settingsManager.contextualMode.collectAsState(initial = "Auto")
    val iconPack by settingsManager.iconPack.collectAsState(initial = null)
    
    val backupManager = remember { com.example.data.BackupManager(context, settingsManager) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            scope.launch {
                val result = backupManager.exportSettings(it)
                if (result.isSuccess) {
                    snackbarHostState.showSnackbar("Backup exported successfully!")
                } else {
                    snackbarHostState.showSnackbar("Export failed: ${result.exceptionOrNull()?.localizedMessage}")
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            pendingImportUri = it
            showImportConfirmDialog = true
        }
    }

    if (showImportConfirmDialog && pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = { showImportConfirmDialog = false },
            title = { Text("Restore Backup") },
            text = { Text("This will replace your current settings and hidden apps. Continue?") },
            confirmButton = {
                TextButton(onClick = {
                    showImportConfirmDialog = false
                    pendingImportUri?.let { uri ->
                        scope.launch {
                            val result = backupManager.importSettings(uri)
                            if (result.isSuccess) {
                                snackbarHostState.showSnackbar(result.getOrDefault("Restored successfully"))
                            } else {
                                snackbarHostState.showSnackbar("Import error: ${result.exceptionOrNull()?.localizedMessage}")
                            }
                        }
                    }
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                ListItem(
                    headlineContent = { Text("Backup/Restore Layout") },
                    modifier = Modifier.padding(vertical = 8.dp),
                    trailingContent = {
                        Row {
                            TextButton(onClick = { exportLauncher.launch("launcher_backup.json") }) { Text("Export") }
                            TextButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) { Text("Import") }
                        }
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.set_as_default)) },
                    modifier = Modifier.padding(vertical = 8.dp),
                    trailingContent = {
                        Button(onClick = {
                            val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                            context.startActivity(intent)
                        }) {
                            Text("Open Settings")
                        }
                    }
                )
            }
            
            item {
                ListItem(
                    headlineContent = { Text("Change Wallpaper") },
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                        context.startActivity(Intent.createChooser(intent, "Select Wallpaper"))
                    }
                )
            }
            
            item {
                ListItem(
                    headlineContent = { Text("Show Labels") },
                    trailingContent = {
                        Switch(
                            checked = showLabels,
                            onCheckedChange = { 
                                scope.launch { settingsManager.setShowLabels(it) } 
                            }
                        )
                    }
                )
            }
            
            item {
                val iconPacks = remember { (context.applicationContext as LauncherApplication).iconPackHelper.getInstalledIconPacks() }
                var showIconPackDialog by remember { mutableStateOf(false) }

                ListItem(
                    headlineContent = { Text("Icon Pack") },
                    supportingContent = { Text(if (iconPack.isNullOrEmpty()) "Default" else iconPack!!) },
                    modifier = Modifier.clickable { showIconPackDialog = true }
                )

                if (showIconPackDialog) {
                    AlertDialog(
                        onDismissRequest = { showIconPackDialog = false },
                        title = { Text("Select Icon Pack") },
                        text = {
                            LazyColumn {
                                item {
                                    ListItem(
                                        headlineContent = { Text("Default") },
                                        modifier = Modifier.clickable {
                                            scope.launch { settingsManager.setIconPack(null) }
                                            showIconPackDialog = false
                                        }
                                    )
                                }
                                items(iconPacks) { pack ->
                                    ListItem(
                                        headlineContent = { Text(pack.second) },
                                        modifier = Modifier.clickable {
                                            scope.launch { settingsManager.setIconPack(pack.first) }
                                            showIconPackDialog = false
                                        }
                                    )
                                }
                            }
                        },
                        confirmButton = { TextButton(onClick = { showIconPackDialog = false }) { Text("Cancel") } }
                    )
                }
            }
            
            item {
                ListItem(
                    headlineContent = { Text("Reliability Settings") },
                    supportingContent = { Text("Fix background restrictions") },
                    modifier = Modifier.clickable { context.startActivity(Intent(context, com.example.ui.ReliabilityActivity::class.java)) }
                )
            }

            item {
                Text("Search Engine", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleSmall)
                val currentEngine by settingsManager.searchEngine.collectAsState(initial = com.example.data.WebSearchEngine.GOOGLE)
                Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.example.data.WebSearchEngine.entries.forEach { engine ->
                        FilterChip(
                            selected = currentEngine == engine,
                            onClick = { scope.launch { settingsManager.setSearchEngine(engine) } },
                            label = { Text(engine.displayName) }
                        )
                    }
                }
            }

            item {
                Text("Device Profile", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleSmall)
                Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                    listOf("Auto", "Pixel 9 Style", "Honor X9d Native").forEach { profile ->
                        FilterChip(
                            selected = deviceProfile == profile,
                            onClick = { scope.launch { settingsManager.setDeviceProfile(profile) } },
                            label = { Text(profile) }
                        )
                    }
                }
            }

            item {
                Text("Contextual Dashboard Mode", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleSmall)
                Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Auto", "Work Mode", "Home Mode").forEach { mode ->
                        FilterChip(
                            selected = contextualMode == mode,
                            onClick = { scope.launch { settingsManager.setContextualMode(mode) } },
                            label = { Text(mode) }
                        )
                    }
                }
            }
            
            item {
                Text("Icon Shape", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleSmall)
                Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                    listOf("circle", "squircle", "square").forEach { shape ->
                        FilterChip(
                            selected = iconShape == shape,
                            onClick = { scope.launch { settingsManager.setIconShape(shape) } },
                            label = { Text(shape.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }

            item {
                Text("Grid Size", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleSmall)
                Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                    listOf("4x4", "5x5").forEach { size ->
                        FilterChip(
                            selected = gridSize == size,
                            onClick = { scope.launch { settingsManager.setGridSize(size) } },
                            label = { Text(size) }
                        )
                    }
                }
            }

            item {
                val edgeBackGesture by settingsManager.edgeBackGesture.collectAsState(initial = true)
                ListItem(
                    headlineContent = { Text("Simulated Edge Back Gesture") },
                    trailingContent = {
                        Switch(
                            checked = edgeBackGesture,
                            onCheckedChange = { 
                                scope.launch { settingsManager.setEdgeBackGesture(it) } 
                            }
                        )
                    }
                )
            }

            item {
                val hasContactPermission = remember {
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.READ_CONTACTS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                }
                var permissionGranted by remember { mutableStateOf(hasContactPermission) }
                val contactPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    permissionGranted = granted
                    if (!granted) {
                        scope.launch { snackbarHostState.showSnackbar("Contact permission denied. Search will only include apps and settings.") }
                    }
                }

                ListItem(
                    headlineContent = { Text("Include Contacts in Search") },
                    supportingContent = { Text("Search your device contacts directly from the launcher") },
                    trailingContent = {
                        Switch(
                            checked = permissionGranted,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    contactPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                                } else {
                                    permissionGranted = false
                                    scope.launch { snackbarHostState.showSnackbar("Revoke permission from system settings if needed.") }
                                }
                            }
                        )
                    }
                )
            }

            item {
                var showNotificationDialog by remember { mutableStateOf(false) }
                ListItem(
                    headlineContent = { Text("Notification Badges (Unread Count)") },
                    supportingContent = { Text("Enable notification access to show unread badges on app icons") },
                    modifier = Modifier.clickable {
                        showNotificationDialog = true
                    }
                )

                if (showNotificationDialog) {
                    AlertDialog(
                        onDismissRequest = { showNotificationDialog = false },
                        title = { Text("Notification Access Permission") },
                        text = { Text("Nova Home uses notification access solely to display unread badge counts on app icons. No personal notification content is ever stored or transmitted.") },
                        confirmButton = {
                            TextButton(onClick = {
                                showNotificationDialog = false
                                try {
                                    context.startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                } catch (e: Exception) {}
                            }) { Text("Open Settings") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showNotificationDialog = false }) { Text("Cancel") }
                        }
                    )
                }
            }
            
            item {
                Text("Backup & Restore", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleSmall)
                ListItem(
                    headlineContent = { Text("Export Backup") },
                    supportingContent = { Text("Save settings and hidden apps to JSON") },
                    modifier = Modifier.clickable {
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        exportLauncher.launch("pixel-launcher-backup-$dateFormat.json")
                    }
                )
                ListItem(
                    headlineContent = { Text("Import Backup") },
                    supportingContent = { Text("Restore settings from JSON file") },
                    modifier = Modifier.clickable {
                        importLauncher.launch(arrayOf("application/json", "*/*"))
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.reset_layout)) },
                    modifier = Modifier.padding(vertical = 8.dp),
                    colors = ListItemDefaults.colors(headlineColor = MaterialTheme.colorScheme.error),
                    trailingContent = {
                        TextButton(onClick = { viewModel.resetLayout() }) {
                            Text("Reset")
                        }
                    }
                )
            }
        }
    }
}

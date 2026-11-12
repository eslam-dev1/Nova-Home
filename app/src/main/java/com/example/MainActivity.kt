package com.example

import android.util.Log

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.runtime.collectAsState
import com.example.R
import com.example.data.AppInfo
import com.example.data.ItemType
import com.example.data.LauncherItem
import com.example.ui.LauncherViewModel
import com.example.ui.SettingsActivity
import com.example.ui.theme.NovaHomeTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import com.google.android.gms.location.LocationServices

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels {
        LauncherViewModel.Factory(
            (application as LauncherApplication).repository,
            (application as LauncherApplication).settingsManager
        )
    }

    private val appCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
            packageName?.let {
                viewModel.removeAppReferences(it)
                viewModel.loadApps()
            }
        }
        override fun onPackageAdded(packageName: String?, user: UserHandle?) {
            viewModel.loadApps()
        }
        override fun onPackageChanged(packageName: String?, user: UserHandle?) {
            viewModel.loadApps()
        }
        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) {
            viewModel.loadApps()
        }
        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) {
            viewModel.loadApps()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("NovaHome", "MainActivity onCreate")
        enableEdgeToEdge()
        
        try {
            val launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            launcherApps.registerCallback(appCallback)
        } catch (e: Exception) {
            Log.e("NovaHome", "Failed to register app callback", e)
        }

        setContent {
            NovaHomeTheme {
                LauncherScreen(viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        launcherApps.unregisterCallback(appCallback)
    }

    // Handle home button press if activity is already running
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            // Logic to return to home screen and close drawer handled by ViewModel/State
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherScreen(viewModel: LauncherViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allApps by viewModel.filteredApps.collectAsState(initial = emptyList())
    val homeItems by viewModel.homeItems.collectAsState(initial = emptyList())
    val recentlyUsedApps by viewModel.recentlyUsedApps.collectAsState(initial = emptyList())
    val suggestedFolders by viewModel.suggestedFolders.collectAsState(initial = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsState(initial = "")
    val gridSize by viewModel.gridSize.collectAsState(initial = "5x5")
    val notificationCounts by viewModel.notificationCounts.collectAsState(initial = emptyMap())
    
    var isDrawerOpen by remember { mutableStateOf(false) }
    var isEdgePanelOpen by remember { mutableStateOf(false) }
    
    BackHandler(isDrawerOpen || isEdgePanelOpen) {
        if (isDrawerOpen) isDrawerOpen = false
        if (isEdgePanelOpen) isEdgePanelOpen = false
    }

    com.example.ui.components.AdvancedGestureHandler(
        context = context,
        onSwipeDown = { isDrawerOpen = false },
        onSwipeUp = { isDrawerOpen = true },
        onDoubleTap = {}
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
        val weatherState by viewModel.weatherState.collectAsState()
        
        // Location fetching for weather
        val fusedLocationClient = remember { com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context) }
        val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    loc?.let { viewModel.fetchWeather(it.latitude, it.longitude) }
                }
            }
        }
        
        LaunchedEffect(Unit) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    loc?.let { viewModel.fetchWeather(it.latitude, it.longitude) }
                }
            } else {
                locationPermission.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        HomeScreen(
            homeItems = homeItems,
            weatherState = weatherState,
            onAppLaunch = { item -> 
                viewModel.recordLaunch(item.packageName!!)
                launchApp(context, item.packageName!!, item.className!!, null) 
            },
            onLongClick = { /* Handle long press */ },
            onRemove = { viewModel.deleteItem(it) },
            notificationCounts = notificationCounts
        )

        // Dock & Bottom Search Pill
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Dock(
                items = homeItems.filter { it.containerId == LauncherItem.CONTAINER_DOCK },
                onAppLaunch = { app -> launchApp(context, app.packageName!!, app.className!!, null) },
                onRemove = { viewModel.deleteItem(it) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            SearchPill(onClick = { isDrawerOpen = true })
        }

        val webResults by viewModel.webResults.collectAsState()

        // App Drawer Overlay with Pixel-like Spring Physics
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))
        ) {
            AppDrawer(
                apps = allApps,
                recentlyUsedApps = recentlyUsedApps,
                suggestedFolders = suggestedFolders,
                notificationCounts = notificationCounts,
                searchQuery = searchQuery,
                webResults = webResults,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                onAppLaunch = { app -> 
                    viewModel.recordLaunch(app.packageName)
                    launchApp(context, app.packageName, app.className, app.userHandle)
                    isDrawerOpen = false
                },
                onAddSuggestedFolder = { suggestion ->
                    // Find first empty spot or just add to first screen
                    val itemsToAdd = suggestion.apps.map { app ->
                        LauncherItem(
                            type = ItemType.APP,
                            screenIndex = 0,
                            cellX = 0,
                            cellY = 0,
                            label = app.label,
                            packageName = app.packageName,
                            className = app.className
                        )
                    }
                    viewModel.createFolder(suggestion.name, 0, 0, 0, itemsToAdd)
                    isDrawerOpen = false
                },
                onAddToHome = { app ->
                    viewModel.addItemToHome(
                        LauncherItem(
                            type = ItemType.APP,
                            screenIndex = 0,
                            cellX = 0, // Placeholder for logic to find empty spot
                            cellY = 0,
                            label = app.label,
                            packageName = app.packageName,
                            className = app.className
                        )
                    )
                    isDrawerOpen = false
                },
                onWebSearch = { web ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(web.url))
                    context.startActivity(intent)
                },
                onClose = { isDrawerOpen = false }
            )
        }

        // Edge Panel Trigger Handle on the right side
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(16.dp)
                .height(100.dp)
                .clickable { isEdgePanelOpen = !isEdgePanelOpen }
        )

        // Edge Panel
        com.example.ui.components.EdgePanel(
            isOpen = isEdgePanelOpen,
            onClose = { isEdgePanelOpen = false }
        )
    }
  }
}

@Composable
fun HomeScreen(
    homeItems: List<LauncherItem>,
    weatherState: String?,
    onAppLaunch: (LauncherItem) -> Unit,
    onLongClick: (LauncherItem) -> Unit,
    onRemove: (LauncherItem) -> Unit,
    notificationCounts: Map<String, Int>
) {
    val pagerState = rememberPagerState(pageCount = { 3 }) 
    val context = LocalContext.current
    val wallpaperManager = WallpaperManager.getInstance(context)
    val windowToken = (context as? android.app.Activity)?.window?.decorView?.windowToken

    LaunchedEffect(pagerState.currentPage, pagerState.currentPageOffsetFraction) {
        if (windowToken != null) {
            val totalPages = pagerState.pageCount
            if (totalPages > 1) {
                val scrollPosition = pagerState.currentPage + pagerState.currentPageOffsetFraction
                val xOffset = scrollPosition / (totalPages - 1)
                wallpaperManager.setWallpaperOffsets(windowToken, xOffset, 0f)
            }
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(64.dp))
        DateWidget(weatherState)
        com.example.ui.components.ContextualBanner()
        
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
            HomeGrid(
                items = homeItems.filter { it.screenIndex == page && it.containerId == LauncherItem.CONTAINER_DESKTOP },
                onItemClick = onAppLaunch,
                onItemLongClick = onLongClick,
                onItemRemove = onRemove,
                notificationCounts = notificationCounts,
                modifier = Modifier.graphicsLayer {
                    alpha = androidx.compose.ui.util.lerp(
                        start = 0.5f,
                        stop = 1f,
                        fraction = 1f - pageOffset.coerceIn(0f, 1f)
                    )
                    scaleX = androidx.compose.ui.util.lerp(
                        start = 0.8f,
                        stop = 1f,
                        fraction = 1f - pageOffset.coerceIn(0f, 1f)
                    )
                    scaleY = androidx.compose.ui.util.lerp(
                        start = 0.8f,
                        stop = 1f,
                        fraction = 1f - pageOffset.coerceIn(0f, 1f)
                    )
                }
            )
        }
    }
}

@Composable
fun HomeGrid(
    items: List<LauncherItem>,
    onItemClick: (LauncherItem) -> Unit,
    onItemLongClick: (LauncherItem) -> Unit,
    onItemRemove: (LauncherItem) -> Unit,
    notificationCounts: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        contentPadding = PaddingValues(16.dp),
        modifier = modifier.fillMaxSize(),
        userScrollEnabled = false
    ) {
        items(items) { item ->
            AppIconItem(
                label = item.label ?: "",
                packageName = item.packageName ?: "",
                className = item.className ?: "",
                notificationCount = notificationCounts[item.packageName] ?: 0,
                onClick = { onItemClick(item) },
                onLongClick = { onItemLongClick(item) },
                onRemove = { onItemRemove(item) }
            )
        }
    }
}

@Composable
fun DateWidget(weather: String?) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val dayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time)
    val dateText = SimpleDateFormat("MMMM d", Locale.getDefault()).format(calendar.time)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 16.dp)
    ) {
        Text(
            text = dayName,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = dateText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        weather?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun Dock(
    items: List<LauncherItem>,
    onAppLaunch: (LauncherItem) -> Unit,
    onRemove: (LauncherItem) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            AppIconItem(
                label = item.label ?: "",
                packageName = item.packageName ?: "",
                className = item.className ?: "",
                onClick = { onAppLaunch(item) },
                onRemove = { onRemove(item) }
            )
        }
        
        // Fill empty spots if less than 5
        if (items.size < 5) {
            repeat(5 - items.size) {
                 Box(modifier = Modifier.size(56.dp).background(Color.White.copy(alpha = 0.1f), CircleShape))
            }
        }
    }
}

@Composable
fun SearchPill(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .height(52.dp)
            .testTag("search_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.drawer_search_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun AppDrawer(
    apps: List<AppInfo>,
    recentlyUsedApps: List<AppInfo>,
    suggestedFolders: List<com.example.data.SuggestedFolder>,
    notificationCounts: Map<String, Int>,
    searchQuery: String,
    webResults: List<com.example.ui.search.WebResult> = emptyList(),
    onSearchQueryChange: (String) -> Unit,
    onAppLaunch: (AppInfo) -> Unit,
    onAddSuggestedFolder: (com.example.data.SuggestedFolder) -> Unit,
    onAddToHome: (AppInfo) -> Unit,
    onWebSearch: (com.example.ui.search.WebResult) -> Unit = {},
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .blur(radius = 20.dp)
            .testTag("app_drawer"),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .testTag("app_drawer_search_input"),
                placeholder = { Text(stringResource(R.string.drawer_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = CircleShape,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            if (searchQuery.isNotEmpty()) {
                com.example.ui.search.SearchCategoriesView(
                    apps = apps,
                    contacts = emptyList(), // Placeholder for future contact integration
                    settings = emptyList(), // Placeholder for future settings integration
                    webResults = webResults,
                    hasContactPermission = false,
                    onAppClick = onAppLaunch,
                    onWebClick = onWebSearch,
                    modifier = Modifier.weight(1f)
                )
            } else {
                if (suggestedFolders.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestedFolders.forEach { folder ->
                            SuggestionChip(
                                onClick = { onAddSuggestedFolder(folder) },
                                label = { Text("Create ${folder.name} folder") }
                            )
                        }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (recentlyUsedApps.isNotEmpty()) {
                        item(span = { GridItemSpan(5) }) {
                            Text(
                                text = stringResource(R.string.recently_used),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(recentlyUsedApps) { app ->
                            AppIconItem(
                                label = app.label,
                                packageName = app.packageName,
                                className = app.className,
                                icon = app.icon,
                                notificationCount = notificationCounts[app.packageName] ?: 0,
                                onClick = { onAppLaunch(app) },
                                onLongClick = { onAddToHome(app) }
                            )
                        }
                        item(span = { GridItemSpan(5) }) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp))
                        }
                    }

                    items(apps) { app ->
                        AppIconItem(
                            label = app.label,
                            packageName = app.packageName,
                            className = app.className,
                            icon = app.icon,
                            notificationCount = notificationCounts[app.packageName] ?: 0,
                            onClick = { onAppLaunch(app) },
                            onLongClick = { onAddToHome(app) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIconItem(
    label: String,
    packageName: String,
    className: String,
    icon: android.graphics.drawable.Drawable? = null,
    notificationCount: Int = 0,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var appIcon by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(icon?.toBitmap()?.asImageBitmap()) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(packageName, className) {
        if (appIcon == null) {
            val loader = (context.applicationContext as LauncherApplication).iconLoader
            val drawable = loader.loadIcon(packageName, className)
            appIcon = drawable?.toBitmap()?.asImageBitmap()
        }
    }

    Box {
        Column(
            modifier = Modifier
                .testTag("app_item_${packageName}")
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        showMenu = true
                        onLongClick?.invoke()
                    }
                )
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                appIcon?.let { bitmap ->
                    Image(
                        bitmap = bitmap,
                        contentDescription = label,
                        modifier = Modifier.fillMaxSize().padding(8.dp)
                    )
                } ?: run {
                    // Fallback or placeholder if icon is loading
                    Box(modifier = Modifier.fillMaxSize().background(Color.Gray.copy(alpha = 0.2f)))
                }

                if (notificationCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.TopEnd)
                            .clip(CircleShape)
                            .background(Color.Red)
                    ) {
                        Text(
                            text = notificationCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        com.example.ui.components.AppItemPopupMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            onAppInfo = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", packageName, null)
                }
                context.startActivity(intent)
            },
            onUninstall = {
                val intent = Intent(Intent.ACTION_DELETE).apply {
                    data = android.net.Uri.fromParts("package", packageName, null)
                }
                context.startActivity(intent)
            },
            onRemove = onRemove
        )
    }
}

private fun launchApp(context: Context, packageName: String, className: String, user: UserHandle?) {
    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    try {
        launcherApps.startMainActivity(
            ComponentName(packageName, className),
            user ?: android.os.Process.myUserHandle(),
            null,
            null
        )
    } catch (e: Exception) {
        // Fallback or error log
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun SearchPillPreview() {
    NovaHomeTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            SearchPill(onClick = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppIconPreview() {
    NovaHomeTheme {
        AppIconItem(
            label = "Settings",
            packageName = "com.android.settings",
            className = "com.android.settings.Settings",
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DateWidgetPreview() {
    NovaHomeTheme {
        DateWidget(weather = "25°C, Sunny")
    }
}
@Preview(showBackground = true)
@Composable
fun FolderIconPreview() {
    NovaHomeTheme {
        com.example.ui.components.FolderIcon(
            folder = LauncherItem(type = ItemType.FOLDER, screenIndex = 0, cellX = 0, cellY = 0, label = "Social"),
            folderItems = listOf(
                LauncherItem(type = ItemType.APP, screenIndex = 0, cellX = 0, cellY = 0, rank = 0),
                LauncherItem(type = ItemType.APP, screenIndex = 0, cellX = 0, cellY = 0, rank = 1)
            ),
            onClick = {},
            onLongClick = {}
        )
    }
}

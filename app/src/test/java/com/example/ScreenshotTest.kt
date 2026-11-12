package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.AppInfo
import com.example.data.ItemType
import com.example.data.LauncherItem
import com.example.ui.search.ContactResult
import com.example.ui.search.SearchCategoriesView
import com.example.ui.search.SettingResult
import com.example.ui.search.WebResult
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-420dpi")
class ScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleHomeItems = listOf(
        LauncherItem(
            id = 1,
            type = ItemType.APP,
            screenIndex = 0,
            cellX = 0,
            cellY = 0,
            label = "Gmail",
            packageName = "com.google.android.gm",
            className = "ConversationListActivity"
        ),
        LauncherItem(
            id = 2,
            type = ItemType.APP,
            screenIndex = 0,
            cellX = 1,
            cellY = 0,
            label = "Chrome",
            packageName = "com.android.chrome",
            className = "Main"
        ),
        LauncherItem(
            id = 3,
            type = ItemType.APP,
            screenIndex = 0,
            cellX = 2,
            cellY = 0,
            label = "Camera",
            packageName = "com.android.camera",
            className = "Camera"
        ),
        LauncherItem(
            id = 4,
            type = ItemType.APP,
            screenIndex = 0,
            cellX = 3,
            cellY = 0,
            label = "Settings",
            packageName = "com.android.settings",
            className = "Settings"
        )
    )

    private val sampleApps = listOf(
        AppInfo(label = "Calculator", packageName = "com.google.android.calculator", className = "Calculator"),
        AppInfo(label = "Chrome", packageName = "com.android.chrome", className = "Main"),
        AppInfo(label = "Gmail", packageName = "com.google.android.gm", className = "ConversationListActivity"),
        AppInfo(label = "Instagram", packageName = "com.instagram.android", className = "MainActivity"),
        AppInfo(label = "Maps", packageName = "com.google.android.apps.maps", className = "MapsActivity"),
        AppInfo(label = "Photos", packageName = "com.google.android.apps.photos", className = "HomeActivity"),
        AppInfo(label = "Settings", packageName = "com.android.settings", className = "SettingsActivity"),
        AppInfo(label = "YouTube", packageName = "com.google.android.youtube", className = "HomeActivity")
    )

    private val sampleContacts = listOf(
        ContactResult(name = "Sarah Connor", lookupKey = "lookup_sarah"),
        ContactResult(name = "John Doe", lookupKey = "lookup_john")
    )

    private val sampleSettings = listOf(
        SettingResult(title = "Display Settings", summary = "Adjust grid size, icon shape and wallpaper"),
        SettingResult(title = "Privacy Settings", summary = "Manage hidden applications")
    )

    private val sampleWebResults = listOf(
        WebResult(query = "Android Launcher", url = "https://duckduckgo.com/?q=Android+Launcher")
    )

    @Test
    fun homeScreen_default() {
        composeTestRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                HomeScreen(
                    homeItems = sampleHomeItems,
                    weatherState = "Sunny 24°C",
                    onAppLaunch = {},
                    onLongClick = {},
                    onRemove = {},
                    notificationCounts = mapOf("com.google.android.gm" to 3)
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/homeScreen_default.png")
    }

    @Test
    fun homeScreen_empty() {
        composeTestRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                HomeScreen(
                    homeItems = emptyList(),
                    weatherState = null,
                    onAppLaunch = {},
                    onLongClick = {},
                    onRemove = {},
                    notificationCounts = emptyMap()
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/homeScreen_empty.png")
    }

    @Test
    fun appDrawer_lightTheme() {
        composeTestRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                AppDrawer(
                    apps = sampleApps,
                    recentlyUsedApps = sampleApps.take(4),
                    suggestedFolders = emptyList(),
                    notificationCounts = mapOf("com.google.android.gm" to 2),
                    searchQuery = "",
                    onSearchQueryChange = {},
                    onAppLaunch = {},
                    onAddSuggestedFolder = {},
                    onAddToHome = {},
                    onClose = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/appDrawer_lightTheme.png")
    }

    @Test
    fun appDrawer_darkTheme() {
        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                AppDrawer(
                    apps = sampleApps,
                    recentlyUsedApps = sampleApps.take(4),
                    suggestedFolders = emptyList(),
                    notificationCounts = mapOf("com.google.android.gm" to 2),
                    searchQuery = "",
                    onSearchQueryChange = {},
                    onAppLaunch = {},
                    onAddSuggestedFolder = {},
                    onAddToHome = {},
                    onClose = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/appDrawer_darkTheme.png")
    }

    @Test
    fun appDrawer_rtlLayout() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(colorScheme = lightColorScheme()) {
                    AppDrawer(
                        apps = sampleApps,
                        recentlyUsedApps = sampleApps.take(4),
                        suggestedFolders = emptyList(),
                        notificationCounts = mapOf("com.google.android.gm" to 2),
                        searchQuery = "",
                        onSearchQueryChange = {},
                        onAppLaunch = {},
                        onAddSuggestedFolder = {},
                        onAddToHome = {},
                        onClose = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/appDrawer_rtlLayout.png")
    }

    @Test
    fun search_withResults() {
        composeTestRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                SearchCategoriesView(
                    apps = sampleApps.take(3),
                    contacts = sampleContacts,
                    settings = sampleSettings,
                    webResults = sampleWebResults,
                    hasContactPermission = true
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/search_withResults.png")
    }

    @Test
    fun search_noResults() {
        composeTestRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                SearchCategoriesView(
                    apps = emptyList(),
                    contacts = emptyList(),
                    settings = emptyList(),
                    webResults = emptyList(),
                    hasContactPermission = true
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/search_noResults.png")
    }
}

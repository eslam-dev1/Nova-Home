package com.example

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.AppInfo
import com.example.ui.search.SearchEngine
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDrawerUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeRepo = FakeAppRepository()
    private val searchEngine = SearchEngine()

    @Before
    fun setUp() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun shouldShowExpectedApps_whenDrawerOpens() {
        val apps = fakeRepo.getApps()
        composeTestRule.setContent {
            AppDrawer(
                apps = apps,
                recentlyUsedApps = emptyList(),
                suggestedFolders = emptyList(),
                notificationCounts = emptyMap(),
                searchQuery = "",
                onSearchQueryChange = {},
                onAppLaunch = {},
                onAddSuggestedFolder = {},
                onAddToHome = {},
                onClose = {}
            )
        }

        composeTestRule.onNodeWithText("Gmail").assertIsDisplayed()
        composeTestRule.onNodeWithText("Instagram").assertIsDisplayed()
        composeTestRule.onNodeWithText("Chrome").assertIsDisplayed()
        composeTestRule.onNodeWithText("Calculator").assertIsDisplayed()
    }

    @Test
    fun shouldFilterResults_whenQueryTyped() {
        val allApps = fakeRepo.getApps()
        composeTestRule.setContent {
            var query by remember { mutableStateOf("") }
            val filtered = remember(query) { searchEngine.filterApps(allApps, query) }

            AppDrawer(
                apps = filtered,
                recentlyUsedApps = emptyList(),
                suggestedFolders = emptyList(),
                notificationCounts = emptyMap(),
                searchQuery = query,
                onSearchQueryChange = { query = it },
                onAppLaunch = {},
                onAddSuggestedFolder = {},
                onAddToHome = {},
                onClose = {}
            )
        }

        composeTestRule.onNodeWithTag("app_drawer_search_input").performTextInput("insta")

        composeTestRule.onNodeWithText("Instagram").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gmail").assertDoesNotExist()
        composeTestRule.onNodeWithText("Chrome").assertDoesNotExist()
    }

    @Test
    fun shouldMatchFuzzy_whenQueryIsPartial() {
        val allApps = fakeRepo.getApps()
        composeTestRule.setContent {
            var query by remember { mutableStateOf("") }
            val filtered = remember(query) { searchEngine.filterApps(allApps, query) }

            AppDrawer(
                apps = filtered,
                recentlyUsedApps = emptyList(),
                suggestedFolders = emptyList(),
                notificationCounts = emptyMap(),
                searchQuery = query,
                onSearchQueryChange = { query = it },
                onAppLaunch = {},
                onAddSuggestedFolder = {},
                onAddToHome = {},
                onClose = {}
            )
        }

        composeTestRule.onNodeWithTag("app_drawer_search_input").performTextInput("gmai")

        composeTestRule.onNodeWithText("Gmail").assertIsDisplayed()
        composeTestRule.onNodeWithText("Calculator").assertDoesNotExist()
    }

    @Test
    fun shouldShowAllApps_whenQueryIsEmpty() {
        val allApps = fakeRepo.getApps()
        composeTestRule.setContent {
            var query by remember { mutableStateOf("insta") }
            val filtered = remember(query) { searchEngine.filterApps(allApps, query) }

            AppDrawer(
                apps = filtered,
                recentlyUsedApps = emptyList(),
                suggestedFolders = emptyList(),
                notificationCounts = emptyMap(),
                searchQuery = query,
                onSearchQueryChange = { query = it },
                onAppLaunch = {},
                onAddSuggestedFolder = {},
                onAddToHome = {},
                onClose = {}
            )
        }

        composeTestRule.onNodeWithText("Instagram").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gmail").assertDoesNotExist()

        composeTestRule.onNodeWithTag("app_drawer_search_input").performTextClearance()

        composeTestRule.onNodeWithText("Gmail").assertIsDisplayed()
        composeTestRule.onNodeWithText("Instagram").assertIsDisplayed()
        composeTestRule.onNodeWithText("Chrome").assertIsDisplayed()
        composeTestRule.onNodeWithText("Calculator").assertIsDisplayed()
    }

    @Test
    fun shouldShowEmptyState_whenNoResults() {
        val allApps = fakeRepo.getApps()
        composeTestRule.setContent {
            var query by remember { mutableStateOf("") }
            val filtered = remember(query) { searchEngine.filterApps(allApps, query) }

            AppDrawer(
                apps = filtered,
                recentlyUsedApps = emptyList(),
                suggestedFolders = emptyList(),
                notificationCounts = emptyMap(),
                searchQuery = query,
                onSearchQueryChange = { query = it },
                onAppLaunch = {},
                onAddSuggestedFolder = {},
                onAddToHome = {},
                onClose = {}
            )
        }

        composeTestRule.onNodeWithTag("app_drawer_search_input").performTextInput("zzzzzz")

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("No results found")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule.onNodeWithText("No results found").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gmail").assertDoesNotExist()
    }

    @Test
    fun shouldLaunchApp_whenAppTapped() {
        val apps = fakeRepo.getApps()
        var launchedIntent: Intent? = null

        composeTestRule.setContent {
            AppDrawer(
                apps = apps,
                recentlyUsedApps = emptyList(),
                suggestedFolders = emptyList(),
                notificationCounts = emptyMap(),
                searchQuery = "",
                onSearchQueryChange = {},
                onAppLaunch = { app ->
                    val intent = Intent(Intent.ACTION_MAIN).apply {
                        setClassName(app.packageName, app.className)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    launchedIntent = intent
                },
                onAddSuggestedFolder = {},
                onAddToHome = {},
                onClose = {}
            )
        }

        composeTestRule.onNodeWithTag("app_item_com.google.android.gm").performClick()

        assertNotNull("Expected app launch intent to be created", launchedIntent)
        assertEquals("com.google.android.gm", launchedIntent?.component?.packageName)
        assertEquals("com.google.android.gm.ConversationListActivity", launchedIntent?.component?.className)
    }

    @Test
    fun shouldNotShowHiddenApps_whenHiddenListIsNotEmpty() {
        val allApps = fakeRepo.getApps()
        val hiddenPackages = setOf("com.instagram.android")
        val visibleApps = searchEngine.filterApps(allApps, "", hiddenPackages)

        composeTestRule.setContent {
            AppDrawer(
                apps = visibleApps,
                recentlyUsedApps = emptyList(),
                suggestedFolders = emptyList(),
                notificationCounts = emptyMap(),
                searchQuery = "",
                onSearchQueryChange = {},
                onAppLaunch = {},
                onAddSuggestedFolder = {},
                onAddToHome = {},
                onClose = {}
            )
        }

        composeTestRule.onNodeWithText("Gmail").assertIsDisplayed()
        composeTestRule.onNodeWithText("Chrome").assertIsDisplayed()
        composeTestRule.onNodeWithText("Instagram").assertDoesNotExist()
    }
}

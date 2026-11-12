package com.example

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.AppInfo
import com.example.ui.search.SearchCategoriesView
import com.example.ui.search.SettingResult
import com.example.ui.search.WebResult
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class SearchCategoriesUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeAppRepo = FakeAppRepository()
    private val fakeContactRepo = FakeContactRepository()

    @Test
    fun shouldGroupResultsByCategory() {
        val apps = fakeAppRepo.getApps()
        val contacts = fakeContactRepo.getContacts()
        val settings = listOf(
            SettingResult("Display Settings", "Adjust grid size, icon shape and wallpaper"),
            SettingResult("Privacy Settings", "Manage hidden applications")
        )
        val webResults = listOf(
            WebResult("Android Launcher", "https://duckduckgo.com/?q=Android+Launcher")
        )

        composeTestRule.setContent {
            SearchCategoriesView(
                apps = apps,
                contacts = contacts,
                settings = settings,
                webResults = webResults,
                hasContactPermission = true
            )
        }

        // Verify section headers exist and are displayed
        composeTestRule.onNodeWithTag("section_header_apps").assertIsDisplayed()
        composeTestRule.onNodeWithTag("section_header_contacts").assertIsDisplayed()
        composeTestRule.onNodeWithTag("section_header_settings").assertIsDisplayed()
        composeTestRule.onNodeWithTag("section_header_web").assertIsDisplayed()

        // Verify items in each category are present
        composeTestRule.onNodeWithText("Gmail").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sarah Connor").assertIsDisplayed()
        composeTestRule.onNodeWithText("Display Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Search web for \"Android Launcher\"").assertIsDisplayed()
    }

    @Test
    fun shouldHideContactsSection_whenPermissionDenied() {
        val apps = fakeAppRepo.getApps()
        val contacts = fakeContactRepo.getContacts()
        val settings = listOf(
            SettingResult("Display Settings", "Adjust grid size, icon shape and wallpaper")
        )
        val webResults = listOf(
            WebResult("Android Launcher", "https://duckduckgo.com/?q=Android+Launcher")
        )

        composeTestRule.setContent {
            SearchCategoriesView(
                apps = apps,
                contacts = contacts,
                settings = settings,
                webResults = webResults,
                hasContactPermission = false
            )
        }

        // Contacts section header and items must NOT be displayed
        composeTestRule.onNodeWithTag("section_header_contacts").assertDoesNotExist()
        composeTestRule.onNodeWithText("Sarah Connor").assertDoesNotExist()

        // Other sections must remain visible
        composeTestRule.onNodeWithTag("section_header_apps").assertIsDisplayed()
        composeTestRule.onNodeWithTag("section_header_settings").assertIsDisplayed()
        composeTestRule.onNodeWithTag("section_header_web").assertIsDisplayed()
    }

    @Test
    fun shouldDisplayPassedWebUrl() {
        val apps = fakeAppRepo.getApps()
        val dummyUrl = "https://example.com/search?q=Android"
        val webResults = listOf(
            WebResult("Android", dummyUrl)
        )

        composeTestRule.setContent {
            SearchCategoriesView(
                apps = apps,
                contacts = emptyList(),
                settings = emptyList(),
                webResults = webResults,
                hasContactPermission = false
            )
        }

        // Verify the passed URL is displayed
        composeTestRule.onNodeWithText(dummyUrl).assertIsDisplayed()
    }

    @Test
    fun shouldLaunchAppOnEnterKey_whenFirstResultFocused() {
        val apps = fakeAppRepo.getApps()
        var launchedApp: AppInfo? = null

        composeTestRule.setContent {
            SearchCategoriesView(
                apps = apps,
                contacts = emptyList(),
                settings = emptyList(),
                webResults = emptyList(),
                hasContactPermission = false,
                onAppClick = { launchedApp = it }
            )
        }

        // Send Down key to focus the first item (Apps header or first app), then Enter
        composeTestRule.onRoot().performKeyInput {
            pressKey(Key.DirectionDown)
            pressKey(Key.Enter)
        }

        // Verify that the app was 'clicked'
        assertTrue("An app should have been launched", launchedApp != null)
    }
}

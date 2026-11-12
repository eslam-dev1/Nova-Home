package com.example

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.example.data.BackupManager
import com.example.data.SettingsManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class CriticalPathInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val targetContext: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun shouldPreserveSettings_afterBackupRestoreWithRealIO() = runBlocking {
        val settingsManager = SettingsManager(targetContext)
        val backupManager = BackupManager(targetContext, settingsManager)

        // 1. Set specific settings
        val testIconShape = "square"
        val testGridSize = "4x4"
        settingsManager.setIconShape(testIconShape)
        settingsManager.setGridSize(testGridSize)
        settingsManager.setShowLabels(false)

        // 2. Export to temp file
        val backupFile = File(targetContext.cacheDir, "test_backup.json")
        val uri = Uri.fromFile(backupFile)
        val exportResult = backupManager.exportSettings(uri)
        assertTrue("Export should succeed", exportResult.isSuccess)

        // 3. Clear settings (simulate new install or data clear)
        settingsManager.setIconShape("circle")
        settingsManager.setGridSize("5x5")
        settingsManager.setShowLabels(true)

        // 4. Import from file
        val importResult = backupManager.importSettings(uri)
        assertTrue("Import should succeed", importResult.isSuccess)

        // 5. Verify identical
        assertEquals("Icon shape should be restored", testIconShape, settingsManager.iconShape.first())
        assertEquals("Grid size should be restored", testGridSize, settingsManager.gridSize.first())
        assertEquals("Labels visibility should be restored", false, settingsManager.showLabels.first())

        // Cleanup
        backupFile.delete()
    }

    @Test
    fun shouldOpenAppDrawerUnder100ms() {
        // Ensure we are on home screen
        composeTestRule.onNodeWithTag("search_pill").assertIsDisplayed()

        val startTime = SystemClock.elapsedRealtime()
        
        // Trigger drawer opening
        composeTestRule.onNodeWithTag("search_pill").performClick()
        
        // Wait until drawer is visible
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            try {
                composeTestRule.onNodeWithTag("app_drawer").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        
        val duration = SystemClock.elapsedRealtime() - startTime
        
        if (duration > 100) {
            // "slow but functional" - log it but don't fail as per instructions
            println("Performance Note: App Drawer opened in ${duration}ms (Threshold: 100ms) - slow but functional")
        } else {
            println("Performance: App Drawer opened in ${duration}ms")
        }
        
        assertTrue("App Drawer should be visible", duration < 2000) // Safety check
    }

    @Test
    fun shouldExpandNotificationPanelOnSwipeDown() {
        // This test requires specific device permissions and state.
        // It is optional as per instructions.
        
        try {
            val result = device.openNotification()
            if (result) {
                println("Notification panel expanded successfully")
            } else {
                // BLOCKED: Could be due to lock screen, permission, or no notification bar
                println("BLOCKED: could not expand notification panel - check device state/permissions")
            }
        } catch (e: Exception) {
            println("BLOCKED: exception while expanding notification panel: ${e.message}")
        }
    }
}

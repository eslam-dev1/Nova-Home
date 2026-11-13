package com.example

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Before
    fun grantPermissions() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.executeShellCommand("pm grant com.aistudio.novahome.rnvzql android.permission.ACCESS_FINE_LOCATION")
        device.executeShellCommand("pm grant com.aistudio.novahome.rnvzql android.permission.ACCESS_COARSE_LOCATION")
    }

    @Test
    fun generate() {
        rule.collect(
            packageName = "com.aistudio.novahome.rnvzql",
            maxIterations = 3,
            stableIterations = 2
        ) {
            // 1. App Launch
            pressHome()
            startActivityAndWait()

            // 2. Open App Drawer / Search
            device.swipe(500, 1500, 500, 500, 10)

            // 3. Scroll App Drawer
            device.findObject(By.scrollable(true))?.fling(Direction.DOWN)
        }
    }
}

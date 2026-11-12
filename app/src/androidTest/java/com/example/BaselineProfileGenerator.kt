package com.example

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

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

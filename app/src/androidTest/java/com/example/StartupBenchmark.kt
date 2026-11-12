package com.example

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startupCold() = benchmarkRule.measureRepeated(
        packageName = "com.aistudio.novahome.rnvzql",
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.DEFAULT,
        startupMode = StartupMode.COLD,
        iterations = 5
    ) {
        pressHome()
        startActivityAndWait()
    }

    @Test
    fun scrollDrawerFrameTiming() = benchmarkRule.measureRepeated(
        packageName = "com.aistudio.novahome.rnvzql",
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.DEFAULT,
        startupMode = StartupMode.WARM,
        iterations = 5
    ) {
        pressHome()
        startActivityAndWait()
        
        // Open App Drawer
        device.swipe(500, 1500, 500, 500, 10)
        
        // Scroll App Drawer
        val list = device.findObject(By.scrollable(true))
        list?.setGestureMargin(device.displayWidth / 5)
        list?.fling(Direction.DOWN)
    }
}

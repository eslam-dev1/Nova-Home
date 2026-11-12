package com.example

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.example.data.SettingsManager
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest {

    private lateinit var context: Context
    private lateinit var settingsManager: SettingsManager
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val testFile = File.createTempFile("test_prefs", ".preferences_pb", context.cacheDir)
        testFile.deleteOnExit()
        
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile }
        )
        settingsManager = SettingsManager(context, testDataStore)
    }

    @Test
    fun shouldReturnDefaultValues_onFirstRead() = runTest {
        assertThat(settingsManager.showLabels.first()).isTrue()
        assertThat(settingsManager.gridSize.first()).isEqualTo("5x5")
        assertThat(settingsManager.iconShape.first()).isEqualTo("circle")
    }

    @Test
    fun shouldReturnWrittenValue_afterWrite() = runTest {
        settingsManager.setGridSize("4x4")
        assertThat(settingsManager.gridSize.first()).isEqualTo("4x4")

        settingsManager.setShowLabels(false)
        assertThat(settingsManager.showLabels.first()).isFalse()
    }

    @Test
    fun shouldEmitOnEveryChange_whenFlowCollected() = runTest {
        settingsManager.iconShape.test {
            assertThat(awaitItem()).isEqualTo("circle")

            settingsManager.setIconShape("square")
            assertThat(awaitItem()).isEqualTo("square")

            settingsManager.setIconShape("squircle")
            assertThat(awaitItem()).isEqualTo("squircle")
        }
    }

    @Test
    fun shouldNotCorruptState_onConcurrentWrites() = runTest {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        
        val jobs = List(10) { i ->
            scope.launch {
                settingsManager.setGridSize(if (i % 2 == 0) "4x4" else "5x5")
            }
        }
        
        jobs.forEach { it.join() }

        val value = settingsManager.gridSize.first()
        assertThat(value).isIn(listOf("4x4", "5x5"))
    }
}

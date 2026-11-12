package com.example

import com.example.data.SettingsManager
import com.example.data.WebSearchEngine
import com.example.data.LauncherRepository
import com.example.ui.LauncherViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LauncherViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = mockk<LauncherRepository>(relaxed = true)
    private val settingsManager = mockk<SettingsManager>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { settingsManager.isFirstRun } returns flowOf(false)
        every { settingsManager.showLabels } returns flowOf(true)
        every { settingsManager.iconShape } returns flowOf("circle")
        every { settingsManager.gridSize } returns flowOf("5x5")
        every { settingsManager.deviceProfile } returns flowOf("Auto")
        every { settingsManager.contextualMode } returns flowOf("Auto")
        every { settingsManager.hiddenApps } returns flowOf(emptySet())
        every { settingsManager.iconPack } returns flowOf(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `emits DUCKDUCKGO search URL when setting is DuckDuckGo`() = runTest {
        every { settingsManager.searchEngine } returns flowOf(WebSearchEngine.DUCKDUCKGO)
        
        val viewModel = LauncherViewModel(repository, settingsManager)
        
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.webResults.collect()
        }
        
        viewModel.setSearchQuery("test query")
        advanceUntilIdle()
        
        val results = viewModel.webResults.value
        assertTrue("Results should contain DuckDuckGo URL. Results: $results", results.any { it.url.startsWith("https://duckduckgo.com") })
    }

    @Test
    fun `emits BING search URL when setting is Bing`() = runTest {
        every { settingsManager.searchEngine } returns flowOf(WebSearchEngine.BING)
        
        val viewModel = LauncherViewModel(repository, settingsManager)
        
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.webResults.collect()
        }
        
        viewModel.setSearchQuery("test query")
        advanceUntilIdle()
        
        val results = viewModel.webResults.value
        assertTrue("Results should contain Bing URL. Results: $results", results.any { it.url.startsWith("https://www.bing.com") })
    }
}

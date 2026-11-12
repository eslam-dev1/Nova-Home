package com.example

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import com.example.data.SettingsManager
import com.example.ui.IconPackResolver
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IconPackResolverTest {

    private lateinit var mockContext: Context
    private lateinit var mockPackageManager: PackageManager
    private lateinit var mockSettings: SettingsManager
    private lateinit var mockPackContext: Context
    private lateinit var mockResources: Resources
    private lateinit var originalDrawable: Drawable
    private lateinit var customDrawable: Drawable
    private lateinit var resolver: IconPackResolver

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockPackageManager = mockk(relaxed = true)
        mockSettings = mockk(relaxed = true)
        mockPackContext = mockk(relaxed = true)
        mockResources = mockk(relaxed = true)
        originalDrawable = mockk()
        customDrawable = mockk()

        every { mockContext.packageManager } returns mockPackageManager
        resolver = IconPackResolver(mockContext, mockSettings, mockPackageManager)
    }

    @Test
    fun shouldReturnOriginalIcon_whenNoIconPackInstalled() = runTest {
        every { mockSettings.iconPackPackageName } returns flowOf(null)
        every { mockPackageManager.getApplicationIcon("com.example.app") } returns originalDrawable

        val result = resolver.getIcon("com.example.app", "MainActivity")

        assertThat(result).isEqualTo(originalDrawable)
        verify(exactly = 1) { mockPackageManager.getApplicationIcon("com.example.app") }
    }

    @Test
    fun shouldReturnCustomIcon_whenIconPackInstalledAndAppIsMapped() = runTest {
        every { mockSettings.iconPackPackageName } returns flowOf("com.test.iconpack")
        every { mockContext.createPackageContext("com.test.iconpack", any()) } returns mockPackContext
        every { mockPackContext.resources } returns mockResources
        every { mockResources.getIdentifier("ic_com_example_app", "drawable", "com.test.iconpack") } returns 101
        every { mockResources.getDrawable(101, null) } returns customDrawable

        val result = resolver.getIcon("com.example.app", "MainActivity")

        assertThat(result).isEqualTo(customDrawable)
        verify(exactly = 0) { mockPackageManager.getApplicationIcon(any<String>()) }
    }

    @Test
    fun shouldFallbackToOriginal_whenAppMissingFromInstalledIconPack() = runTest {
        every { mockSettings.iconPackPackageName } returns flowOf("com.test.iconpack")
        every { mockContext.createPackageContext("com.test.iconpack", any()) } returns mockPackContext
        every { mockPackContext.resources } returns mockResources
        every { mockResources.getIdentifier("ic_com_example_app", "drawable", "com.test.iconpack") } returns 0
        every { mockPackageManager.getApplicationIcon("com.example.app") } returns originalDrawable

        val result = resolver.getIcon("com.example.app", "MainActivity")

        assertThat(result).isEqualTo(originalDrawable)
        verify(exactly = 1) { mockPackageManager.getApplicationIcon("com.example.app") }
    }

    @Test
    fun shouldUseCache_onSecondCallForSameComponentName() = runTest {
        every { mockSettings.iconPackPackageName } returns flowOf(null)
        every { mockPackageManager.getApplicationIcon("com.example.app") } returns originalDrawable

        val first = resolver.getIcon("com.example.app", "MainActivity")
        val second = resolver.getIcon("com.example.app", "MainActivity")

        assertThat(first).isEqualTo(originalDrawable)
        assertThat(second).isEqualTo(originalDrawable)
        verify(exactly = 1) { mockPackageManager.getApplicationIcon("com.example.app") }
    }

    @Test
    fun shouldDeduplicateConcurrentRequests_forSameComponentName() = runTest {
        every { mockSettings.iconPackPackageName } returns flowOf(null)
        every { mockPackageManager.getApplicationIcon("com.example.app") } answers {
            Thread.sleep(50)
            originalDrawable
        }

        coroutineScope {
            val deferreds = (1..10).map {
                async(Dispatchers.IO) {
                    resolver.getIcon("com.example.app", "MainActivity")
                }
            }
            val results = deferreds.map { it.await() }
            results.forEach { assertThat(it).isEqualTo(originalDrawable) }
        }

        verify(exactly = 1) { mockPackageManager.getApplicationIcon("com.example.app") }
    }
}

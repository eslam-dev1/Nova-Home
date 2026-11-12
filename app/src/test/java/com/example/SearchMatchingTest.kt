package com.example

import com.example.data.AppInfo
import com.example.ui.search.SearchEngine
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class SearchMatchingTest {

    private lateinit var searchEngine: SearchEngine
    private lateinit var sampleApps: List<AppInfo>

    @Before
    fun setup() {
        searchEngine = SearchEngine()
        sampleApps = listOf(
            AppInfo("Gmail", "com.google.android.gm", "Main"),
            AppInfo("Instagram", "com.instagram.android", "Main"),
            AppInfo("Chrome", "com.android.chrome", "Main"),
            AppInfo("تطبيق اختبار", "com.test.arabic", "Main"),
            AppInfo("C++ IDE", "com.ide.cpp", "Main"),
            AppInfo("App-1 Tool", "com.app.one", "Main")
        )
    }

    @Test
    fun shouldReturnAllApps_whenQueryIsEmpty() {
        val result = searchEngine.filterApps(sampleApps, "", emptySet())
        assertThat(result).hasSize(sampleApps.size)
    }

    @Test
    fun shouldReturnAllApps_whenQueryIsBlank() {
        val result = searchEngine.filterApps(sampleApps, "   ", emptySet())
        assertThat(result).hasSize(sampleApps.size)
    }

    @Test
    fun shouldMatchCaseInsensitively() {
        val result = searchEngine.filterApps(sampleApps, "gMaIl", emptySet())
        assertThat(result).hasSize(1)
        assertThat(result[0].label).isEqualTo("Gmail")
    }

    @Test
    fun shouldMatchArabicText() {
        val result = searchEngine.filterApps(sampleApps, "اختبار", emptySet())
        assertThat(result).hasSize(1)
        assertThat(result[0].label).isEqualTo("تطبيق اختبار")
    }

    @Test
    fun shouldNotMatch_whenQueryNotInLabel() {
        val result = searchEngine.filterApps(sampleApps, "nonexistent", emptySet())
        assertThat(result).isEmpty()
    }

    @Test
    fun shouldExcludeHiddenApps_whenQueryMatches() {
        val result = searchEngine.filterApps(sampleApps, "Gmail", setOf("com.google.android.gm"))
        assertThat(result).isEmpty()
    }

    @Test
    fun shouldMatchFuzzy_whenQueryIsPartial() {
        // "gmai" matching "Gmail"
        val result = searchEngine.filterApps(sampleApps, "gmai", emptySet())
        assertThat(result).isNotEmpty()
        assertThat(result[0].label).isEqualTo("Gmail")
    }

    @Test
    fun shouldHandleSpecialCharactersInQuery() {
        val cppResult = searchEngine.filterApps(sampleApps, "c++", emptySet())
        assertThat(cppResult).hasSize(1)
        assertThat(cppResult[0].label).isEqualTo("C++ IDE")

        val appOneResult = searchEngine.filterApps(sampleApps, "app-1", emptySet())
        assertThat(appOneResult).hasSize(1)
        assertThat(appOneResult[0].label).isEqualTo("App-1 Tool")
    }
}

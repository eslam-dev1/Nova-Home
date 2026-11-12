package com.example.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.AppInfo
import kotlinx.coroutines.launch

data class SettingResult(val title: String, val summary: String)
data class WebResult(val query: String, val url: String)

@Composable
fun SearchCategoriesView(
    apps: List<AppInfo>,
    contacts: List<ContactResult>,
    settings: List<SettingResult>,
    webResults: List<WebResult>,
    hasContactPermission: Boolean,
    onAppClick: (AppInfo) -> Unit = {},
    onContactClick: (ContactResult) -> Unit = {},
    onSettingClick: (SettingResult) -> Unit = {},
    onWebClick: (WebResult) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    // Flatten all result items for index-based keyboard navigation
    val focusableItems = remember(apps, contacts, settings, webResults, hasContactPermission) {
        val flat = mutableListOf<Any>()
        flat.addAll(apps)
        if (hasContactPermission) flat.addAll(contacts)
        flat.addAll(settings)
        flat.addAll(webResults)
        flat
    }

    var focusedIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(apps, contacts, settings, webResults, hasContactPermission) {
        focusedIndex = -1
    }

    // Exact calculation mapping focusedIndex to the actual LazyColumn item index (accounting for section headers)
    fun getLazyListIndex(targetFocusIndex: Int): Int {
        if (targetFocusIndex < 0) return 0
        var currentFocusIndex = 0
        var lazyIndex = 0

        if (apps.isNotEmpty()) {
            lazyIndex++ // Apps header
            if (targetFocusIndex < currentFocusIndex + apps.size) {
                return lazyIndex + (targetFocusIndex - currentFocusIndex)
            }
            currentFocusIndex += apps.size
            lazyIndex += apps.size
        }

        if (hasContactPermission && contacts.isNotEmpty()) {
            lazyIndex++ // Contacts header
            if (targetFocusIndex < currentFocusIndex + contacts.size) {
                return lazyIndex + (targetFocusIndex - currentFocusIndex)
            }
            currentFocusIndex += contacts.size
            lazyIndex += contacts.size
        }

        if (settings.isNotEmpty()) {
            lazyIndex++ // Settings header
            if (targetFocusIndex < currentFocusIndex + settings.size) {
                return lazyIndex + (targetFocusIndex - currentFocusIndex)
            }
            currentFocusIndex += settings.size
            lazyIndex += settings.size
        }

        if (webResults.isNotEmpty()) {
            lazyIndex++ // Web header
            if (targetFocusIndex < currentFocusIndex + webResults.size) {
                return lazyIndex + (targetFocusIndex - currentFocusIndex)
            }
            currentFocusIndex += webResults.size
            lazyIndex += webResults.size
        }

        return lazyIndex.coerceAtLeast(0)
    }

    val handleItemClick: (Any, Int) -> Unit = { item, index ->
        focusedIndex = index
        when (item) {
            is AppInfo -> onAppClick(item)
            is ContactResult -> onContactClick(item)
            is SettingResult -> onSettingClick(item)
            is WebResult -> onWebClick(item)
        }
    }

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {
            // Ignore focus request failure in test environment
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("search_results_container")
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.DirectionDown -> {
                            if (focusableItems.isNotEmpty()) {
                                focusedIndex = (focusedIndex + 1).coerceAtMost(focusableItems.size - 1)
                                scope.launch { listState.animateScrollToItem(getLazyListIndex(focusedIndex)) }
                            }
                            true
                        }
                        Key.DirectionUp -> {
                            if (focusableItems.isNotEmpty()) {
                                focusedIndex = (focusedIndex - 1).coerceAtLeast(0)
                                scope.launch { listState.animateScrollToItem(getLazyListIndex(focusedIndex)) }
                            }
                            true
                        }
                        Key.Enter, Key.NumPadEnter -> {
                            val targetIndex = if (focusedIndex < 0 && focusableItems.isNotEmpty()) 0 else focusedIndex
                            if (targetIndex in focusableItems.indices) {
                                handleItemClick(focusableItems[targetIndex], targetIndex)
                            }
                            true
                        }
                        else -> false
                    }
                } else false
            },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        var currentIndex = 0

        // 1. Apps Section
        if (apps.isNotEmpty()) {
            item {
                Text(
                    text = "Apps",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("section_header_apps")
                )
            }
            items(apps.size) { appIndex ->
                val app = apps[appIndex]
                val itemIdx = currentIndex + appIndex
                val isFocused = itemIdx == focusedIndex
                val itemColor = if (isFocused) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent

                ListItem(
                    headlineContent = { Text(app.label) },
                    supportingContent = { Text(app.packageName) },
                    modifier = Modifier
                        .testTag("search_result_app_${app.packageName}")
                        .background(itemColor)
                        .clickable { handleItemClick(app, itemIdx) }
                )
            }
            currentIndex += apps.size
        }

        // 2. Contacts Section
        if (hasContactPermission && contacts.isNotEmpty()) {
            item {
                Text(
                    text = "Contacts",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("section_header_contacts")
                )
            }
            items(contacts.size) { contactIndex ->
                val contact = contacts[contactIndex]
                val itemIdx = currentIndex + contactIndex
                val isFocused = itemIdx == focusedIndex
                val itemColor = if (isFocused) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent

                ListItem(
                    headlineContent = { Text(contact.name) },
                    leadingContent = { Icon(Icons.Default.Call, contentDescription = null) },
                    modifier = Modifier
                        .testTag("search_result_contact_${contact.name}")
                        .background(itemColor)
                        .clickable { handleItemClick(contact, itemIdx) }
                )
            }
            currentIndex += contacts.size
        }

        // 3. Settings Section
        if (settings.isNotEmpty()) {
            item {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("section_header_settings")
                )
            }
            items(settings.size) { settingIndex ->
                val setting = settings[settingIndex]
                val itemIdx = currentIndex + settingIndex
                val isFocused = itemIdx == focusedIndex
                val itemColor = if (isFocused) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent

                ListItem(
                    headlineContent = { Text(setting.title) },
                    supportingContent = { Text(setting.summary) },
                    leadingContent = { Icon(Icons.Default.Settings, contentDescription = null) },
                    modifier = Modifier
                        .testTag("search_result_setting_${setting.title}")
                        .background(itemColor)
                        .clickable { handleItemClick(setting, itemIdx) }
                )
            }
            currentIndex += settings.size
        }

        // 4. Web Section
        if (webResults.isNotEmpty()) {
            item {
                Text(
                    text = "Web",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("section_header_web")
                )
            }
            items(webResults.size) { webIndex ->
                val web = webResults[webIndex]
                val itemIdx = currentIndex + webIndex
                val isFocused = itemIdx == focusedIndex
                val itemColor = if (isFocused) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent

                ListItem(
                    headlineContent = { Text("Search web for \"${web.query}\"") },
                    supportingContent = { Text(web.url) },
                    leadingContent = { Icon(Icons.Default.Public, contentDescription = null) },
                    modifier = Modifier
                        .testTag("search_result_web_${web.query}")
                        .background(itemColor)
                        .clickable { handleItemClick(web, itemIdx) }
                )
            }
            currentIndex += webResults.size
        }

        // 5. Empty State
        if (apps.isEmpty() && (!hasContactPermission || contacts.isEmpty()) && settings.isEmpty() && webResults.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No results found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

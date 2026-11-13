package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.NotificationStateManager
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LauncherViewModel(
    private val repository: LauncherRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val allApps: StateFlow<List<AppInfo>> = _allApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val searchEngine = com.example.ui.search.SearchEngine()

    val filteredApps = combine(_allApps, _searchQuery, settingsManager.hiddenApps) { apps, query, hidden ->
        searchEngine.filterApps(apps, query, hidden)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchEnginePreference = settingsManager.searchEngine.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WebSearchEngine.GOOGLE)

    val webResults = combine(_searchQuery, searchEnginePreference) { query, engine ->
        if (query.isBlank()) emptyList()
        else listOf(com.example.ui.search.WebResult(query, engine.buildUrl(query)))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val homeItems = repository.allHomeItems.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val recentlyUsedApps: StateFlow<List<AppInfo>> = combine(
        _allApps,
        repository.getRecentlyUsedApps(5)
    ) { all, recentlyUsed ->
        val packageMap = all.associateBy { it.packageName }
        recentlyUsed.mapNotNull { launch -> packageMap[launch.packageName] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suggestedFolders: StateFlow<List<SuggestedFolder>> = _allApps.map { apps ->
        AppCategorizer.getSuggestedFolders(apps)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val showLabels = settingsManager.showLabels.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val iconShape = settingsManager.iconShape.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "circle")
    val gridSize = settingsManager.gridSize.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "5x5")
    val weatherState = MutableStateFlow<String?>(null)
    val notificationCounts = NotificationStateManager.notificationCounts
    
    fun setSearchEngine(engine: WebSearchEngine) {
        viewModelScope.launch { settingsManager.setSearchEngine(engine) }
    }

    fun fetchWeather(lat: Double, lon: Double) {
        viewModelScope.launch {
            val repository = com.example.weather.WeatherRepository()
            val weather = repository.getWeather(lat, lon)
            if (weather != null) {
                weatherState.value = "${weather.current.temperature}°C, ${weather.current.weatherDescriptions.firstOrNull() ?: ""}"
            } else {
                weatherState.value = "Weather Unavailable"
            }
        }
    }
    
    val userSelectedProfile = settingsManager.deviceProfile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Auto")
    
    val effectiveProfile = userSelectedProfile.map { selected ->
        com.example.ui.DeviceProfiles.resolveProfile(selected, repository.context)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        com.example.ui.DeviceProfiles.detectProfile(repository.context)
    )

    fun autoFitDisplayToPhone(profileSelection: String = "Auto") {
        viewModelScope.launch {
            val targetProfile = com.example.ui.DeviceProfiles.resolveProfile(profileSelection, repository.context)
            settingsManager.applyDetectedDeviceSettings(targetProfile, profileSelection)
        }
    }

    fun setShowLabels(show: Boolean) {
        viewModelScope.launch { settingsManager.setShowLabels(show) }
    }

    fun setIconShape(shape: String) {
        viewModelScope.launch { settingsManager.setIconShape(shape) }
    }

    fun setGridSize(size: String) {
        viewModelScope.launch { settingsManager.setGridSize(size) }
    }


    init {
        loadApps()
        checkFirstRun()
    }

    private fun checkFirstRun() {
        viewModelScope.launch {
            val isFirst = settingsManager.isFirstRun.first()
            val detectedProfile = com.example.ui.DeviceProfiles.detectProfile(repository.context)
            if (isFirst) {
                settingsManager.applyDetectedDeviceSettings(detectedProfile, "Auto")
            }

            val existingItems = try {
                repository.getAllItemsList()
            } catch (e: Exception) {
                emptyList()
            }

            if (isFirst || existingItems.isEmpty()) {
                val apps = repository.getInstalledApps()
                if (apps.isNotEmpty()) {
                    val commonPackages = listOf(
                        "com.google.android.dialer", "com.android.contacts", "com.hihonor.contacts", "com.huawei.contacts",
                        "com.google.android.apps.messaging", "com.android.mms", "com.hihonor.mms",
                        "com.android.chrome", "com.hihonor.browser", "com.huawei.browser",
                        "com.google.android.apps.photos", "com.hihonor.photos", "com.huawei.photos", "com.android.gallery3d",
                        "com.google.android.gm", "com.hihonor.email",
                        "com.google.android.calendar", "com.hihonor.calendar",
                        "com.android.camera", "com.hihonor.camera", "com.huawei.camera",
                        "com.android.settings"
                    )

                    val maxDock = detectedProfile.dockItemsCount.coerceIn(4, 6)
                    val prioritizedDockApps = (apps.filter { it.packageName in commonPackages } + apps)
                        .distinctBy { it.packageName }
                        .take(maxDock)

                    prioritizedDockApps.forEachIndexed { dockIndex, app ->
                        repository.insertItem(
                            LauncherItem(
                                type = ItemType.APP,
                                screenIndex = 0,
                                cellX = dockIndex,
                                cellY = 0,
                                containerId = LauncherItem.CONTAINER_DOCK,
                                label = app.label,
                                packageName = app.packageName,
                                className = app.className
                            )
                        )
                    }

                    val dockPackages = prioritizedDockApps.map { it.packageName }.toSet()
                    val desktopApps = apps.filterNot { it.packageName in dockPackages }
                        .take(detectedProfile.gridColumns)

                    desktopApps.forEachIndexed { index, app ->
                        repository.insertItem(
                            LauncherItem(
                                type = ItemType.APP,
                                screenIndex = 0,
                                cellX = index % detectedProfile.gridColumns,
                                cellY = index / detectedProfile.gridColumns,
                                containerId = LauncherItem.CONTAINER_DESKTOP,
                                label = app.label,
                                packageName = app.packageName,
                                className = app.className
                            )
                        )
                    }

                    settingsManager.setFirstRun(false)
                }
            }
        }
    }

    fun loadApps() {
        viewModelScope.launch {
            _allApps.value = repository.getInstalledApps()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun recordLaunch(packageName: String) {
        viewModelScope.launch {
            repository.recordAppLaunch(packageName)
        }
    }

    fun addItemToHome(item: LauncherItem) {
        viewModelScope.launch {
            repository.insertItem(item)
        }
    }

    fun createFolder(name: String, screenIndex: Int, cellX: Int, cellY: Int, apps: List<LauncherItem>) {
        viewModelScope.launch {
            val folderId = repository.insertItem(
                LauncherItem(
                    type = ItemType.FOLDER,
                    screenIndex = screenIndex,
                    cellX = cellX,
                    cellY = cellY,
                    label = name,
                    containerId = LauncherItem.CONTAINER_DESKTOP
                )
            )
            
            apps.forEachIndexed { index, app ->
                repository.updateItem(
                    app.copy(
                        containerId = folderId,
                        rank = index,
                        screenIndex = -1, // Not on main screen grid anymore
                        cellX = -1,
                        cellY = -1
                    )
                )
            }
        }
    }

    fun renameFolder(folderId: Long, newName: String) {
        viewModelScope.launch {
            val items = homeItems.value
            items.find { it.id == folderId }?.let {
                repository.updateItem(it.copy(label = newName))
            }
        }
    }

    fun moveItemToFolder(itemId: Long, folderId: Long) {
        viewModelScope.launch {
            val items = homeItems.value
            val item = items.find { it.id == itemId }
            val folderItems = items.filter { it.containerId == folderId }
            
            item?.let {
                repository.updateItem(
                    it.copy(
                        containerId = folderId,
                        rank = folderItems.size,
                        screenIndex = -1,
                        cellX = -1,
                        cellY = -1
                    )
                )
            }
        }
    }

    fun updateItem(item: LauncherItem) {
        viewModelScope.launch {
            repository.updateItem(item)
        }
    }

    fun deleteItem(item: LauncherItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    fun removeAppReferences(packageName: String) {
        viewModelScope.launch {
            repository.deleteItemsByPackageName(packageName)
        }
    }

    fun resetLayout() {
        viewModelScope.launch {
            repository.deleteAllItems()
            settingsManager.setFirstRun(true)
            checkFirstRun()
        }
    }

    class Factory(
        private val repository: LauncherRepository,
        private val settingsManager: SettingsManager
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LauncherViewModel(repository, settingsManager) as T
        }
    }
}

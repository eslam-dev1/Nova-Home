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
        if (selected == "Auto") {
            com.example.ui.DeviceProfiles.detectProfile(repository.context)
        } else {
            if (selected == "Honor X9d Native") com.example.ui.DeviceProfiles.HONOR_X9D else com.example.ui.DeviceProfiles.PIXEL_9
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.ui.DeviceProfiles.PIXEL_9)

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
            if (settingsManager.isFirstRun.first()) {
                // Add some default apps to dock/desktop if found
                val apps = repository.getInstalledApps()
                val commonPackages = listOf(
                    "com.google.android.dialer", "com.android.contacts",
                    "com.google.android.apps.messaging", "com.android.mms",
                    "com.android.chrome", "com.google.android.apps.photos",
                    "com.google.android.gm", "com.google.android.calendar",
                    "com.google.android.googlequicksearchbox"
                )
                
                var dockIndex = 0
                apps.filter { it.packageName in commonPackages }.forEach { app ->
                    if (dockIndex < 5) {
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
                        dockIndex++
                    }
                }
                settingsManager.setFirstRun(false)
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

package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_settings")

class SettingsManager(
    private val context: Context,
    private val dataStore: DataStore<Preferences> = context.dataStore
) {

    companion object {
        val SHOW_LABELS = booleanPreferencesKey("show_labels")
        val ICON_SHAPE = stringPreferencesKey("icon_shape")
        val GRID_SIZE = stringPreferencesKey("grid_size")
        val DEVICE_PROFILE = stringPreferencesKey("device_profile")
        val EDGE_BACK_GESTURE = booleanPreferencesKey("edge_back_gesture")
        val ICON_PACK = stringPreferencesKey("icon_pack")
        val HIDDEN_APPS = stringSetPreferencesKey("hidden_apps")
        val CONTEXTUAL_MODE = stringPreferencesKey("contextual_mode")
        val IS_FIRST_RUN = booleanPreferencesKey("is_first_run")
        val SEARCH_ENGINE = stringPreferencesKey("search_engine")
    }

    val showLabels: Flow<Boolean> = dataStore.data.map { it[SHOW_LABELS] ?: true }
    val iconShape: Flow<String> = dataStore.data.map { it[ICON_SHAPE] ?: "circle" }
    val gridSize: Flow<String> = dataStore.data.map { it[GRID_SIZE] ?: "5x5" }
    val edgeBackGesture: Flow<Boolean> = dataStore.data.map { it[EDGE_BACK_GESTURE] ?: true }
    val deviceProfile: Flow<String> = dataStore.data.map { it[DEVICE_PROFILE] ?: "Auto" }
    val iconPack: Flow<String?> = dataStore.data.map { it[ICON_PACK] }
    val hiddenApps: Flow<Set<String>> = dataStore.data.map { it[HIDDEN_APPS] ?: emptySet() }
    val contextualMode: Flow<String> = dataStore.data.map { it[CONTEXTUAL_MODE] ?: "Auto" }
    val isFirstRun: Flow<Boolean> = dataStore.data.map { it[IS_FIRST_RUN] ?: true }
    val searchEngine: Flow<WebSearchEngine> = dataStore.data.map { 
        val name = it[SEARCH_ENGINE] ?: WebSearchEngine.GOOGLE.name
        try { WebSearchEngine.valueOf(name) } catch (e: Exception) { WebSearchEngine.GOOGLE }
    }

    suspend fun setShowLabels(show: Boolean) {
        dataStore.edit { it[SHOW_LABELS] = show }
    }

    suspend fun setSearchEngine(engine: WebSearchEngine) {
        dataStore.edit { it[SEARCH_ENGINE] = engine.name }
    }

    suspend fun setIconShape(shape: String) {
        dataStore.edit { it[ICON_SHAPE] = shape }
    }

    suspend fun setGridSize(size: String) {
        dataStore.edit { it[GRID_SIZE] = size }
    }

    suspend fun setEdgeBackGesture(enabled: Boolean) {
        dataStore.edit { it[EDGE_BACK_GESTURE] = enabled }
    }

    suspend fun setDeviceProfile(profile: String) {
        dataStore.edit { it[DEVICE_PROFILE] = profile }
    }

    suspend fun applyDetectedDeviceSettings(
        profile: com.example.ui.DeviceProfile,
        profileSelection: String = "Auto"
    ) {
        dataStore.edit {
            it[DEVICE_PROFILE] = profileSelection
            it[GRID_SIZE] = profile.recommendedGridSize
            it[ICON_SHAPE] = profile.recommendedIconShape
            it[SHOW_LABELS] = true
        }
    }

    suspend fun setContextualMode(mode: String) {
        dataStore.edit { it[CONTEXTUAL_MODE] = mode }
    }

    val iconPackPackageName: Flow<String?> = dataStore.data.map { it[ICON_PACK] }

    suspend fun setIconPack(packageName: String?) {
        dataStore.edit { 
            if (packageName == null) it.remove(ICON_PACK)
            else it[ICON_PACK] = packageName 
        }
    }

    suspend fun setFirstRun(isFirstRun: Boolean) {
        dataStore.edit { it[IS_FIRST_RUN] = isFirstRun }
    }


    suspend fun hideApp(packageName: String) {
        dataStore.edit {
            val current = it[HIDDEN_APPS] ?: emptySet()
            it[HIDDEN_APPS] = current + packageName
        }
    }

    suspend fun unhideApp(packageName: String) {
        dataStore.edit {
            val current = it[HIDDEN_APPS] ?: emptySet()
            it[HIDDEN_APPS] = current - packageName
        }
    }
}

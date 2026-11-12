package com.example

import android.app.Application
import android.util.Log
import androidx.room.Room
import com.example.data.LauncherDatabase
import com.example.data.LauncherRepository
import com.example.data.SettingsManager
import com.example.ui.AdaptiveIconHelper
import com.example.ui.IconLoader
import com.example.ui.IconPackHelper

class LauncherApplication : Application() {
    lateinit var database: LauncherDatabase
    lateinit var repository: LauncherRepository
    lateinit var settingsManager: SettingsManager
    lateinit var adaptiveIconHelper: AdaptiveIconHelper
    lateinit var iconPackHelper: IconPackHelper
    lateinit var iconLoader: IconLoader

    override fun onCreate() {
        super.onCreate()
        Log.d("NovaHome", "LauncherApplication onCreate")
        try {
            database = Room.databaseBuilder(
                this,
                LauncherDatabase::class.java,
                "launcher_database"
            ).addMigrations(LauncherDatabase.MIGRATION_1_2, LauncherDatabase.MIGRATION_2_3, LauncherDatabase.MIGRATION_3_4)
            .build()
            Log.d("NovaHome", "Database initialized")
        } catch (e: Exception) {
            Log.e("NovaHome", "Database initialization failed", e)
        }
        
        repository = LauncherRepository(this, database.launcherDao())
        settingsManager = SettingsManager(this)
        iconPackHelper = com.example.ui.IconPackHelper(this)
        adaptiveIconHelper = com.example.ui.AdaptiveIconHelper(this, settingsManager, iconPackHelper)
        iconLoader = com.example.ui.IconLoader(this)
    }
}

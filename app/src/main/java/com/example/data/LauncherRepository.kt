package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class LauncherRepository(
    val context: Context,
    private val launcherDao: LauncherDao
) {
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager

    val allHomeItems: Flow<List<LauncherItem>> = launcherDao.getAllItems()

    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val apps = mutableListOf<AppInfo>()
        val profiles = try {
            userManager.userProfiles
        } catch (e: Exception) {
            Log.w("LauncherRepository", "Failed to read userProfiles, falling back to myUserHandle", e)
            listOf(Process.myUserHandle())
        }

        for (profile in profiles) {
            try {
                val isWorkProfile = isWorkProfile(profile)
                val activities = launcherApps.getActivityList(null, profile)
                for (activity in activities) {
                    apps.add(
                        AppInfo(
                            label = activity.label.toString(),
                            packageName = activity.applicationInfo.packageName,
                            className = activity.name,
                            icon = null, // Lazy loaded on demand by IconLoader
                            userHandle = profile,
                            isWorkProfile = isWorkProfile
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w("LauncherRepository", "Failed to query apps for profile $profile", e)
            }
        }

        if (apps.isEmpty()) {
            try {
                val pm = context.packageManager
                val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                val resolveInfos = pm.queryIntentActivities(launcherIntent, 0)
                val mainUser = Process.myUserHandle()
                for (info in resolveInfos) {
                    val activityInfo = info.activityInfo ?: continue
                    apps.add(
                        AppInfo(
                            label = info.loadLabel(pm).toString(),
                            packageName = activityInfo.packageName,
                            className = activityInfo.name,
                            icon = null,
                            userHandle = mainUser,
                            isWorkProfile = false
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("LauncherRepository", "Fallback queryIntentActivities failed", e)
            }
        }

        apps.sortBy { it.label.lowercase() }
        apps
    }

    private fun isWorkProfile(userHandle: UserHandle): Boolean {
        return userHandle != Process.myUserHandle()
    }

    suspend fun insertItem(item: LauncherItem) = launcherDao.insertItem(item)
    suspend fun updateItem(item: LauncherItem) = launcherDao.updateItem(item)
    suspend fun deleteItem(item: LauncherItem) = launcherDao.deleteItem(item)
    suspend fun deleteItemById(id: Long) = launcherDao.deleteItemById(id)
    suspend fun deleteAllItems() = launcherDao.deleteAllItems()
    suspend fun deleteAllFolders() = launcherDao.deleteAllFolders()
    suspend fun insertFolder(folder: FolderEntity) = launcherDao.insertFolder(folder)
    suspend fun deleteItemsByPackageName(packageName: String) = launcherDao.deleteItemsByPackageName(packageName)

    suspend fun recordAppLaunch(packageName: String) {
        launcherDao.insertAppLaunch(AppLaunch(packageName, System.currentTimeMillis()))
    }

    fun getRecentlyUsedApps(limit: Int): Flow<List<AppLaunch>> = launcherDao.getRecentlyUsedApps(limit)

    suspend fun getAllItemsList(): List<LauncherItem> = launcherDao.getAllItemsList()
    suspend fun getAllFoldersList(): List<FolderEntity> = launcherDao.getAllFoldersList()

    fun registerAppChangeListener(callback: LauncherApps.Callback) {
        launcherApps.registerCallback(callback)
    }

    fun unregisterAppChangeListener(callback: LauncherApps.Callback) {
        launcherApps.unregisterCallback(callback)
    }
}

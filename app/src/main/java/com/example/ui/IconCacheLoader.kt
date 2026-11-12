package com.example.ui

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class IconCache(maxEntries: Int = 200) {
    private val lruCache = object : LruCache<String, Drawable>(maxEntries) {}

    fun get(key: String): Drawable? = lruCache.get(key)
    fun put(key: String, drawable: Drawable) {
        lruCache.put(key, drawable)
    }
}

class IconLoader(private val context: Context) {
    private val cache = IconCache(200)
    private val mutex = Mutex()

    suspend fun loadIcon(packageName: String, className: String, user: android.os.UserHandle? = null): Drawable? {
        val key = "$packageName/$className"
        
        // 1. Check cache
        cache.get(key)?.let { return it }

        // 2. Load on IO thread with mutex deduplication
        return mutex.withLock {
            cache.get(key)?.let { return@withLock it }

            val drawable = withContext(Dispatchers.IO) {
                try {
                    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                    val handle = user ?: android.os.Process.myUserHandle()
                    val activities = launcherApps.getActivityList(packageName, handle)
                    val activity = activities.find { it.componentName.className == className }
                    activity?.getBadgedIcon(0) ?: context.packageManager.getApplicationIcon(packageName)
                } catch (e: Exception) {
                    try {
                        context.packageManager.getApplicationIcon(packageName)
                    } catch (ex: Exception) {
                        null
                    }
                }
            }

            if (drawable != null) {
                cache.put(key, drawable)
            }
            drawable
        }
    }
}

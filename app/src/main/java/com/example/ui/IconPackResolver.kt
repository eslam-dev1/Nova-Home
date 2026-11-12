package com.example.ui

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.LruCache
import com.example.data.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class IconPackResolver(
    private val context: Context,
    private val settings: SettingsManager,
    private val packageManager: PackageManager = context.packageManager
) {
    private val iconCache = LruCache<String, Drawable>(100)
    private val mutex = Mutex()

    suspend fun getIcon(packageName: String, className: String = ""): Drawable? {
        val cacheKey = "$packageName:$className"
        
        // 1. Check in-memory cache
        iconCache.get(cacheKey)?.let { return it }

        // 2. Thread-safe deduplication with Mutex
        return mutex.withLock {
            iconCache.get(cacheKey)?.let { return@withLock it }

            withContext(Dispatchers.IO) {
                var resolvedIcon: Drawable? = null
                val activePack = settings.iconPackPackageName.first()

                if (!activePack.isNullOrEmpty()) {
                    try {
                        val packContext = context.createPackageContext(activePack, Context.CONTEXT_IGNORE_SECURITY)
                        val resourceName = "ic_${packageName.replace(".", "_")}"
                        val resId = packContext.resources.getIdentifier(resourceName, "drawable", activePack)
                        if (resId != 0) {
                            resolvedIcon = packContext.resources.getDrawable(resId, null)
                        }
                    } catch (e: Exception) {
                        resolvedIcon = null
                    }
                }

                // 3. Fallback to original app icon from packageManager if no icon pack or app not in pack
                if (resolvedIcon == null) {
                    try {
                        resolvedIcon = packageManager.getApplicationIcon(packageName)
                    } catch (e: Exception) {
                        resolvedIcon = null
                    }
                }

                if (resolvedIcon != null) {
                    iconCache.put(cacheKey, resolvedIcon)
                }
                resolvedIcon
            }
        }
    }
}

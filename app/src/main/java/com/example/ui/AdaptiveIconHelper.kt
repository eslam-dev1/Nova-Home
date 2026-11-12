package com.example.ui

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Process
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import com.example.data.SettingsManager
import kotlinx.coroutines.flow.first

class AdaptiveIconHelper(
    private val context: Context,
    private val settingsManager: SettingsManager,
    private val iconPackHelper: IconPackHelper
) {
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val iconCache = LruCache<String, Bitmap>(100)

    suspend fun getAppIcon(packageName: String, className: String): Bitmap? {
        val cacheKey = "$packageName/$className"
        iconCache.get(cacheKey)?.let { return it }

        // Check for icon pack override
        val iconPack = settingsManager.iconPack.first()
        val overriddenIcon: Bitmap? = if (!iconPack.isNullOrEmpty()) {
            iconPackHelper.getIcon(packageName, iconPack)
        } else null
        
        if (overriddenIcon != null) {
            iconCache.put(cacheKey, overriddenIcon)
            return overriddenIcon
        }

        // Try to get from launcher
        val bitmap: Bitmap? = try {
            val activities = launcherApps.getActivityList(packageName, Process.myUserHandle())
            val activity = activities.find { it.name == className }
            val drawable = activity?.getIcon(0)
            if (drawable != null) drawableToBitmap(drawable) else null
        } catch (e: Exception) {
            null
        }

        if (bitmap != null) {
            iconCache.put(cacheKey, bitmap)
        }
        return bitmap
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        }
        return drawable.toBitmap()
    }
}

package com.example.ui

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap

class IconPackHelper(private val context: Context) {
    fun getInstalledIconPacks(): List<Pair<String, String>> {
        val intent = android.content.Intent("org.adw.launcher.icons")
        val resolveInfoList = context.packageManager.queryIntentActivities(intent, android.content.pm.PackageManager.GET_META_DATA)
        return resolveInfoList.map {
            it.activityInfo.packageName to it.loadLabel(context.packageManager).toString()
        }
    }

    fun getIcon(packageName: String, iconPackPackageName: String): Bitmap? {
        return try {
            val iconPackContext = context.createPackageContext(iconPackPackageName, Context.CONTEXT_IGNORE_SECURITY)
            // Simplified lookup - in reality, icon packs use appfilter.xml
            val resourceName = "ic_${packageName.replace(".", "_")}"
            val resId = iconPackContext.resources.getIdentifier(resourceName, "drawable", iconPackPackageName)
            
            if (resId != 0) {
                val drawable = iconPackContext.resources.getDrawable(resId, null)
                drawable.toBitmap()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

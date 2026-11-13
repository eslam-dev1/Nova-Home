package com.example.ui

import android.content.Context
import android.os.Build
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class DeviceProfile(
    val name: String,
    val cornerRadius: Dp,
    val cutoutSize: Dp,
    val gridColumns: Int,
    val iconSize: Dp,
    val recommendedGridSize: String = "${gridColumns}x${gridColumns}",
    val recommendedIconShape: String = "circle",
    val horizontalPadding: Dp = 16.dp,
    val dockItemsCount: Int = gridColumns
)

object DeviceProfiles {
    val COMPACT_PHONE = DeviceProfile(
        name = "Compact Phone",
        cornerRadius = 20.dp,
        cutoutSize = 12.dp,
        gridColumns = 4,
        iconSize = 50.dp,
        recommendedGridSize = "4x4",
        recommendedIconShape = "circle",
        horizontalPadding = 12.dp,
        dockItemsCount = 4
    )

    val PIXEL_9 = DeviceProfile(
        name = "Pixel 9 Style",
        cornerRadius = 24.dp,
        cutoutSize = 16.dp,
        gridColumns = 5,
        iconSize = 56.dp,
        recommendedGridSize = "5x5",
        recommendedIconShape = "circle",
        horizontalPadding = 16.dp,
        dockItemsCount = 5
    )

    val HONOR_X9D = DeviceProfile(
        name = "Honor X9d Native",
        cornerRadius = 32.dp,
        cutoutSize = 12.dp,
        gridColumns = 5,
        iconSize = 60.dp,
        recommendedGridSize = "5x5",
        recommendedIconShape = "squircle",
        horizontalPadding = 14.dp,
        dockItemsCount = 5
    )

    val LARGE_DISPLAY = DeviceProfile(
        name = "Large / Foldable Display",
        cornerRadius = 28.dp,
        cutoutSize = 16.dp,
        gridColumns = 6,
        iconSize = 64.dp,
        recommendedGridSize = "6x6",
        recommendedIconShape = "squircle",
        horizontalPadding = 24.dp,
        dockItemsCount = 6
    )

    val ALL_PROFILES = listOf(
        "Auto",
        COMPACT_PHONE.name,
        PIXEL_9.name,
        HONOR_X9D.name,
        LARGE_DISPLAY.name
    )

    fun detectProfile(context: Context): DeviceProfile {
        return try {
            val metrics = context.resources?.displayMetrics ?: return PIXEL_9
            val config = context.resources?.configuration
            val minPx = min(metrics.widthPixels, metrics.heightPixels).coerceAtLeast(1)
            val maxPx = max(metrics.widthPixels, metrics.heightPixels).coerceAtLeast(1)
            val aspectRatio = maxPx.toFloat() / minPx.toFloat()
            val density = metrics.densityDpi
            val densityScale = if (metrics.density > 0f) metrics.density else 1f
            val widthDp = minPx / densityScale
            val smallestWidthDp = if (config != null && config.smallestScreenWidthDp > 0) {
                config.smallestScreenWidthDp.toFloat()
            } else {
                widthDp
            }

            val manufacturer = (Build.MANUFACTURER ?: "").lowercase()
            val brand = (Build.BRAND ?: "").lowercase()
            val model = (Build.MODEL ?: "").lowercase()
            val isHonorOrHuawei = manufacturer.contains("honor") ||
                brand.contains("honor") ||
                model.contains("honor") ||
                manufacturer.contains("huawei") ||
                brand.contains("huawei")

            when {
                smallestWidthDp >= 600f || widthDp >= 600f -> LARGE_DISPLAY
                (smallestWidthDp > 0f && smallestWidthDp < 360f) || (widthDp > 0f && widthDp < 360f) -> COMPACT_PHONE
                isHonorOrHuawei || (aspectRatio >= 2.0f && density >= 400) -> HONOR_X9D
                else -> PIXEL_9
            }
        } catch (e: Exception) {
            PIXEL_9
        }
    }

    fun resolveProfile(selectedProfileName: String, context: Context): DeviceProfile {
        return when (selectedProfileName) {
            "Auto" -> detectProfile(context)
            COMPACT_PHONE.name -> COMPACT_PHONE
            HONOR_X9D.name -> HONOR_X9D
            LARGE_DISPLAY.name -> LARGE_DISPLAY
            PIXEL_9.name -> PIXEL_9
            else -> detectProfile(context)
        }
    }

    fun getDeviceSummary(context: Context): String {
        return try {
            val metrics = context.resources.displayMetrics
            val minPx = min(metrics.widthPixels, metrics.heightPixels).coerceAtLeast(1)
            val maxPx = max(metrics.widthPixels, metrics.heightPixels).coerceAtLeast(1)
            val densityScale = if (metrics.density > 0f) metrics.density else 1f
            val widthDp = (minPx / densityScale).roundToInt()
            val heightDp = (maxPx / densityScale).roundToInt()

            val manufacturer = (Build.MANUFACTURER ?: "Android").trim()
            val model = (Build.MODEL ?: "Phone").trim()
            val deviceTitle = if (model.startsWith(manufacturer, ignoreCase = true)) {
                model
            } else {
                "$manufacturer $model"
            }.trim()

            val detected = detectProfile(context)
            "$deviceTitle • ${minPx}×${maxPx}px (${widthDp}×${heightDp}dp, ${metrics.densityDpi}dpi) → ${detected.name}"
        } catch (e: Exception) {
            PIXEL_9.name
        }
    }
}

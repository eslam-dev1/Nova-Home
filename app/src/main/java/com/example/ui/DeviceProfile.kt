package com.example.ui

import android.content.Context
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class DeviceProfile(
    val name: String,
    val cornerRadius: Dp,
    val cutoutSize: Dp,
    val gridColumns: Int,
    val iconSize: Dp
)

object DeviceProfiles {
    val PIXEL_9 = DeviceProfile(
        name = "Pixel 9 Style",
        cornerRadius = 24.dp,
        cutoutSize = 16.dp,
        gridColumns = 5,
        iconSize = 56.dp
    )
    val HONOR_X9D = DeviceProfile(
        name = "Honor X9d Native",
        cornerRadius = 32.dp,
        cutoutSize = 12.dp,
        gridColumns = 5,
        iconSize = 64.dp
    )

    fun detectProfile(context: Context): DeviceProfile {
        val metrics = context.resources.displayMetrics
        val aspectRatio = metrics.heightPixels.toFloat() / metrics.widthPixels.toFloat()
        val density = metrics.densityDpi

        // Honor X9d: ~2.2 aspect ratio, ~420+ ppi
        return if (aspectRatio >= 2.0f && density >= 400) {
            HONOR_X9D
        } else {
            PIXEL_9
        }
    }
}

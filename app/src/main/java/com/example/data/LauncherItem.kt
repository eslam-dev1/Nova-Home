package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ItemType {
    APP, FOLDER, WIDGET
}

@Entity(tableName = "launcher_items")
data class LauncherItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: ItemType,
    val screenIndex: Int,
    val cellX: Int,
    val cellY: Int,
    val spanX: Int = 1,
    val spanY: Int = 1,
    val containerId: Long = CONTAINER_DESKTOP, // desktop or folder id
    val label: String? = null,
    val packageName: String? = null,
    val className: String? = null,
    val widgetId: Int? = null,
    val userSerialNumber: Long? = null,
    val rank: Int = 0, // for items inside folder
    val profileId: Long = 0 // for workspace profiles
) {
    companion object {
        const val CONTAINER_DESKTOP = -100L
        const val CONTAINER_DOCK = -101L
    }
}

package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val screenIndex: Int,
    val cellX: Int,
    val cellY: Int,
    val containerId: Long = LauncherItem.CONTAINER_DESKTOP
)

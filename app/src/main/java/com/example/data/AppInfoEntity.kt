package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_info")
data class AppInfoEntity(
    @PrimaryKey val packageName: String,
    val className: String,
    val label: String,
    val isHidden: Boolean = false,
    val customLabel: String? = null,
    val installTime: Long = System.currentTimeMillis()
)

package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_launches")
data class AppLaunch(
    @PrimaryKey val packageName: String,
    val lastLaunchTimestamp: Long
)

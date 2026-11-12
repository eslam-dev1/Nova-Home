package com.example.data

import android.graphics.drawable.Drawable

data class AppInfo(
    val label: String,
    val packageName: String,
    val className: String,
    val icon: Drawable? = null,
    val userHandle: android.os.UserHandle? = null,
    val isWorkProfile: Boolean = false
)

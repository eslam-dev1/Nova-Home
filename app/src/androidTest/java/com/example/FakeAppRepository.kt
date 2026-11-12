package com.example

import com.example.data.AppInfo

class FakeAppRepository {
    private val apps = mutableListOf<AppInfo>()

    init {
        apps.addAll(
            listOf(
                AppInfo(
                    label = "Gmail",
                    packageName = "com.google.android.gm",
                    className = "com.google.android.gm.ConversationListActivity"
                ),
                AppInfo(
                    label = "Instagram",
                    packageName = "com.instagram.android",
                    className = "com.instagram.mainactivity.MainActivity"
                ),
                AppInfo(
                    label = "Chrome",
                    packageName = "com.android.chrome",
                    className = "com.google.android.apps.chrome.Main"
                ),
                AppInfo(
                    label = "Calculator",
                    packageName = "com.google.android.calculator",
                    className = "com.android.calculator2.Calculator"
                )
            )
        )
    }

    fun getApps(): List<AppInfo> = apps.toList()

    fun setApps(newApps: List<AppInfo>) {
        apps.clear()
        apps.addAll(newApps)
    }
}

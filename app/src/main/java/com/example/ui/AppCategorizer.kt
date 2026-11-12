package com.example.ui

import com.example.data.AppInfo
import com.example.data.SuggestedFolder

object AppCategorizer {
    
    private val socialApps = setOf(
        "com.facebook.katana", "com.facebook.lite", "com.instagram.android", 
        "com.twitter.android", "com.snapchat.android", "com.whatsapp", 
        "com.discord", "com.linkedin.android", "com.reddit.frontpage"
    )
    
    private val gameApps = setOf(
        "com.supercell.clashofclans", "com.king.candycrushsaga", 
        "com.mojang.minecraftpe", "com.tencent.ig", "com.activision.callofduty.shooter"
    )
    
    private val productivityApps = setOf(
        "com.google.android.apps.docs", "com.google.android.apps.docs.editors.sheets",
        "com.google.android.apps.docs.editors.docs", "com.microsoft.office.word",
        "com.dropbox.android", "com.evernote"
    )

    fun getSuggestedFolders(apps: List<AppInfo>): List<SuggestedFolder> {
        val social = apps.filter { it.packageName in socialApps }
        val games = apps.filter { it.packageName in gameApps }
        val productivity = apps.filter { it.packageName in productivityApps }
        
        return listOfNotNull(
            if (social.size >= 2) SuggestedFolder("Social", social) else null,
            if (games.size >= 2) SuggestedFolder("Games", games) else null,
            if (productivity.size >= 2) SuggestedFolder("Productivity", productivity) else null
        )
    }
}

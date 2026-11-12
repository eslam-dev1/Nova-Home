package com.example.data

object AppCategorizer {
    private val SOCIAL_KEYWORDS = listOf("whatsapp", "facebook", "instagram", "twitter", "x", "snapchat", "messenger", "telegram", "tiktok", "linkedin")
    private val GAMES_KEYWORDS = listOf("game", "candy", "clash", "pubg", "asphalt", "minecraft", "roblox", "among", "angry")
    private val TOOLS_KEYWORDS = listOf("calculator", "calendar", "clock", "files", "browser", "chrome", "settings", "drive", "keep", "notes")
    private val MEDIA_KEYWORDS = listOf("youtube", "netflix", "spotify", "music", "video", "photos", "gallery", "prime", "hulu")

    fun getSuggestedFolders(apps: List<AppInfo>): List<SuggestedFolder> {
        val social = apps.filter { app -> SOCIAL_KEYWORDS.any { app.label.contains(it, ignoreCase = true) || app.packageName.contains(it, ignoreCase = true) } }
        val games = apps.filter { app -> GAMES_KEYWORDS.any { app.label.contains(it, ignoreCase = true) || app.packageName.contains(it, ignoreCase = true) } }
        val tools = apps.filter { app -> TOOLS_KEYWORDS.any { app.label.contains(it, ignoreCase = true) || app.packageName.contains(it, ignoreCase = true) } }
        val media = apps.filter { app -> MEDIA_KEYWORDS.any { app.label.contains(it, ignoreCase = true) || app.packageName.contains(it, ignoreCase = true) } }

        val suggestions = mutableListOf<SuggestedFolder>()
        if (social.size >= 2) suggestions.add(SuggestedFolder("Social", social))
        if (games.size >= 2) suggestions.add(SuggestedFolder("Games", games))
        if (tools.size >= 2) suggestions.add(SuggestedFolder("Tools", tools))
        if (media.size >= 2) suggestions.add(SuggestedFolder("Media", media))

        return suggestions
    }
}

data class SuggestedFolder(
    val name: String,
    val apps: List<AppInfo>
)

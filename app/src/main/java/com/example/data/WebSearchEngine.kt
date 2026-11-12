package com.example.data

import java.net.URLEncoder

enum class WebSearchEngine(val baseUrlTemplate: String, val displayName: String) {
    GOOGLE("https://www.google.com/search?q=%s", "Google"),
    DUCKDUCKGO("https://duckduckgo.com/?q=%s", "DuckDuckGo"),
    BING("https://www.bing.com/search?q=%s", "Bing");

    fun buildUrl(query: String): String {
        return try {
            baseUrlTemplate.format(URLEncoder.encode(query, "UTF-8"))
        } catch (e: Exception) {
            baseUrlTemplate.replace("%s", URLEncoder.encode(query, "UTF-8"))
        }
    }
}

package com.example.ui.search

import com.example.data.AppInfo

class SearchEngine {
    fun filterApps(
        apps: List<AppInfo>,
        query: String,
        hiddenPackages: Set<String> = emptySet()
    ): List<AppInfo> {
        val visibleApps = apps.filter { !hiddenPackages.contains(it.packageName) }
        if (query.isBlank()) {
            return visibleApps
        }

        val queryLower = query.lowercase().trim()
        
        val prefixMatches = mutableListOf<AppInfo>()
        val containsMatches = mutableListOf<AppInfo>()
        val fuzzyMatches = mutableListOf<AppInfo>()

        for (app in visibleApps) {
            val labelLower = app.label.lowercase()
            when {
                labelLower.startsWith(queryLower) -> prefixMatches.add(app)
                labelLower.contains(queryLower) -> containsMatches.add(app)
                isFuzzyMatch(labelLower, queryLower) -> fuzzyMatches.add(app)
            }
        }

        return (prefixMatches + containsMatches + fuzzyMatches).distinct()
    }

    private fun isFuzzyMatch(text: String, query: String): Boolean {
        if (query.length < 3) return false
        return levenshteinDistance(text, query) <= 2 || isSubsequence(query, text)
    }

    private fun isSubsequence(pattern: String, text: String): Boolean {
        var pIdx = 0
        var tIdx = 0
        while (pIdx < pattern.length && tIdx < text.length) {
            if (pattern[pIdx] == text[tIdx]) {
                pIdx++
            }
            tIdx++
        }
        return pIdx == pattern.length
    }

    private fun levenshteinDistance(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLength = lhs.length
        val rhsLength = rhs.length
        var cost = IntArray(lhsLength + 1) { it }
        var newCost = IntArray(lhsLength + 1) { 0 }

        for (i in 1..rhsLength) {
            newCost[0] = i
            for (j in 1..lhsLength) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                newCost[j] = minOf(
                    cost[j] + 1,
                    newCost[j - 1] + 1,
                    cost[j - 1] + match
                )
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhsLength]
    }
}

package com.example

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object NotificationStateManager {
    private val _notificationCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val notificationCounts: StateFlow<Map<String, Int>> = _notificationCounts.asStateFlow()

    fun updateNotificationCount(packageName: String, count: Int) {
        _notificationCounts.update { current ->
            if (count > 0) {
                current + (packageName to count)
            } else {
                current - packageName
            }
        }
    }
}

class LauncherNotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val currentCount = NotificationStateManager.notificationCounts.value[packageName] ?: 0
        NotificationStateManager.updateNotificationCount(packageName, currentCount + 1)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val currentCount = NotificationStateManager.notificationCounts.value[packageName] ?: 0
        if (currentCount > 0) {
            NotificationStateManager.updateNotificationCount(packageName, currentCount - 1)
        }
    }
}

package com.snoozeai.ainotificationagent.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.snoozeai.ainotificationagent.BuildConfig
import com.snoozeai.ainotificationagent.backend.ApiClient
import com.snoozeai.ainotificationagent.data.SnoozeRepository
import com.snoozeai.ainotificationagent.data.SnoozeDatabase
import com.snoozeai.ainotificationagent.data.Settings
import com.snoozeai.ainotificationagent.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SnoozeNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val repository by lazy {
        val api = ApiClient.create(BuildConfig.BASE_URL)
        val db = SnoozeDatabase.build(applicationContext)
        SnoozeRepository(api, db.snoozeDao())
    }

    private val settingsRepository by lazy { SettingsRepository(applicationContext) }

    @Volatile
    private var latestSettings: Settings? = null

    private val publisher by lazy { NotificationPublisher(applicationContext) }
    private val scheduler by lazy { SnoozeScheduler(applicationContext, publisher) }

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            settingsRepository.settings.collectLatest { settings ->
                latestSettings = settings
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val fromPackage = sbn.packageName ?: return
        if (fromPackage == packageName) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (text.isBlank() && title.isBlank()) return

        serviceScope.launch {
            try {
                val settings = latestSettings
                val item = repository.ingestNotification(
                    title = title,
                    body = text,
                    defaultSnoozeMinutes = settings?.defaultSnoozeMinutes ?: 60,
                    hints = settings?.hints
                )
                publisher.postSummary(item)
                scheduler.schedule(item, settings?.quietHours)
            } catch (t: Throwable) {
                Log.e("SnoozeNLS", "Failed to process notification", t)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.coroutineContext.cancel()
    }
}

package com.snoozeai.ainotificationagent.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.snoozeai.ainotificationagent.BuildConfig
import com.snoozeai.ainotificationagent.backend.ApiClient
import com.snoozeai.ainotificationagent.data.SettingsRepository
import com.snoozeai.ainotificationagent.data.SnoozeDatabase
import com.snoozeai.ainotificationagent.data.SnoozeRepository
import com.snoozeai.ainotificationagent.data.SnoozedItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

class SnoozeActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val minutes = intent.getLongExtra(KEY_MINUTES, 0L)
        val title = intent.getStringExtra(KEY_TITLE).orEmpty()
        val summary = intent.getStringExtra(KEY_SUMMARY).orEmpty()
        val id = intent.getStringExtra(KEY_ID).orEmpty()
        if (minutes <= 0 || summary.isBlank()) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val api = ApiClient.create(BuildConfig.BASE_URL)
                val db = SnoozeDatabase.build(context)
                val repo = SnoozeRepository(api, db.snoozeDao())
                val settingsRepo = SettingsRepository(context)
                val settings = settingsRepo.settings.firstOrNull()
                val item = SnoozedItem(
                    id = id.ifBlank { "local" },
                    title = title.ifBlank { "Snoozed" },
                    body = summary,
                    summary = summary,
                    urgency = null,
                    snoozeUntil = Instant.now().plusSeconds(minutes * 60)
                )
                repo.save(item)
                NotificationPublisher(context).postSummary(item)
                SnoozeScheduler(context, NotificationPublisher(context)).schedule(item, settings?.quietHours)
            } catch (t: Throwable) {
                Log.e("SnoozeAction", "Failed to handle snooze action", t)
            } finally {
                withContext(Dispatchers.Main) {
                    pending.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_SNOOZE = "com.snoozeai.ainotificationagent.SNOOZE"
        const val KEY_MINUTES = "minutes"
        const val KEY_TITLE = "title"
        const val KEY_SUMMARY = "summary"
        const val KEY_ID = "id"
    }
}

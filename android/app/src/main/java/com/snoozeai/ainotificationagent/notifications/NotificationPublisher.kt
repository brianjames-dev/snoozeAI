package com.snoozeai.ainotificationagent.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.snoozeai.ainotificationagent.R
import com.snoozeai.ainotificationagent.data.SnoozedItem
import java.util.UUID
import kotlin.random.Random

class NotificationPublisher(private val context: Context) {

    private val channelId = "snoozeai_summaries"

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_description)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun postSummary(item: SnoozedItem) {
        ensureChannel()

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(item.title)
            .setContentText(item.summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(item.summary))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(
                0,
                context.getString(R.string.action_snooze_15),
                snoozePendingIntent(item, 15)
            )
            .addAction(
                0,
                context.getString(R.string.action_snooze_60),
                snoozePendingIntent(item, 60)
            )
            .build()

        NotificationManagerCompat.from(context)
            .notify(Random.nextInt(0, Int.MAX_VALUE), notification)
    }

    private fun snoozePendingIntent(item: SnoozedItem, minutes: Int): PendingIntent {
        val intent = android.content.Intent(context, SnoozeActionReceiver::class.java).apply {
            action = SnoozeActionReceiver.ACTION_SNOOZE
            putExtra(SnoozeActionReceiver.KEY_MINUTES, minutes.toLong())
            putExtra(SnoozeActionReceiver.KEY_TITLE, item.title)
            putExtra(SnoozeActionReceiver.KEY_SUMMARY, item.summary)
            putExtra(SnoozeActionReceiver.KEY_ID, item.id.ifBlank { UUID.randomUUID().toString() })
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(
            context,
            (item.id.hashCode() + minutes),
            intent,
            flags
        )
    }
}

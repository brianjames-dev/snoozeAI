package com.snoozeai.ainotificationagent.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.snoozeai.ainotificationagent.data.QuietHours
import com.snoozeai.ainotificationagent.data.SnoozedItem
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class SnoozeScheduler(
    private val context: Context,
    private val publisher: NotificationPublisher
) {
    fun schedule(item: SnoozedItem, quietHours: QuietHours?) {
        val target = adjustForQuietHours(item.snoozeUntil, quietHours)
        val delaySeconds = Duration.between(Instant.now(), target).coerceAtLeast(Duration.ZERO)
        val work = OneTimeWorkRequestBuilder<SnoozeWorker>()
            .setInitialDelay(delaySeconds.seconds, TimeUnit.SECONDS)
            .setInputData(
                Data.Builder()
                    .putString(SnoozeWorker.KEY_TITLE, item.title)
                    .putString(SnoozeWorker.KEY_SUMMARY, item.summary)
                    .putLong(SnoozeWorker.KEY_TARGET_EPOCH, target.epochSecond)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueue(work)
    }

    private fun adjustForQuietHours(target: Instant, quietHours: QuietHours?): Instant {
        if (quietHours == null || !quietHours.enabled) return target
        val zone = ZoneId.systemDefault()
        val localTarget = target.atZone(zone)
        val start = quietHours.start
        val end = quietHours.end

        fun isQuiet(t: LocalTime): Boolean {
            return if (start < end) {
                t >= start && t < end
            } else {
                // spans midnight
                t >= start || t < end
            }
        }

        if (!isQuiet(localTarget.toLocalTime())) return target

        val nextEnd = if (start < end) {
            localTarget.toLocalDate().atTime(end)
        } else {
            // quiet spans midnight; end is next day
            localTarget.toLocalDate().plusDays(1).atTime(end)
        }
        return nextEnd.atZone(zone).toInstant()
    }
}

class SnoozeWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        val summary = inputData.getString(KEY_SUMMARY) ?: return Result.failure()
        val target = inputData.getLong(KEY_TARGET_EPOCH, 0L)
        val snoozeUntil = if (target > 0) Instant.ofEpochSecond(target) else Instant.now()
        NotificationPublisher(applicationContext).postSummary(
            SnoozedItem(
                id = "local",
                title = title,
                body = summary,
                summary = summary,
                urgency = null,
                snoozeUntil = snoozeUntil
            )
        )
        return Result.success()
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_SUMMARY = "summary"
        const val KEY_TARGET_EPOCH = "target_epoch"
    }
}

package com.snoozeai.ainotificationagent.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime

private val Context.dataStore by preferencesDataStore(name = "snooze_settings")

data class QuietHours(
    val enabled: Boolean,
    val start: LocalTime,
    val end: LocalTime
)

data class Settings(
    val quietHours: QuietHours,
    val defaultSnoozeMinutes: Long,
    val hints: List<String>
)

class SettingsRepository(private val context: Context) {

    private val quietEnabledKey = booleanPreferencesKey("quiet_enabled")
    private val quietStartKey = stringPreferencesKey("quiet_start")
    private val quietEndKey = stringPreferencesKey("quiet_end")
    private val defaultSnoozeKey = intPreferencesKey("default_snooze_minutes")
    private val hintsKey = stringPreferencesKey("hints_csv")

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        val quietEnabled = prefs[quietEnabledKey] ?: false
        val quietStart = LocalTime.parse(prefs[quietStartKey] ?: "09:00")
        val quietEnd = LocalTime.parse(prefs[quietEndKey] ?: "17:00")
        val defaultSnooze = prefs[defaultSnoozeKey]?.toLong() ?: 60L
        val hints = prefs[hintsKey].orEmpty()
            .takeIf { it.isNotBlank() }
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        Settings(
            quietHours = QuietHours(
                enabled = quietEnabled,
                start = quietStart,
                end = quietEnd
            ),
            defaultSnoozeMinutes = defaultSnooze,
            hints = hints
        )
    }

    suspend fun setQuietHours(enabled: Boolean, start: LocalTime, end: LocalTime) {
        context.dataStore.edit { prefs ->
            prefs[quietEnabledKey] = enabled
            prefs[quietStartKey] = start.toString()
            prefs[quietEndKey] = end.toString()
        }
    }

    suspend fun setDefaultSnooze(minutes: Long) {
        context.dataStore.edit { prefs ->
            prefs[defaultSnoozeKey] = minutes.toInt()
        }
    }

    suspend fun setHints(hints: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[hintsKey] = hints.joinToString(",") { it.trim() }
        }
    }
}

package com.snoozeai.ainotificationagent.data

import android.content.Context
import androidx.room.Database
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.snoozeai.ainotificationagent.backend.ApiService
import com.snoozeai.ainotificationagent.backend.ClassifyRequest
import com.snoozeai.ainotificationagent.backend.SummarizeRequest
import com.snoozeai.ainotificationagent.backend.StoreRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

@TypeConverters(InstantConverter::class)
@Database(entities = [SnoozeEntity::class], version = 2)
abstract class SnoozeDatabase : RoomDatabase() {
    abstract fun snoozeDao(): SnoozeDao

    companion object {
        fun build(context: Context): SnoozeDatabase =
            Room.databaseBuilder(context, SnoozeDatabase::class.java, "snoozes.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}

class InstantConverter {
    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.epochSecond

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochSecond(it) }
}

data class SnoozedItem(
    val id: String,
    val title: String,
    val body: String,
    val summary: String,
    val urgency: Double?,
    val snoozeUntil: Instant
)

@androidx.room.Entity(tableName = "snoozes")
data class SnoozeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val summary: String,
    val urgency: Double?,
    val snoozeUntil: Instant
) {
    fun toModel() = SnoozedItem(id, title, body, summary, urgency, snoozeUntil)

    companion object {
        fun fromModel(item: SnoozedItem) = SnoozeEntity(
            id = item.id,
            title = item.title,
            body = item.body,
            summary = item.summary,
            urgency = item.urgency,
            snoozeUntil = item.snoozeUntil
        )
    }
}

@Dao
interface SnoozeDao {
    @Query("SELECT * FROM snoozes ORDER BY snoozeUntil DESC")
    fun all(): Flow<List<SnoozeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SnoozeEntity)

    @Query("DELETE FROM snoozes WHERE id = :id")
    suspend fun delete(id: String)
}

class SnoozeRepository(
    private val api: ApiService,
    private val dao: SnoozeDao
) {
    val items: Flow<List<SnoozedItem>> = dao.all().map { rows -> rows.map { it.toModel() } }

    suspend fun syncLatest() {
        val remote = api.items().items.map { it.toModel() }
        remote.forEach { item -> dao.upsert(SnoozeEntity.fromModel(item)) }
    }

    suspend fun ingestNotification(
        title: String,
        body: String,
        defaultSnoozeMinutes: Long = 60,
        hints: List<String>? = null
    ): SnoozedItem {
        val id = UUID.randomUUID().toString()
        val summarize = api.summarize(SummarizeRequest(text = body))
        val classify = api.classify(ClassifyRequest(text = body, hints = hints))
        val snoozeUntil = Instant.now().plusSeconds(defaultSnoozeMinutes * 60)

        val item = SnoozedItem(
            id = id,
            title = title.ifBlank { "Notification" },
            body = body,
            summary = summarize.summary,
            urgency = classify.urgency.toDoubleOrNull(),
            snoozeUntil = snoozeUntil
        )

        api.store(
            StoreRequest(
                id = item.id,
                title = item.title,
            body = item.body,
            summary = item.summary,
            urgency = item.urgency?.toString(),
            snoozeUntil = DateTimeFormatter.ISO_INSTANT.format(item.snoozeUntil)
        )
        )
        dao.upsert(SnoozeEntity.fromModel(item))
        return item
    }

    suspend fun save(item: SnoozedItem) {
        dao.upsert(SnoozeEntity.fromModel(item))
    }

    suspend fun remove(id: String) {
        dao.delete(id)
    }
}

private fun com.snoozeai.ainotificationagent.backend.SnoozedItemResponse.toModel(): SnoozedItem {
    val parsedInstant = runCatching { Instant.parse(snoozeUntil) }.getOrDefault(Instant.now())
    return SnoozedItem(
        id = id,
        title = title,
        body = body ?: summary,
        summary = summary,
        urgency = urgency,
        snoozeUntil = parsedInstant
    )
}

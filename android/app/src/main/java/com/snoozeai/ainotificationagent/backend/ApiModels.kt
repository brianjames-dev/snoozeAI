package com.snoozeai.ainotificationagent.backend

import com.squareup.moshi.Json

data class SummarizeRequest(
    val text: String,
    @Json(name = "max_tokens") val maxTokens: Int = 80
)

data class SummarizeResponse(
    val summary: String
)

data class ClassifyRequest(
    val text: String,
    val hints: List<String>? = null
)

data class ClassifyResponse(
    val urgency: String,
    val label: String
)

data class StoreRequest(
    val id: String,
    val title: String,
    val body: String,
    val summary: String,
    val urgency: String? = null,
    val snoozeUntil: String
)

data class StoreResponse(
    val ok: Boolean,
    val id: String
)

data class SnoozedItemResponse(
    val id: String,
    val title: String,
    val body: String? = null,
    val summary: String,
    val urgency: Double?,
    val snoozeUntil: String
)

data class ItemsResponse(
    val items: List<SnoozedItemResponse>
)

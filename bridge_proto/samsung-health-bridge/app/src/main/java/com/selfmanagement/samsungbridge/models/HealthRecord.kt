package com.selfmanagement.samsungbridge.models

import com.google.gson.annotations.SerializedName

/**
 * Base canonical health record that normalizes Samsung Health data
 * into our application's standard format.
 *
 * All Samsung-specific data is transformed into this format before
 * leaving the Samsung layer. The backend never sees Samsung SDK objects.
 */
data class HealthRecord(
    /** Source integration identifier — always "samsung_health" for this bridge */
    @SerializedName("source")
    val source: String = "samsung_health",

    /** Samsung Health record UUID, used for deduplication */
    @SerializedName("source_record_id")
    val sourceRecordId: String?,

    /** Device name/identifier that produced this record */
    @SerializedName("source_device")
    val sourceDevice: String?,

    /** Record type: "steps", "heart_rate", "sleep", "exercise" */
    @SerializedName("type")
    val type: String,

    /** ISO-8601 timestamp when the measurement started */
    @SerializedName("timestamp_start")
    val timestampStart: String,

    /** ISO-8601 timestamp when the measurement ended (null for instant measurements) */
    @SerializedName("timestamp_end")
    val timestampEnd: String?,

    /** Primary numeric value (e.g., step count, BPM) */
    @SerializedName("value")
    val value: Double?,

    /** Unit of the value (e.g., "count", "bpm", "ms", "kcal", "m") */
    @SerializedName("unit")
    val unit: String?,

    /** Additional type-specific metadata */
    @SerializedName("metadata")
    val metadata: Map<String, Any?> = emptyMap()
) {
    companion object {
        const val SOURCE_SAMSUNG_HEALTH = "samsung_health"

        const val TYPE_STEPS = "steps"
        const val TYPE_HEART_RATE = "heart_rate"
        const val TYPE_SLEEP = "sleep"
        const val TYPE_EXERCISE = "exercise"

        const val UNIT_COUNT = "count"
        const val UNIT_BPM = "bpm"
        const val UNIT_MILLISECONDS = "ms"
        const val UNIT_KCAL = "kcal"
        const val UNIT_METERS = "m"
    }
}

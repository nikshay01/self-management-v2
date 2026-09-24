package com.selfmanagement.samsungbridge.models

import com.google.gson.annotations.SerializedName

/**
 * The complete JSON payload sent to the backend API.
 *
 * Format:
 * ```json
 * {
 *   "source": "samsung_health",
 *   "device": "galaxy_watch",
 *   "sync_started_at": "2026-09-24T07:30:00Z",
 *   "sync_range": { "start": "...", "end": "..." },
 *   "records": [ ... ]
 * }
 * ```
 */
data class HealthSyncPayload(
    /** Integration source — always "samsung_health" */
    @SerializedName("source")
    val source: String = HealthRecord.SOURCE_SAMSUNG_HEALTH,

    /** Primary device identifier, if known */
    @SerializedName("device")
    val device: String?,

    /** ISO-8601 timestamp when this sync operation started */
    @SerializedName("sync_started_at")
    val syncStartedAt: String,

    /** Time range that was queried */
    @SerializedName("sync_range")
    val syncRange: SyncRange,

    /** All normalized health records */
    @SerializedName("records")
    val records: List<HealthRecord>
)

/**
 * The time range covered by a sync operation.
 */
data class SyncRange(
    /** ISO-8601 start of the sync query window */
    @SerializedName("start")
    val start: String,

    /** ISO-8601 end of the sync query window */
    @SerializedName("end")
    val end: String
)

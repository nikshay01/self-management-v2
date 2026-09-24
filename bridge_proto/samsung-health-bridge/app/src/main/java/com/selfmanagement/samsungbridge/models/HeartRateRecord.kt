package com.selfmanagement.samsungbridge.models

/**
 * Strongly-typed heart rate record for display and local processing.
 * Represents a single heart rate sample from Samsung Health.
 *
 * The backend calculates average/min/max from raw samples —
 * we send individual samples rather than pre-computing aggregates
 * when raw data is available.
 */
data class HeartRateRecord(
    /** Samsung record UUID */
    val sourceRecordId: String?,
    /** Device that recorded the heart rate */
    val sourceDevice: String?,
    /** ISO-8601 start timestamp */
    val timestampStart: String,
    /** ISO-8601 end timestamp */
    val timestampEnd: String?,
    /** Heart rate in BPM */
    val heartRate: Double,
    /** Minimum HR in this measurement period, if available */
    val min: Double?,
    /** Maximum HR in this measurement period, if available */
    val max: Double?,
    /** Heart beat count during this measurement, if available */
    val heartBeatCount: Int?
) {
    /**
     * Convert to canonical [HealthRecord] for API transport.
     */
    fun toHealthRecord(): HealthRecord {
        val meta = mutableMapOf<String, Any?>()
        min?.let { meta["min_bpm"] = it }
        max?.let { meta["max_bpm"] = it }
        heartBeatCount?.let { meta["heart_beat_count"] = it }

        return HealthRecord(
            source = HealthRecord.SOURCE_SAMSUNG_HEALTH,
            sourceRecordId = sourceRecordId,
            sourceDevice = sourceDevice,
            type = HealthRecord.TYPE_HEART_RATE,
            timestampStart = timestampStart,
            timestampEnd = timestampEnd,
            value = heartRate,
            unit = HealthRecord.UNIT_BPM,
            metadata = meta
        )
    }
}

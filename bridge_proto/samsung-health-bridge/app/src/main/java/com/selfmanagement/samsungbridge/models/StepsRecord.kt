package com.selfmanagement.samsungbridge.models

/**
 * Strongly-typed steps record for display and local processing.
 * Created from Samsung Health step data before converting to [HealthRecord].
 */
data class StepsRecord(
    /** Samsung record UUID */
    val sourceRecordId: String?,
    /** Device that recorded the steps */
    val sourceDevice: String?,
    /** ISO-8601 start timestamp */
    val timestampStart: String,
    /** ISO-8601 end timestamp */
    val timestampEnd: String?,
    /** Number of steps */
    val count: Int,
    /** Calories burned (kcal), if available */
    val calories: Double?,
    /** Distance covered (meters), if available */
    val distance: Double?,
    /** Speed (m/s), if available */
    val speed: Double?
) {
    /**
     * Convert to canonical [HealthRecord] for API transport.
     */
    fun toHealthRecord(): HealthRecord {
        val meta = mutableMapOf<String, Any?>()
        calories?.let { meta["calories_kcal"] = it }
        distance?.let { meta["distance_m"] = it }
        speed?.let { meta["speed_ms"] = it }

        return HealthRecord(
            source = HealthRecord.SOURCE_SAMSUNG_HEALTH,
            sourceRecordId = sourceRecordId,
            sourceDevice = sourceDevice,
            type = HealthRecord.TYPE_STEPS,
            timestampStart = timestampStart,
            timestampEnd = timestampEnd,
            value = count.toDouble(),
            unit = HealthRecord.UNIT_COUNT,
            metadata = meta
        )
    }
}

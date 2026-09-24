package com.selfmanagement.samsungbridge.models

/**
 * Strongly-typed sleep record for display and local processing.
 *
 * Maps to Samsung Health's Sleep data type. Sleep stages (light/deep/REM/awake)
 * are stored in the [stages] list when available from SleepStage data.
 *
 * No "sleep quality" score is fabricated — only data Samsung actually provides.
 */
data class SleepRecord(
    /** Samsung record UUID */
    val sourceRecordId: String?,
    /** Device that recorded the sleep session */
    val sourceDevice: String?,
    /** ISO-8601 timestamp when the user went to bed */
    val timestampStart: String,
    /** ISO-8601 timestamp when the user woke up */
    val timestampEnd: String,
    /** Total sleep duration in milliseconds */
    val durationMs: Long,
    /** Sleep stages, if available from SleepStage data */
    val stages: List<SleepStageInfo> = emptyList()
) {
    /**
     * Convert to canonical [HealthRecord] for API transport.
     */
    fun toHealthRecord(): HealthRecord {
        val meta = mutableMapOf<String, Any?>()
        meta["duration_ms"] = durationMs

        if (stages.isNotEmpty()) {
            meta["stages"] = stages.map { stage ->
                mapOf(
                    "stage" to stage.stageName,
                    "start" to stage.timestampStart,
                    "end" to stage.timestampEnd,
                    "duration_ms" to stage.durationMs
                )
            }
        }

        return HealthRecord(
            source = HealthRecord.SOURCE_SAMSUNG_HEALTH,
            sourceRecordId = sourceRecordId,
            sourceDevice = sourceDevice,
            type = HealthRecord.TYPE_SLEEP,
            timestampStart = timestampStart,
            timestampEnd = timestampEnd,
            value = durationMs.toDouble(),
            unit = HealthRecord.UNIT_MILLISECONDS,
            metadata = meta
        )
    }
}

/**
 * Individual sleep stage within a sleep session.
 */
data class SleepStageInfo(
    /** Stage name: "awake", "light", "deep", "rem" */
    val stageName: String,
    /** ISO-8601 start timestamp */
    val timestampStart: String,
    /** ISO-8601 end timestamp */
    val timestampEnd: String,
    /** Duration of this stage in milliseconds */
    val durationMs: Long
) {
    companion object {
        const val STAGE_AWAKE = "awake"
        const val STAGE_LIGHT = "light"
        const val STAGE_DEEP = "deep"
        const val STAGE_REM = "rem"
        const val STAGE_UNKNOWN = "unknown"
    }
}

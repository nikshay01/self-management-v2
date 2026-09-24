package com.selfmanagement.samsungbridge.models

/**
 * Strongly-typed exercise record for display and local processing.
 *
 * All fields are nullable except timestamps, because Samsung Health
 * may not provide every field for every exercise type. We never
 * fabricate values that Samsung doesn't supply.
 */
data class ExerciseRecord(
    /** Samsung record UUID */
    val sourceRecordId: String?,
    /** Device that recorded the exercise */
    val sourceDevice: String?,
    /** ISO-8601 start timestamp */
    val timestampStart: String,
    /** ISO-8601 end timestamp */
    val timestampEnd: String?,
    /** Samsung exercise type code */
    val exerciseType: Int?,
    /** Human-readable exercise type name */
    val exerciseTypeName: String?,
    /** Custom exercise type string (if not a predefined type) */
    val customType: String?,
    /** Total duration in milliseconds */
    val durationMs: Long?,
    /** Distance in meters */
    val distanceM: Double?,
    /** Calories burned (kcal) */
    val caloriesKcal: Double?,
    /** Altitude gain in meters */
    val altitudeGainM: Double?
) {
    /**
     * Convert to canonical [HealthRecord] for API transport.
     */
    fun toHealthRecord(): HealthRecord {
        val meta = mutableMapOf<String, Any?>()
        exerciseType?.let { meta["exercise_type_code"] = it }
        exerciseTypeName?.let { meta["exercise_type_name"] = it }
        customType?.let { meta["custom_type"] = it }
        durationMs?.let { meta["duration_ms"] = it }
        distanceM?.let { meta["distance_m"] = it }
        caloriesKcal?.let { meta["calories_kcal"] = it }
        altitudeGainM?.let { meta["altitude_gain_m"] = it }

        return HealthRecord(
            source = HealthRecord.SOURCE_SAMSUNG_HEALTH,
            sourceRecordId = sourceRecordId,
            sourceDevice = sourceDevice,
            type = HealthRecord.TYPE_EXERCISE,
            timestampStart = timestampStart,
            timestampEnd = timestampEnd,
            value = durationMs?.toDouble(),
            unit = HealthRecord.UNIT_MILLISECONDS,
            metadata = meta
        )
    }

    companion object {
        /**
         * Map Samsung exercise type codes to human-readable names.
         * Based on Samsung Health SDK HealthConstants.Exercise exercise type constants.
         */
        fun exerciseTypeName(typeCode: Int): String {
            return when (typeCode) {
                1001 -> "Walking"
                1002 -> "Running"
                1003 -> "Baseball"
                1004 -> "Softball"
                1005 -> "Cricket"
                1006 -> "Golf"
                1007 -> "Billiards"
                1008 -> "Bowling"
                1009 -> "Hockey"
                1010 -> "Rugby"
                1011 -> "Basketball"
                1012 -> "Football"
                1013 -> "Handball"
                1014 -> "Soccer"
                1015 -> "Volleyball"
                1016 -> "Table Tennis"
                1017 -> "Squash"
                1018 -> "Badminton"
                1019 -> "Tennis"
                1020 -> "Cycling"
                1021 -> "Mountain Biking"
                1022 -> "Stationary Bike"
                2001 -> "Swimming"
                2002 -> "Aqua Aerobics"
                3001 -> "Hiking"
                3002 -> "Climbing"
                3003 -> "Rock Climbing"
                4001 -> "Yoga"
                4002 -> "Pilates"
                4003 -> "Stretching"
                5001 -> "Aerobics"
                5002 -> "Dance"
                5003 -> "Jump Rope"
                6001 -> "Rowing Machine"
                6002 -> "Elliptical Trainer"
                6003 -> "Stair Climber"
                10001 -> "Weightlifting"
                10002 -> "Calisthenics"
                10003 -> "Circuit Training"
                14001 -> "Martial Arts"
                14002 -> "Fencing"
                15001 -> "Skiing"
                15002 -> "Snowboarding"
                15003 -> "Ice Skating"
                else -> "Exercise ($typeCode)"
            }
        }
    }
}

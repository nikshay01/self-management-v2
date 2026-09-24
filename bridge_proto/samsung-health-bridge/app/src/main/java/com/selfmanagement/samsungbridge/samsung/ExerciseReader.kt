package com.selfmanagement.samsungbridge.samsung

import android.util.Log
import com.samsung.android.sdk.healthdata.HealthConstants
import com.samsung.android.sdk.healthdata.HealthDataResolver
import com.samsung.android.sdk.healthdata.HealthDataStore
import com.selfmanagement.samsungbridge.models.ExerciseRecord

/**
 * Reads exercise/activity data from Samsung Health.
 *
 * Retrieves exercise sessions with available metrics: type, duration,
 * distance, calories, altitude gain. Only fields that Samsung actually
 * provides are populated — nothing is fabricated.
 *
 * All Samsung SDK interaction is encapsulated here.
 */
class ExerciseReader(
    private val healthDataStore: HealthDataStore
) {
    companion object {
        private const val TAG = "ExerciseReader"
    }

    /**
     * Read exercise records within the specified time range.
     *
     * @param startTimeMillis Start of the query range (UTC milliseconds)
     * @param endTimeMillis End of the query range (UTC milliseconds)
     * @param callback Called with the list of exercise records
     */
    fun readExercise(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (List<ExerciseRecord>) -> Unit
    ) {
        Log.d(TAG, "Reading exercise from ${SamsungDataMapper.toIso8601(startTimeMillis)} " +
                "to ${SamsungDataMapper.toIso8601(endTimeMillis)}")

        try {
            val resolver = HealthDataResolver(healthDataStore, null)

            val filter = HealthDataResolver.Filter.and(
                HealthDataResolver.Filter.greaterThanEquals(
                    HealthConstants.SessionMeasurement.START_TIME, startTimeMillis
                ),
                HealthDataResolver.Filter.lessThan(
                    HealthConstants.SessionMeasurement.START_TIME, endTimeMillis
                )
            )

            val request = HealthDataResolver.ReadRequest.Builder()
                .setDataType(HealthConstants.Exercise.HEALTH_DATA_TYPE)
                .setProperties(arrayOf(
                    HealthConstants.Common.UUID,
                    HealthConstants.Common.DEVICE_UUID,
                    HealthConstants.Common.PACKAGE_NAME,
                    HealthConstants.SessionMeasurement.START_TIME,
                    HealthConstants.SessionMeasurement.END_TIME,
                    HealthConstants.SessionMeasurement.TIME_OFFSET,
                    HealthConstants.Exercise.EXERCISE_TYPE,
                    HealthConstants.Exercise.EXERCISE_CUSTOM_TYPE,
                    HealthConstants.Exercise.CALORIE,
                    HealthConstants.Exercise.DURATION,
                    HealthConstants.Exercise.DISTANCE,
                    HealthConstants.Exercise.ALTITUDE_GAIN
                ))
                .setFilter(filter)
                .build()

            resolver.read(request).setResultListener { result ->
                val records = mutableListOf<ExerciseRecord>()

                try {
                    val iterator = result.iterator()
                    while (iterator.hasNext()) {
                        val data = iterator.next()
                        try {
                            val record = mapToExerciseRecord(data)
                            if (record != null) {
                                records.add(record)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Skipping malformed exercise record", e)
                        }
                    }
                    Log.d(TAG, "Retrieved ${records.size} exercise records")
                } catch (e: Exception) {
                    Log.e(TAG, "Error iterating exercise results", e)
                } finally {
                    result.close()
                }

                callback(records)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to read exercise", e)
            callback(emptyList())
        }
    }

    /**
     * Map a Samsung HealthData record to our [ExerciseRecord] model.
     * Returns null if the record is missing the start time.
     */
    private fun mapToExerciseRecord(
        data: HealthDataResolver.ReadResult.DataIterator.HealthData
    ): ExerciseRecord? {
        val startTime = SamsungDataMapper.extractStartTime(data) ?: return null

        val exerciseType = SamsungDataMapper.safeGetInt(data, HealthConstants.Exercise.EXERCISE_TYPE)
        val customType = SamsungDataMapper.safeGetString(data, HealthConstants.Exercise.EXERCISE_CUSTOM_TYPE)
        val durationMs = SamsungDataMapper.safeGetLong(data, HealthConstants.Exercise.DURATION)

        return ExerciseRecord(
            sourceRecordId = SamsungDataMapper.extractRecordId(
                data, HealthConstants.Exercise.HEALTH_DATA_TYPE
            ),
            sourceDevice = SamsungDataMapper.extractDevice(data),
            timestampStart = startTime,
            timestampEnd = SamsungDataMapper.extractEndTime(data),
            exerciseType = exerciseType,
            exerciseTypeName = exerciseType?.let { ExerciseRecord.exerciseTypeName(it) },
            customType = customType,
            durationMs = durationMs,
            distanceM = SamsungDataMapper.safeGetFloat(data, HealthConstants.Exercise.DISTANCE)?.toDouble(),
            caloriesKcal = SamsungDataMapper.safeGetFloat(data, HealthConstants.Exercise.CALORIE)?.toDouble(),
            altitudeGainM = SamsungDataMapper.safeGetFloat(data, HealthConstants.Exercise.ALTITUDE_GAIN)?.toDouble()
        )
    }
}

package com.selfmanagement.samsungbridge.samsung

import android.util.Log
import com.samsung.android.sdk.healthdata.HealthConstants
import com.samsung.android.sdk.healthdata.HealthDataResolver
import com.samsung.android.sdk.healthdata.HealthDataStore
import com.selfmanagement.samsungbridge.models.StepsRecord

/**
 * Reads step count data from Samsung Health.
 *
 * Uses HealthConstants.StepCount to query individual step records
 * within a given time range. Each record represents a measurement
 * interval (e.g., 5 minutes of walking).
 *
 * All Samsung SDK interaction is encapsulated here — only [StepsRecord]
 * objects leave this class.
 */
class StepReader(
    private val healthDataStore: HealthDataStore
) {
    companion object {
        private const val TAG = "StepReader"
    }

    /**
     * Read step count records within the specified time range.
     *
     * @param startTimeMillis Start of the query range (UTC milliseconds)
     * @param endTimeMillis End of the query range (UTC milliseconds)
     * @param callback Called with the list of step records, or empty list on error
     */
    fun readSteps(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (List<StepsRecord>) -> Unit
    ) {
        Log.d(TAG, "Reading steps from ${SamsungDataMapper.toIso8601(startTimeMillis)} " +
                "to ${SamsungDataMapper.toIso8601(endTimeMillis)}")

        try {
            val resolver = HealthDataResolver(healthDataStore, null)

            // Build a time-range filter for step count records
            val filter = HealthDataResolver.Filter.and(
                HealthDataResolver.Filter.greaterThanEquals(
                    HealthConstants.SessionMeasurement.START_TIME, startTimeMillis
                ),
                HealthDataResolver.Filter.lessThan(
                    HealthConstants.SessionMeasurement.START_TIME, endTimeMillis
                )
            )

            val request = HealthDataResolver.ReadRequest.Builder()
                .setDataType(HealthConstants.StepCount.HEALTH_DATA_TYPE)
                .setProperties(arrayOf(
                    HealthConstants.Common.UUID,
                    HealthConstants.Common.DEVICE_UUID,
                    HealthConstants.Common.PACKAGE_NAME,
                    HealthConstants.SessionMeasurement.START_TIME,
                    HealthConstants.SessionMeasurement.END_TIME,
                    HealthConstants.SessionMeasurement.TIME_OFFSET,
                    HealthConstants.StepCount.COUNT,
                    HealthConstants.StepCount.CALORIE,
                    HealthConstants.StepCount.DISTANCE,
                    HealthConstants.StepCount.SPEED
                ))
                .setFilter(filter)
                .build()

            resolver.read(request).setResultListener { result ->
                val records = mutableListOf<StepsRecord>()

                try {
                    val iterator = result.iterator()
                    while (iterator.hasNext()) {
                        val data = iterator.next()
                        try {
                            val record = mapToStepsRecord(data)
                            if (record != null) {
                                records.add(record)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Skipping malformed step record", e)
                        }
                    }
                    Log.d(TAG, "Retrieved ${records.size} step records")
                } catch (e: Exception) {
                    Log.e(TAG, "Error iterating step results", e)
                } finally {
                    result.close()
                }

                callback(records)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to read steps", e)
            callback(emptyList())
        }
    }

    /**
     * Read aggregate (total) step count for the specified time range.
     * Useful for displaying "Steps today: X" without iterating all records.
     *
     * @param startTimeMillis Start of the query range (UTC milliseconds)
     * @param endTimeMillis End of the query range (UTC milliseconds)
     * @param callback Called with the total step count, or null on error
     */
    fun readTotalSteps(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (Int?) -> Unit
    ) {
        Log.d(TAG, "Reading total steps for range")

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

            val request = HealthDataResolver.AggregateRequest.Builder()
                .setDataType(HealthConstants.StepCount.HEALTH_DATA_TYPE)
                .addFunction(
                    HealthDataResolver.AggregateRequest.AggregateFunction.SUM,
                    HealthConstants.StepCount.COUNT,
                    "total_steps"
                )
                .setFilter(filter)
                .build()

            resolver.aggregate(request).setResultListener { result ->
                try {
                    val iterator = result.iterator()
                    if (iterator.hasNext()) {
                        val data = iterator.next()
                        val totalSteps = SamsungDataMapper.safeGetInt(data, "total_steps")
                        Log.d(TAG, "Total steps: $totalSteps")
                        callback(totalSteps)
                    } else {
                        Log.d(TAG, "No aggregate step data")
                        callback(null)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading aggregate steps", e)
                    callback(null)
                } finally {
                    result.close()
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to aggregate steps", e)
            callback(null)
        }
    }

    /**
     * Map a Samsung HealthData record to our [StepsRecord] model.
     * Returns null if the record is missing critical fields.
     */
    private fun mapToStepsRecord(
        data: HealthDataResolver.ReadResult.DataIterator.HealthData
    ): StepsRecord? {
        val startTime = SamsungDataMapper.extractStartTime(data) ?: return null
        val count = SamsungDataMapper.safeGetInt(data, HealthConstants.StepCount.COUNT) ?: return null

        return StepsRecord(
            sourceRecordId = SamsungDataMapper.extractRecordId(
                data, HealthConstants.StepCount.HEALTH_DATA_TYPE
            ),
            sourceDevice = SamsungDataMapper.extractDevice(data),
            timestampStart = startTime,
            timestampEnd = SamsungDataMapper.extractEndTime(data),
            count = count,
            calories = SamsungDataMapper.safeGetFloat(data, HealthConstants.StepCount.CALORIE)?.toDouble(),
            distance = SamsungDataMapper.safeGetFloat(data, HealthConstants.StepCount.DISTANCE)?.toDouble(),
            speed = SamsungDataMapper.safeGetFloat(data, HealthConstants.StepCount.SPEED)?.toDouble()
        )
    }
}

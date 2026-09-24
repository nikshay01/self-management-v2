package com.selfmanagement.samsungbridge.samsung

import android.util.Log
import com.samsung.android.sdk.healthdata.HealthConstants
import com.samsung.android.sdk.healthdata.HealthDataResolver
import com.samsung.android.sdk.healthdata.HealthDataStore
import com.selfmanagement.samsungbridge.models.HeartRateRecord

/**
 * Reads heart rate data from Samsung Health.
 *
 * Returns individual heart rate samples — the backend is responsible for
 * computing average/min/max from the raw data. This avoids incorrect
 * aggregation on the mobile side.
 *
 * All Samsung SDK interaction is encapsulated here.
 */
class HeartRateReader(
    private val healthDataStore: HealthDataStore
) {
    companion object {
        private const val TAG = "HeartRateReader"
    }

    /**
     * Read heart rate samples within the specified time range.
     *
     * @param startTimeMillis Start of the query range (UTC milliseconds)
     * @param endTimeMillis End of the query range (UTC milliseconds)
     * @param callback Called with the list of heart rate records
     */
    fun readHeartRate(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (List<HeartRateRecord>) -> Unit
    ) {
        Log.d(TAG, "Reading heart rate from ${SamsungDataMapper.toIso8601(startTimeMillis)} " +
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
                .setDataType(HealthConstants.HeartRate.HEALTH_DATA_TYPE)
                .setProperties(arrayOf(
                    HealthConstants.Common.UUID,
                    HealthConstants.Common.DEVICE_UUID,
                    HealthConstants.Common.PACKAGE_NAME,
                    HealthConstants.SessionMeasurement.START_TIME,
                    HealthConstants.SessionMeasurement.END_TIME,
                    HealthConstants.SessionMeasurement.TIME_OFFSET,
                    HealthConstants.HeartRate.HEART_RATE,
                    HealthConstants.HeartRate.HEART_BEAT_COUNT,
                    HealthConstants.HeartRate.MIN,
                    HealthConstants.HeartRate.MAX
                ))
                .setFilter(filter)
                .build()

            resolver.read(request).setResultListener { result ->
                val records = mutableListOf<HeartRateRecord>()

                try {
                    val iterator = result.iterator()
                    while (iterator.hasNext()) {
                        val data = iterator.next()
                        try {
                            val record = mapToHeartRateRecord(data)
                            if (record != null) {
                                records.add(record)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Skipping malformed heart rate record", e)
                        }
                    }
                    Log.d(TAG, "Retrieved ${records.size} heart rate records")
                } catch (e: Exception) {
                    Log.e(TAG, "Error iterating heart rate results", e)
                } finally {
                    result.close()
                }

                callback(records)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to read heart rate", e)
            callback(emptyList())
        }
    }

    /**
     * Map a Samsung HealthData record to our [HeartRateRecord] model.
     * Returns null if the record is missing the heart rate value.
     */
    private fun mapToHeartRateRecord(
        data: HealthDataResolver.ReadResult.DataIterator.HealthData
    ): HeartRateRecord? {
        val startTime = SamsungDataMapper.extractStartTime(data) ?: return null
        val heartRate = SamsungDataMapper.safeGetFloat(data, HealthConstants.HeartRate.HEART_RATE)
            ?.toDouble() ?: return null

        return HeartRateRecord(
            sourceRecordId = SamsungDataMapper.extractRecordId(
                data, HealthConstants.HeartRate.HEALTH_DATA_TYPE
            ),
            sourceDevice = SamsungDataMapper.extractDevice(data),
            timestampStart = startTime,
            timestampEnd = SamsungDataMapper.extractEndTime(data),
            heartRate = heartRate,
            min = SamsungDataMapper.safeGetFloat(data, HealthConstants.HeartRate.MIN)?.toDouble(),
            max = SamsungDataMapper.safeGetFloat(data, HealthConstants.HeartRate.MAX)?.toDouble(),
            heartBeatCount = SamsungDataMapper.safeGetInt(data, HealthConstants.HeartRate.HEART_BEAT_COUNT)
        )
    }
}

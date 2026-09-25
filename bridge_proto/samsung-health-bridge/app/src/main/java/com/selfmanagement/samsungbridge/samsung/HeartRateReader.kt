package com.selfmanagement.samsungbridge.samsung

import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.data.entries.HeartRate
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.InstantTimeFilter
import com.selfmanagement.samsungbridge.models.HeartRateRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Reads heart rate samples from Samsung Health using Samsung Health Data SDK v1.1.0.
 */
class HeartRateReader(
    private val healthDataStore: HealthDataStore
) {
    companion object {
        private const val TAG = "HeartRateReader"
    }

    /**
     * Read heart rate samples within the specified time range.
     */
    fun readHeartRate(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (List<HeartRateRecord>) -> Unit
    ) {
        val startInstant = Instant.ofEpochMilli(startTimeMillis)
        val endInstant = Instant.ofEpochMilli(endTimeMillis)

        Log.d(TAG, "Reading heart rate from $startInstant to $endInstant")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = DataTypes.HEART_RATE.readDataRequestBuilder
                    .setInstantTimeFilter(InstantTimeFilter.of(startInstant, endInstant))
                    .build()

                val response = healthDataStore.readData(request)
                val records = mutableListOf<HeartRateRecord>()

                for (point in response.dataList) {
                    val uid = SamsungDataMapper.extractRecordId(point, "heart_rate")
                    val device = SamsungDataMapper.extractDevice(point.dataSource)
                    val pointStart = SamsungDataMapper.formatInstant(point.startTime, point.zoneOffset)
                    val pointEnd = point.endTime?.let { SamsungDataMapper.formatInstant(it, point.zoneOffset) }
                    val hrValue = point.getValue(DataType.HeartRateType.HEART_RATE)?.toDouble()
                    val min = point.getValue(DataType.HeartRateType.MIN_HEART_RATE)?.toDouble()
                    val max = point.getValue(DataType.HeartRateType.MAX_HEART_RATE)?.toDouble()
                    val seriesData: List<HeartRate>? = point.getValue(DataType.HeartRateType.SERIES_DATA)

                    if (hrValue != null) {
                        records.add(
                            HeartRateRecord(
                                sourceRecordId = uid,
                                sourceDevice = device,
                                timestampStart = pointStart,
                                timestampEnd = pointEnd,
                                heartRate = hrValue,
                                min = min,
                                max = max,
                                heartBeatCount = null
                            )
                        )
                    } else if (!seriesData.isNullOrEmpty()) {
                        seriesData.forEachIndexed { index, sample ->
                            records.add(
                                HeartRateRecord(
                                    sourceRecordId = "${uid}_$index",
                                    sourceDevice = device,
                                    timestampStart = SamsungDataMapper.formatInstant(sample.startTime, point.zoneOffset),
                                    timestampEnd = SamsungDataMapper.formatInstant(sample.endTime, point.zoneOffset),
                                    heartRate = sample.heartRate.toDouble(),
                                    min = sample.min.toDouble(),
                                    max = sample.max.toDouble(),
                                    heartBeatCount = null
                                )
                            )
                        }
                    }
                }

                Log.d(TAG, "Retrieved ${records.size} heart rate records")
                callback(records)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read heart rate", e)
                callback(emptyList())
            }
        }
    }
}

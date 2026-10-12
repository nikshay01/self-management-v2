package com.selfmanagement.samsungbridge.samsung

import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.data.entries.HeartRate
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.selfmanagement.samsungbridge.models.HeartRateRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Reads heart rate samples from Samsung Health using Samsung Health Data SDK v1.1.0.
 */
class HeartRateReader(
    private val healthDataStore: HealthDataStore
) {
    companion object {
        private const val TAG = "HeartRateReader"
        private const val PAGE_SIZE = 1000
        private const val MAX_PAGES = 100
    }

    fun readHeartRate(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (List<HeartRateRecord>) -> Unit
    ) {
        readHeartRate(SyncTimeRange.fromEpochMillis(startTimeMillis, endTimeMillis), callback)
    }

    /**
     * Read heart rate samples within [range] using a [LocalTimeFilter].
     * Each sample timestamp is preserved; samples are not collapsed.
     */
    fun readHeartRate(
        range: SyncTimeRange,
        callback: (List<HeartRateRecord>) -> Unit
    ) {
        Log.d(TAG, "Reading heart rate for range $range")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val localTimeFilter = LocalTimeFilter.of(
                    range.startDateTime,
                    range.endDateTime,
                    true,
                    true
                )
                val records = mutableListOf<HeartRateRecord>()
                var pageToken: String? = null
                var pages = 0

                do {
                    val builder = DataTypes.HEART_RATE.readDataRequestBuilder
                        .setLocalTimeFilter(localTimeFilter)
                        .setPageSize(PAGE_SIZE)
                    if (!pageToken.isNullOrBlank()) {
                        builder.setPageToken(pageToken)
                    }

                    val response = healthDataStore.readData(builder.build())
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
                    pageToken = response.pageToken
                    pages++
                } while (!pageToken.isNullOrBlank() && pages < MAX_PAGES)

                if (!pageToken.isNullOrBlank()) {
                    Log.w(TAG, "Stopped after $MAX_PAGES pages; heart-rate samples may be incomplete")
                }
                Log.d(TAG, "Retrieved ${records.size} heart rate records")
                callback(records)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read heart rate for range $range", e)
                callback(emptyList())
            }
        }
    }
}

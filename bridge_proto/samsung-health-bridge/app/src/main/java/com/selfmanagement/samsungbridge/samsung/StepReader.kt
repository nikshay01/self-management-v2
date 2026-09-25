package com.selfmanagement.samsungbridge.samsung

import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.selfmanagement.samsungbridge.models.StepsRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Reads step count data from Samsung Health using Samsung Health Data SDK v1.1.0.
 */
class StepReader(
    private val healthDataStore: HealthDataStore
) {
    companion object {
        private const val TAG = "StepReader"
    }

    /**
     * Read step count records within the specified time range.
     * In Samsung Health Data SDK, steps are retrieved via AggregateRequest (TOTAL) on DataTypes.STEPS.
     */
    fun readSteps(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (List<StepsRecord>) -> Unit
    ) {
        val startInstant = Instant.ofEpochMilli(startTimeMillis)
        val endInstant = Instant.ofEpochMilli(endTimeMillis)
        val zone = ZoneId.systemDefault()
        val startLocal = LocalDateTime.ofInstant(startInstant, zone)
        val endLocal = LocalDateTime.ofInstant(endInstant, zone)

        Log.d(TAG, "Reading steps from $startLocal to $endLocal")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val aggregateBuilder = DataType.StepsType.TOTAL.requestBuilder
                val request = aggregateBuilder
                    .setLocalTimeFilter(LocalTimeFilter.of(startLocal, endLocal))
                    .build()

                val response = healthDataStore.aggregateData(request)
                val records = mutableListOf<StepsRecord>()

                for (aggregated in response.dataList) {
                    val count = aggregated.value?.toInt() ?: 0
                    if (count > 0) {
                        val recordStart = aggregated.startTime?.let { SamsungDataMapper.formatInstant(it) }
                            ?: SamsungDataMapper.toIso8601(startTimeMillis)
                        val recordEnd = aggregated.endTime?.let { SamsungDataMapper.formatInstant(it) }
                            ?: SamsungDataMapper.toIso8601(endTimeMillis)

                        records.add(
                            StepsRecord(
                                sourceRecordId = "steps_${startTimeMillis}_${endTimeMillis}",
                                sourceDevice = null,
                                timestampStart = recordStart,
                                timestampEnd = recordEnd,
                                count = count,
                                calories = null,
                                distance = null,
                                speed = null
                            )
                        )
                    }
                }
                Log.d(TAG, "Retrieved ${records.size} step aggregate records")
                callback(records)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read steps", e)
                callback(emptyList())
            }
        }
    }

    /**
     * Read total step count for the time range.
     */
    fun readTotalSteps(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (Int?) -> Unit
    ) {
        readSteps(startTimeMillis, endTimeMillis) { records ->
            if (records.isEmpty()) {
                callback(0)
            } else {
                callback(records.sumOf { it.count })
            }
        }
    }
}

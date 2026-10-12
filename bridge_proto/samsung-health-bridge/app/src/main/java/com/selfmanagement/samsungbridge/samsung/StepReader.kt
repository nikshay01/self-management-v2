package com.selfmanagement.samsungbridge.samsung

import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.samsung.android.sdk.health.data.request.LocalTimeGroup
import com.samsung.android.sdk.health.data.request.LocalTimeGroupUnit
import com.selfmanagement.samsungbridge.models.StepsRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Reads step count data from Samsung Health using Samsung Health Data SDK v1.1.0.
 */
class StepReader(
    private val healthDataStore: HealthDataStore
) {
    companion object {
        private const val TAG = "StepReader"
        private const val PAGE_SIZE = 100
        private const val MAX_PAGES = 50
    }

    fun readSteps(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (List<StepsRecord>) -> Unit
    ) {
        readSteps(SyncTimeRange.fromEpochMillis(startTimeMillis, endTimeMillis), callback)
    }

    /**
     * Read daily step totals within [range] using [DataType.StepsType.TOTAL]
     * with [LocalTimeFilter] and daily grouping.
     */
    fun readSteps(
        range: SyncTimeRange,
        callback: (List<StepsRecord>) -> Unit
    ) {
        Log.d(TAG, "Reading daily steps for range $range")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val localTimeFilter = LocalTimeFilter.of(
                    range.startDateTime,
                    range.endDateTime,
                    true,
                    true
                )
                val localTimeGroup = LocalTimeGroup.of(LocalTimeGroupUnit.DAILY, 1)
                val records = mutableListOf<StepsRecord>()
                var pageToken: String? = null
                var pages = 0

                do {
                    val builder = DataType.StepsType.TOTAL.requestBuilder
                        .setLocalTimeFilterWithGroup(localTimeFilter, localTimeGroup)
                        .setPageSize(PAGE_SIZE)
                    if (!pageToken.isNullOrBlank()) {
                        builder.setPageToken(pageToken)
                    }

                    val response = healthDataStore.aggregateData(builder.build())
                    for (aggregated in response.dataList) {
                        val count = aggregated.value?.toInt() ?: 0
                        if (count <= 0) continue

                        val dayStart = java.time.LocalDateTime.ofInstant(aggregated.startTime, range.zoneId)
                        val dayEnd = java.time.LocalDateTime.ofInstant(aggregated.endTime, range.zoneId)

                        val recordStart = SamsungDataMapper.formatLocalDateTime(dayStart, range.zoneId)
                        val recordEnd = SamsungDataMapper.formatLocalDateTime(dayEnd, range.zoneId)
                        val dayKey = dayStart.toLocalDate().toString()

                        records.add(
                            StepsRecord(
                                sourceRecordId = "steps_$dayKey",
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
                    pageToken = response.pageToken
                    pages++
                } while (!pageToken.isNullOrBlank() && pages < MAX_PAGES)

                if (!pageToken.isNullOrBlank()) {
                    Log.w(TAG, "Stopped after $MAX_PAGES pages; daily step results may be incomplete")
                }
                records.sortBy { it.timestampStart }
                Log.d(TAG, "Retrieved ${records.size} daily step records")
                callback(records)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read steps for range $range", e)
                callback(emptyList())
            }
        }
    }

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

package com.selfmanagement.samsungbridge.samsung

import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.data.entries.SleepSession
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.selfmanagement.samsungbridge.models.SleepRecord
import com.selfmanagement.samsungbridge.models.SleepStageInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Reads sleep data from Samsung Health using Samsung Health Data SDK v1.1.0.
 */
class SleepReader(
    private val healthDataStore: HealthDataStore
) {
    companion object {
        private const val TAG = "SleepReader"
        private const val PAGE_SIZE = 200
        private const val MAX_PAGES = 50
    }

    fun readSleep(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (List<SleepRecord>) -> Unit
    ) {
        readSleep(SyncTimeRange.fromEpochMillis(startTimeMillis, endTimeMillis), callback)
    }

    fun readSleep(
        range: SyncTimeRange,
        callback: (List<SleepRecord>) -> Unit
    ) {
        Log.d(TAG, "Reading sleep for range $range")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val localTimeFilter = LocalTimeFilter.of(
                    range.startDateTime,
                    range.endDateTime,
                    true,
                    true
                )
                val records = mutableListOf<SleepRecord>()
                var pageToken: String? = null
                var pages = 0

                do {
                    val builder = DataTypes.SLEEP.readDataRequestBuilder
                        .setLocalTimeFilter(localTimeFilter)
                        .setPageSize(PAGE_SIZE)
                    if (!pageToken.isNullOrBlank()) {
                        builder.setPageToken(pageToken)
                    }

                    val response = healthDataStore.readData(builder.build())
                    for (point in response.dataList) {
                        val uid = SamsungDataMapper.extractRecordId(point, "sleep")
                        val device = SamsungDataMapper.extractDevice(point.dataSource)
                        val pointStart = SamsungDataMapper.formatInstant(point.startTime, point.zoneOffset)
                        val pointEnd = point.endTime?.let { SamsungDataMapper.formatInstant(it, point.zoneOffset) }
                            ?: pointStart

                        val durationMs = point.getValue(DataType.SleepType.DURATION)?.toMillis()
                            ?: (point.endTime?.toEpochMilli()?.minus(point.startTime.toEpochMilli()) ?: 0L)

                        val sessions: List<SleepSession>? = point.getValue(DataType.SleepType.SESSIONS)
                        val stageInfos = mutableListOf<SleepStageInfo>()

                        sessions?.forEach { session ->
                            session.stages?.forEach { stage ->
                                val stageName = when (stage.stage) {
                                    DataType.SleepType.StageType.AWAKE -> SleepStageInfo.STAGE_AWAKE
                                    DataType.SleepType.StageType.LIGHT -> SleepStageInfo.STAGE_LIGHT
                                    DataType.SleepType.StageType.DEEP -> SleepStageInfo.STAGE_DEEP
                                    DataType.SleepType.StageType.REM -> SleepStageInfo.STAGE_REM
                                    else -> SleepStageInfo.STAGE_UNKNOWN
                                }
                                val sStart = SamsungDataMapper.formatInstant(stage.startTime, point.zoneOffset)
                                val sEnd = SamsungDataMapper.formatInstant(stage.endTime, point.zoneOffset)
                                val stageDurationMs = stage.endTime.toEpochMilli() - stage.startTime.toEpochMilli()

                                stageInfos.add(
                                    SleepStageInfo(
                                        stageName = stageName,
                                        timestampStart = sStart,
                                        timestampEnd = sEnd,
                                        durationMs = stageDurationMs
                                    )
                                )
                            }
                        }

                        records.add(
                            SleepRecord(
                                sourceRecordId = uid,
                                sourceDevice = device,
                                timestampStart = pointStart,
                                timestampEnd = pointEnd,
                                durationMs = durationMs,
                                stages = stageInfos
                            )
                        )
                    }
                    pageToken = response.pageToken
                    pages++
                } while (!pageToken.isNullOrBlank() && pages < MAX_PAGES)

                if (!pageToken.isNullOrBlank()) {
                    Log.w(TAG, "Stopped after $MAX_PAGES pages; sleep sessions may be incomplete")
                }
                Log.d(TAG, "Retrieved ${records.size} sleep records")
                callback(records)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read sleep for range $range", e)
                callback(emptyList())
            }
        }
    }
}

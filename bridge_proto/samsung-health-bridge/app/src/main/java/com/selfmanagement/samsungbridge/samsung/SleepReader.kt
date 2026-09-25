package com.selfmanagement.samsungbridge.samsung

import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.data.entries.SleepSession
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.InstantTimeFilter
import com.selfmanagement.samsungbridge.models.SleepRecord
import com.selfmanagement.samsungbridge.models.SleepStageInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Reads sleep data from Samsung Health using Samsung Health Data SDK v1.1.0.
 */
class SleepReader(
    private val healthDataStore: HealthDataStore
) {
    companion object {
        private const val TAG = "SleepReader"
    }

    /**
     * Read sleep records within the specified time range.
     */
    fun readSleep(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (List<SleepRecord>) -> Unit
    ) {
        val startInstant = Instant.ofEpochMilli(startTimeMillis)
        val endInstant = Instant.ofEpochMilli(endTimeMillis)

        Log.d(TAG, "Reading sleep from $startInstant to $endInstant")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = DataTypes.SLEEP.readDataRequestBuilder
                    .setInstantTimeFilter(InstantTimeFilter.of(startInstant, endInstant))
                    .build()

                val response = healthDataStore.readData(request)
                val records = mutableListOf<SleepRecord>()

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

                Log.d(TAG, "Retrieved ${records.size} sleep records")
                callback(records)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read sleep", e)
                callback(emptyList())
            }
        }
    }
}

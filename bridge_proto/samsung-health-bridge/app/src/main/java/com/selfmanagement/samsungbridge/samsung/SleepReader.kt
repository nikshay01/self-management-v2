package com.selfmanagement.samsungbridge.samsung

import android.util.Log
import com.samsung.android.sdk.healthdata.HealthConstants
import com.samsung.android.sdk.healthdata.HealthDataResolver
import com.samsung.android.sdk.healthdata.HealthDataStore
import com.selfmanagement.samsungbridge.models.SleepRecord
import com.selfmanagement.samsungbridge.models.SleepStageInfo

/**
 * Reads sleep data from Samsung Health.
 *
 * Retrieves both top-level Sleep sessions and detailed SleepStage records.
 * Does NOT fabricate a "sleep quality" value — only provides what Samsung
 * actually records.
 *
 * Handles sleep sessions that cross midnight correctly by using
 * Samsung's START_TIME/END_TIME + TIME_OFFSET.
 */
class SleepReader(
    private val healthDataStore: HealthDataStore
) {
    companion object {
        private const val TAG = "SleepReader"
    }

    /**
     * Read sleep records within the specified time range.
     *
     * Queries both Sleep sessions and their associated SleepStage records.
     *
     * @param startTimeMillis Start of the query range (UTC milliseconds)
     * @param endTimeMillis End of the query range (UTC milliseconds)
     * @param callback Called with the list of sleep records
     */
    fun readSleep(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (List<SleepRecord>) -> Unit
    ) {
        Log.d(TAG, "Reading sleep from ${SamsungDataMapper.toIso8601(startTimeMillis)} " +
                "to ${SamsungDataMapper.toIso8601(endTimeMillis)}")

        try {
            val resolver = HealthDataResolver(healthDataStore, null)

            val filter = HealthDataResolver.Filter.and(
                HealthDataResolver.Filter.greaterThanEquals(
                    HealthConstants.SessionMeasurement.START_TIME, startTimeMillis
                ),
                HealthDataResolver.Filter.lessThan(
                    HealthConstants.SessionMeasurement.END_TIME, endTimeMillis
                )
            )

            val request = HealthDataResolver.ReadRequest.Builder()
                .setDataType(HealthConstants.Sleep.HEALTH_DATA_TYPE)
                .setProperties(arrayOf(
                    HealthConstants.Common.UUID,
                    HealthConstants.Common.DEVICE_UUID,
                    HealthConstants.Common.PACKAGE_NAME,
                    HealthConstants.SessionMeasurement.START_TIME,
                    HealthConstants.SessionMeasurement.END_TIME,
                    HealthConstants.SessionMeasurement.TIME_OFFSET
                ))
                .setFilter(filter)
                .build()

            resolver.read(request).setResultListener { result ->
                val sleepSessions = mutableListOf<SleepSessionData>()

                try {
                    val iterator = result.iterator()
                    while (iterator.hasNext()) {
                        val data = iterator.next()
                        try {
                            val session = parseSleepSession(data)
                            if (session != null) {
                                sleepSessions.add(session)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Skipping malformed sleep record", e)
                        }
                    }
                    Log.d(TAG, "Retrieved ${sleepSessions.size} sleep sessions")
                } catch (e: Exception) {
                    Log.e(TAG, "Error iterating sleep results", e)
                } finally {
                    result.close()
                }

                // Now read sleep stages for each session
                if (sleepSessions.isNotEmpty()) {
                    readSleepStages(startTimeMillis, endTimeMillis) { stages ->
                        val records = sleepSessions.map { session ->
                            val sessionStages = stages.filter { stage ->
                                stage.parentSleepId == session.uuid
                            }.map { stageData ->
                                SleepStageInfo(
                                    stageName = mapSleepStage(stageData.stage),
                                    timestampStart = stageData.timestampStart,
                                    timestampEnd = stageData.timestampEnd,
                                    durationMs = stageData.durationMs
                                )
                            }

                            SleepRecord(
                                sourceRecordId = session.uuid,
                                sourceDevice = session.deviceUuid,
                                timestampStart = session.timestampStart,
                                timestampEnd = session.timestampEnd,
                                durationMs = session.durationMs,
                                stages = sessionStages
                            )
                        }
                        callback(records)
                    }
                } else {
                    callback(emptyList())
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to read sleep", e)
            callback(emptyList())
        }
    }

    /**
     * Read SleepStage records within a time range.
     */
    private fun readSleepStages(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (List<SleepStageData>) -> Unit
    ) {
        try {
            val resolver = HealthDataResolver(healthDataStore, null)

            val filter = HealthDataResolver.Filter.and(
                HealthDataResolver.Filter.greaterThanEquals(
                    HealthConstants.SessionMeasurement.START_TIME, startTimeMillis
                ),
                HealthDataResolver.Filter.lessThan(
                    HealthConstants.SessionMeasurement.END_TIME, endTimeMillis
                )
            )

            val request = HealthDataResolver.ReadRequest.Builder()
                .setDataType(HealthConstants.SleepStage.HEALTH_DATA_TYPE)
                .setProperties(arrayOf(
                    HealthConstants.Common.UUID,
                    HealthConstants.SleepStage.SLEEP_ID,
                    HealthConstants.SleepStage.STAGE,
                    HealthConstants.SessionMeasurement.START_TIME,
                    HealthConstants.SessionMeasurement.END_TIME,
                    HealthConstants.SessionMeasurement.TIME_OFFSET
                ))
                .setFilter(filter)
                .build()

            resolver.read(request).setResultListener { result ->
                val stages = mutableListOf<SleepStageData>()

                try {
                    val iterator = result.iterator()
                    while (iterator.hasNext()) {
                        val data = iterator.next()
                        try {
                            val stage = parseSleepStage(data)
                            if (stage != null) {
                                stages.add(stage)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Skipping malformed sleep stage", e)
                        }
                    }
                    Log.d(TAG, "Retrieved ${stages.size} sleep stages")
                } catch (e: Exception) {
                    Log.e(TAG, "Error iterating sleep stages", e)
                } finally {
                    result.close()
                }

                callback(stages)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to read sleep stages", e)
            callback(emptyList())
        }
    }

    /**
     * Parse a Samsung Sleep record into internal session data.
     */
    private fun parseSleepSession(
        data: HealthDataResolver.ReadResult.DataIterator.HealthData
    ): SleepSessionData? {
        val uuid = SamsungDataMapper.extractRecordId(data, HealthConstants.Sleep.HEALTH_DATA_TYPE)
        val startTime = SamsungDataMapper.extractStartTime(data) ?: return null
        val endTime = SamsungDataMapper.extractEndTime(data) ?: return null

        val startMs = SamsungDataMapper.safeGetLong(data, HealthConstants.SessionMeasurement.START_TIME) ?: return null
        val endMs = SamsungDataMapper.safeGetLong(data, HealthConstants.SessionMeasurement.END_TIME) ?: return null

        return SleepSessionData(
            uuid = uuid,
            deviceUuid = SamsungDataMapper.extractDevice(data),
            timestampStart = startTime,
            timestampEnd = endTime,
            durationMs = endMs - startMs
        )
    }

    /**
     * Parse a Samsung SleepStage record.
     */
    private fun parseSleepStage(
        data: HealthDataResolver.ReadResult.DataIterator.HealthData
    ): SleepStageData? {
        val startTime = SamsungDataMapper.extractStartTime(data) ?: return null
        val endTime = SamsungDataMapper.extractEndTime(data) ?: return null

        val startMs = SamsungDataMapper.safeGetLong(data, HealthConstants.SessionMeasurement.START_TIME) ?: return null
        val endMs = SamsungDataMapper.safeGetLong(data, HealthConstants.SessionMeasurement.END_TIME) ?: return null
        val stage = SamsungDataMapper.safeGetInt(data, HealthConstants.SleepStage.STAGE) ?: return null
        val sleepId = SamsungDataMapper.safeGetString(data, HealthConstants.SleepStage.SLEEP_ID)

        return SleepStageData(
            parentSleepId = sleepId,
            stage = stage,
            timestampStart = startTime,
            timestampEnd = endTime,
            durationMs = endMs - startMs
        )
    }

    /**
     * Map Samsung sleep stage integer to human-readable name.
     */
    private fun mapSleepStage(stageCode: Int): String {
        return when (stageCode) {
            HealthConstants.SleepStage.STAGE_AWAKE -> SleepStageInfo.STAGE_AWAKE
            HealthConstants.SleepStage.STAGE_LIGHT -> SleepStageInfo.STAGE_LIGHT
            HealthConstants.SleepStage.STAGE_DEEP -> SleepStageInfo.STAGE_DEEP
            // REM stage constant — Samsung uses value 40004 in some SDK versions
            40004 -> SleepStageInfo.STAGE_REM
            else -> "${SleepStageInfo.STAGE_UNKNOWN}_$stageCode"
        }
    }

    /** Internal data class for parsed sleep sessions */
    private data class SleepSessionData(
        val uuid: String,
        val deviceUuid: String?,
        val timestampStart: String,
        val timestampEnd: String,
        val durationMs: Long
    )

    /** Internal data class for parsed sleep stages */
    private data class SleepStageData(
        val parentSleepId: String?,
        val stage: Int,
        val timestampStart: String,
        val timestampEnd: String,
        val durationMs: Long
    )
}

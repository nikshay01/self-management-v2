package com.selfmanagement.samsungbridge.samsung

import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.data.entries.ExerciseSession
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.selfmanagement.samsungbridge.models.ExerciseRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Reads exercise records and every nested session exposed by Samsung Health Data SDK 1.1.0. */
class ExerciseReader(
    private val healthDataStore: HealthDataStore
) {
    companion object {
        private const val TAG = "ExerciseReader"
        private const val PAGE_SIZE = 200
        private const val MAX_PAGES = 50
    }

    fun readExercise(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (List<ExerciseRecord>) -> Unit
    ) {
        readExercise(SyncTimeRange.fromEpochMillis(startTimeMillis, endTimeMillis), callback)
    }

    fun readExercise(
        range: SyncTimeRange,
        callback: (List<ExerciseRecord>) -> Unit
    ) {
        Log.d(TAG, "Reading exercise for range $range")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val filter = LocalTimeFilter.of(
                    range.startDateTime,
                    range.endDateTime,
                    true,
                    true
                )
                val records = mutableListOf<ExerciseRecord>()
                var pageToken: String? = null
                var pages = 0

                do {
                    val builder = DataTypes.EXERCISE.readDataRequestBuilder
                        .setLocalTimeFilter(filter)
                        .setPageSize(PAGE_SIZE)
                    if (!pageToken.isNullOrBlank()) builder.setPageToken(pageToken)

                    val response = healthDataStore.readData(builder.build())
                    for (point in response.dataList) {
                        val uid = SamsungDataMapper.extractRecordId(point, "exercise")
                        val device = SamsungDataMapper.extractDevice(point.dataSource)
                        val pointStart = SamsungDataMapper.formatInstant(point.startTime, point.zoneOffset)
                        val pointEnd = point.endTime?.let { SamsungDataMapper.formatInstant(it, point.zoneOffset) }
                        val pointType = point.getValue(DataType.ExerciseType.EXERCISE_TYPE)
                        val pointTitle = point.getValue(DataType.ExerciseType.CUSTOM_TITLE)
                        val sessions: List<ExerciseSession>? = point.getValue(DataType.ExerciseType.SESSIONS)

                        if (sessions.isNullOrEmpty()) {
                            val typeName = pointType?.name?.toDisplayName() ?: pointTitle ?: "Exercise"
                            records.add(
                                ExerciseRecord(
                                    sourceRecordId = uid,
                                    sourceDevice = device,
                                    timestampStart = pointStart,
                                    timestampEnd = pointEnd,
                                    exerciseType = pointType?.ordinal,
                                    exerciseTypeName = typeName,
                                    customType = pointTitle,
                                    durationMs = point.endTime?.toEpochMilli()?.minus(point.startTime.toEpochMilli()),
                                    distanceM = null,
                                    caloriesKcal = null,
                                    altitudeGainM = null
                                )
                            )
                        } else {
                            sessions.forEachIndexed { index, session ->
                                val start = SamsungDataMapper.formatInstant(session.startTime, point.zoneOffset)
                                val end = SamsungDataMapper.formatInstant(session.endTime, point.zoneOffset)
                                val sessionType = session.exerciseType
                                val typeName = session.customTitle ?: sessionType.name.toDisplayName()
                                val recordId = if (sessions.size == 1) uid else "${uid}_session_${index + 1}"

                                records.add(
                                    ExerciseRecord(
                                        sourceRecordId = recordId,
                                        sourceDevice = device,
                                        timestampStart = start,
                                        timestampEnd = end,
                                        exerciseType = sessionType.ordinal,
                                        exerciseTypeName = typeName,
                                        customType = session.customTitle ?: pointTitle,
                                        durationMs = session.duration.toMillis(),
                                        distanceM = session.distance?.toDouble(),
                                        caloriesKcal = session.calories.toDouble(),
                                        altitudeGainM = session.altitudeGain?.toDouble(),
                                        sessionDetails = mapSessionDetails(session, point.zoneOffset)
                                    )
                                )
                            }
                        }
                    }

                    pageToken = response.pageToken
                    pages++
                } while (!pageToken.isNullOrBlank() && pages < MAX_PAGES)

                if (!pageToken.isNullOrBlank()) {
                    Log.w(TAG, "Stopped after $MAX_PAGES pages; exercise sessions may be incomplete")
                }
                val sampleCount = records.sumOf { (it.sessionDetails["samples"] as? List<*>)?.size ?: 0 }
                val routePointCount = records.sumOf { (it.sessionDetails["route"] as? List<*>)?.size ?: 0 }
                val swimSessionCount = records.count { "swimming" in it.sessionDetails }
                Log.d(TAG, "Retrieved ${records.size} exercise sessions")
                Log.i(TAG, "Exercise details: $sampleCount samples, $routePointCount route points, $swimSessionCount swim logs")
                callback(records)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read exercise for range $range", e)
                callback(emptyList())
            }
        }
    }

    private fun mapSessionDetails(
        session: ExerciseSession,
        zoneOffset: java.time.ZoneOffset?
    ): Map<String, Any?> {
        val details = linkedMapOf<String, Any?>()
        details["start_time"] = SamsungDataMapper.formatInstant(session.startTime, zoneOffset)
        details["end_time"] = SamsungDataMapper.formatInstant(session.endTime, zoneOffset)
        details["duration_ms"] = session.duration.toMillis()
        details["exercise_type_code"] = session.exerciseType.ordinal
        details["exercise_type_name"] = session.exerciseType.name
        session.customTitle?.let { details["custom_title"] = it }
        details["calories_kcal"] = session.calories.toDouble()
        session.distance?.let { details["distance_m"] = it.toDouble() }
        session.altitudeGain?.let { details["altitude_gain_m"] = it.toDouble() }
        session.altitudeLoss?.let { details["altitude_loss_m"] = it.toDouble() }
        session.count?.let { details["count"] = it }
        details["count_type"] = session.countType.name
        session.maxSpeed?.let { details["max_speed_mps"] = it.toDouble() }
        session.meanSpeed?.let { details["mean_speed_mps"] = it.toDouble() }
        session.maxCalorieBurnRate?.let { details["max_calorie_burn_rate"] = it.toDouble() }
        session.meanCalorieBurnRate?.let { details["mean_calorie_burn_rate"] = it.toDouble() }
        session.maxCadence?.let { details["max_cadence"] = it.toDouble() }
        session.meanCadence?.let { details["mean_cadence"] = it.toDouble() }
        session.maxHeartRate?.let { details["max_heart_rate_bpm"] = it.toDouble() }
        session.meanHeartRate?.let { details["mean_heart_rate_bpm"] = it.toDouble() }
        session.minHeartRate?.let { details["min_heart_rate_bpm"] = it.toDouble() }
        session.maxAltitude?.let { details["max_altitude_m"] = it.toDouble() }
        session.minAltitude?.let { details["min_altitude_m"] = it.toDouble() }
        session.inclineDistance?.let { details["incline_distance_m"] = it.toDouble() }
        session.declineDistance?.let { details["decline_distance_m"] = it.toDouble() }
        session.maxPower?.let { details["max_power_watts"] = it.toDouble() }
        session.meanPower?.let { details["mean_power_watts"] = it.toDouble() }
        session.maxRpm?.let { details["max_rpm"] = it.toDouble() }
        session.meanRpm?.let { details["mean_rpm"] = it.toDouble() }
        session.comment?.let { details["comment"] = it }
        session.vo2Max?.let { details["vo2_max"] = it.toDouble() }
        session.autoDetected?.let { details["auto_detected"] = it }

        runCatching {
            session.log.orEmpty().map { sample ->
                linkedMapOf<String, Any?>(
                    "timestamp" to SamsungDataMapper.formatInstant(sample.timestamp, zoneOffset)
                ).apply {
                    sample.heartRate?.let { put("heart_rate_bpm", it.toDouble()) }
                    sample.cadence?.let { put("cadence", it.toDouble()) }
                    sample.count?.let { put("count", it) }
                    sample.power?.let { put("power_watts", it.toDouble()) }
                    sample.speed?.let { put("speed_mps", it.toDouble()) }
                }
            }
        }.onSuccess { samples -> if (samples.isNotEmpty()) details["samples"] = samples }
            .onFailure { Log.w(TAG, "Exercise samples unavailable for a session", it) }

        runCatching {
            session.route.orEmpty().map { location ->
                linkedMapOf<String, Any?>(
                    "timestamp" to SamsungDataMapper.formatInstant(location.timestamp, zoneOffset),
                    "latitude" to location.latitude.toDouble(),
                    "longitude" to location.longitude.toDouble()
                ).apply {
                    location.altitude?.let { put("altitude_m", it.toDouble()) }
                    location.accuracy?.let { put("accuracy_m", it.toDouble()) }
                }
            }
        }.onSuccess { route -> if (route.isNotEmpty()) details["route"] = route }
            .onFailure { Log.w(TAG, "Exercise route unavailable; check Samsung Health exercise location access", it) }

        runCatching { session.swimmingLog }.getOrNull()?.let { swim ->
            details["swimming"] = linkedMapOf<String, Any?>(
                "pool_length" to swim.poolLength,
                "pool_length_unit" to swim.poolLengthUnit
            ).apply {
                swim.totalDistance?.let { put("total_distance_m", it.toDouble()) }
                swim.totalDuration?.toMillis()?.let { put("total_duration_ms", it) }
                put(
                    "intervals",
                    swim.swimmingIntervals.orEmpty().map { interval ->
                        mapOf(
                            "duration_ms" to interval.duration.toMillis(),
                            "stroke_count" to interval.strokeCount,
                            "stroke_type" to interval.strokeType.name,
                            "interval" to interval.interval
                        )
                    }
                )
            }
        }

        return details
    }

    private fun String.toDisplayName(): String =
        lowercase().replace('_', ' ').replaceFirstChar { it.titlecase() }
}

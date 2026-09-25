package com.selfmanagement.samsungbridge.samsung

import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.data.entries.ExerciseSession
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.InstantTimeFilter
import com.selfmanagement.samsungbridge.models.ExerciseRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Reads exercise data from Samsung Health using Samsung Health Data SDK v1.1.0.
 */
class ExerciseReader(
    private val healthDataStore: HealthDataStore
) {
    companion object {
        private const val TAG = "ExerciseReader"
    }

    /**
     * Read exercise records within the specified time range.
     */
    fun readExercise(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (List<ExerciseRecord>) -> Unit
    ) {
        val startInstant = Instant.ofEpochMilli(startTimeMillis)
        val endInstant = Instant.ofEpochMilli(endTimeMillis)

        Log.d(TAG, "Reading exercise from $startInstant to $endInstant")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = DataTypes.EXERCISE.readDataRequestBuilder
                    .setInstantTimeFilter(InstantTimeFilter.of(startInstant, endInstant))
                    .build()

                val response = healthDataStore.readData(request)
                val records = mutableListOf<ExerciseRecord>()

                for (point in response.dataList) {
                    val uid = SamsungDataMapper.extractRecordId(point, "exercise")
                    val device = SamsungDataMapper.extractDevice(point.dataSource)
                    val pointStart = SamsungDataMapper.formatInstant(point.startTime, point.zoneOffset)
                    val pointEnd = point.endTime?.let { SamsungDataMapper.formatInstant(it, point.zoneOffset) }
                    val predefinedType = point.getValue(DataType.ExerciseType.EXERCISE_TYPE)
                    val customTitle = point.getValue(DataType.ExerciseType.CUSTOM_TITLE)
                    val sessions: List<ExerciseSession>? = point.getValue(DataType.ExerciseType.SESSIONS)

                    var totalDurationMs = point.endTime?.let { it.toEpochMilli() - point.startTime.toEpochMilli() }
                    var totalCalories: Double? = null
                    var totalDistance: Double? = null
                    var altitudeGain: Double? = null

                    sessions?.firstOrNull()?.let { firstSession ->
                        totalDurationMs = firstSession.duration?.toMillis() ?: totalDurationMs
                        totalCalories = firstSession.calories.toDouble()
                        totalDistance = firstSession.distance?.toDouble()
                        altitudeGain = firstSession.altitudeGain?.toDouble()
                    }

                    val typeName = predefinedType?.name?.lowercase()?.replace("_", " ")?.capitalize()
                        ?: customTitle
                        ?: "Exercise"

                    records.add(
                        ExerciseRecord(
                            sourceRecordId = uid,
                            sourceDevice = device,
                            timestampStart = pointStart,
                            timestampEnd = pointEnd,
                            exerciseType = predefinedType?.ordinal,
                            exerciseTypeName = typeName,
                            customType = customTitle,
                            durationMs = totalDurationMs,
                            distanceM = totalDistance,
                            caloriesKcal = totalCalories,
                            altitudeGainM = altitudeGain
                        )
                    )
                }

                Log.d(TAG, "Retrieved ${records.size} exercise records")
                callback(records)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read exercise", e)
                callback(emptyList())
            }
        }
    }
}

package com.selfmanagement.samsungbridge.samsung

import android.util.Log
import com.samsung.android.sdk.healthdata.HealthDataStore
import com.selfmanagement.samsungbridge.models.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

/**
 * Orchestrates reading from all Samsung Health data type readers.
 *
 * Provides a unified interface for reading steps, heart rate, sleep,
 * and exercise data. Each type delegates to its specific reader.
 *
 * Results are returned as our canonical model types — no Samsung
 * SDK objects leak out of this layer.
 */
class SamsungHealthReader(
    private val healthDataStore: HealthDataStore
) {
    companion object {
        private const val TAG = "SamsungHealthReader"
    }

    private val stepReader = StepReader(healthDataStore)
    private val heartRateReader = HeartRateReader(healthDataStore)
    private val sleepReader = SleepReader(healthDataStore)
    private val exerciseReader = ExerciseReader(healthDataStore)

    /**
     * Result of reading all data types.
     */
    data class ReadAllResult(
        val steps: List<StepsRecord> = emptyList(),
        val heartRate: List<HeartRateRecord> = emptyList(),
        val sleep: List<SleepRecord> = emptyList(),
        val exercise: List<ExerciseRecord> = emptyList()
    ) {
        val totalRecords: Int
            get() = steps.size + heartRate.size + sleep.size + exercise.size

        /**
         * Convert all records to canonical [HealthRecord] format.
         */
        fun toHealthRecords(): List<HealthRecord> {
            val records = mutableListOf<HealthRecord>()
            records.addAll(steps.map { it.toHealthRecord() })
            records.addAll(heartRate.map { it.toHealthRecord() })
            records.addAll(sleep.map { it.toHealthRecord() })
            records.addAll(exercise.map { it.toHealthRecord() })
            return records
        }

        /**
         * Generate a human-readable summary for the testing UI.
         */
        fun toDisplaySummary(): String {
            val sb = StringBuilder()

            // Steps summary
            if (steps.isNotEmpty()) {
                val totalSteps = steps.sumOf { it.count }
                sb.appendLine("Steps: $totalSteps")
                sb.appendLine("  Records: ${steps.size}")
                sb.appendLine()
            }

            // Heart rate summary
            if (heartRate.isNotEmpty()) {
                val bpmValues = heartRate.map { it.heartRate }
                sb.appendLine("Heart Rate:")
                sb.appendLine("  Samples: ${heartRate.size}")
                sb.appendLine("  Average: ${bpmValues.average().toInt()} bpm")
                sb.appendLine("  Minimum: ${bpmValues.minOrNull()?.toInt()} bpm")
                sb.appendLine("  Maximum: ${bpmValues.maxOrNull()?.toInt()} bpm")
                sb.appendLine()
            }

            // Sleep summary
            if (sleep.isNotEmpty()) {
                sleep.forEach { session ->
                    val durationHours = session.durationMs / 3600000
                    val durationMinutes = (session.durationMs % 3600000) / 60000
                    sb.appendLine("Sleep:")
                    sb.appendLine("  Start: ${session.timestampStart}")
                    sb.appendLine("  End: ${session.timestampEnd}")
                    sb.appendLine("  Duration: ${durationHours}h ${durationMinutes}m")
                    if (session.stages.isNotEmpty()) {
                        sb.appendLine("  Stages: ${session.stages.size}")
                        session.stages.groupBy { it.stageName }.forEach { (name, stages) ->
                            val totalMs = stages.sumOf { it.durationMs }
                            val mins = totalMs / 60000
                            sb.appendLine("    $name: ${mins}m")
                        }
                    }
                    sb.appendLine()
                }
            }

            // Exercise summary
            if (exercise.isNotEmpty()) {
                exercise.forEach { ex ->
                    val typeName = ex.exerciseTypeName ?: ex.customType ?: "Unknown"
                    sb.appendLine("Exercise: $typeName")
                    ex.durationMs?.let {
                        val mins = it / 60000
                        sb.appendLine("  Duration: ${mins}m")
                    }
                    ex.distanceM?.let {
                        sb.appendLine("  Distance: ${"%.2f".format(it / 1000.0)} km")
                    }
                    ex.caloriesKcal?.let {
                        sb.appendLine("  Calories: ${it.toInt()} kcal")
                    }
                    sb.appendLine()
                }
            }

            if (sb.isEmpty()) {
                sb.append("No data available for the selected time range.")
            }

            return sb.toString().trimEnd()
        }
    }

    /**
     * Read steps within the specified time range.
     */
    fun readSteps(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (List<StepsRecord>) -> Unit
    ) {
        stepReader.readSteps(startTimeMillis, endTimeMillis, callback)
    }

    /**
     * Read total steps within the specified time range (aggregate).
     */
    fun readTotalSteps(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (Int?) -> Unit
    ) {
        stepReader.readTotalSteps(startTimeMillis, endTimeMillis, callback)
    }

    /**
     * Read heart rate samples within the specified time range.
     */
    fun readHeartRate(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (List<HeartRateRecord>) -> Unit
    ) {
        heartRateReader.readHeartRate(startTimeMillis, endTimeMillis, callback)
    }

    /**
     * Read sleep sessions within the specified time range.
     */
    fun readSleep(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (List<SleepRecord>) -> Unit
    ) {
        sleepReader.readSleep(startTimeMillis, endTimeMillis, callback)
    }

    /**
     * Read exercise sessions within the specified time range.
     */
    fun readExercise(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (List<ExerciseRecord>) -> Unit
    ) {
        exerciseReader.readExercise(startTimeMillis, endTimeMillis, callback)
    }

    /**
     * Read ALL data types within the specified time range.
     *
     * Results are collected from all four readers and combined
     * into a single [ReadAllResult].
     *
     * @param startTimeMillis Start of the query range (UTC milliseconds)
     * @param endTimeMillis End of the query range (UTC milliseconds)
     * @param callback Called with the combined results
     */
    fun readAll(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (ReadAllResult) -> Unit
    ) {
        Log.d(TAG, "Reading all data types")

        val stepsRef = AtomicReference<List<StepsRecord>>(emptyList())
        val hrRef = AtomicReference<List<HeartRateRecord>>(emptyList())
        val sleepRef = AtomicReference<List<SleepRecord>>(emptyList())
        val exerciseRef = AtomicReference<List<ExerciseRecord>>(emptyList())

        // Track completion of all four reads
        val remaining = java.util.concurrent.atomic.AtomicInteger(4)

        val checkDone = {
            if (remaining.decrementAndGet() == 0) {
                val result = ReadAllResult(
                    steps = stepsRef.get(),
                    heartRate = hrRef.get(),
                    sleep = sleepRef.get(),
                    exercise = exerciseRef.get()
                )
                Log.d(TAG, "All reads complete — total records: ${result.totalRecords}")
                callback(result)
            }
        }

        readSteps(startTimeMillis, endTimeMillis) { records ->
            stepsRef.set(records)
            checkDone()
        }

        readHeartRate(startTimeMillis, endTimeMillis) { records ->
            hrRef.set(records)
            checkDone()
        }

        readSleep(startTimeMillis, endTimeMillis) { records ->
            sleepRef.set(records)
            checkDone()
        }

        readExercise(startTimeMillis, endTimeMillis) { records ->
            exerciseRef.set(records)
            checkDone()
        }
    }
}

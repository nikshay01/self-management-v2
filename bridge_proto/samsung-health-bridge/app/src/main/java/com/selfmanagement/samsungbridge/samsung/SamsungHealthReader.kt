package com.selfmanagement.samsungbridge.samsung

import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataStore
import com.selfmanagement.samsungbridge.models.*
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
    private val healthDataStore: HealthDataStore,
    private val screenTimeReader: ScreenTimeReader
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
        val exercise: List<ExerciseRecord> = emptyList(),
        val screenTime: ScreenTimeRecord = ScreenTimeRecord()
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

            // Steps summary (daily totals when more than one day)
            if (steps.isNotEmpty()) {
                val totalSteps = steps.sumOf { it.count }
                sb.appendLine("Steps: $totalSteps")
                sb.appendLine("  Daily records: ${steps.size}")
                if (steps.size > 1) {
                    steps.forEach { day ->
                        val dayLabel = day.timestampStart.take(10)
                        sb.appendLine("  $dayLabel → ${day.count}")
                    }
                }
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
                sb.appendLine("Sleep sessions: ${sleep.size}")
                val totalStages = sleep.sumOf { it.stages.size }
                if (totalStages > 0) {
                    sb.appendLine("  Stage records: $totalStages")
                }
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
                sb.appendLine("Exercise sessions: ${exercise.size}")
                exercise.forEach { ex ->
                    val typeName = ex.exerciseTypeName ?: ex.customType ?: "Unknown"
                    sb.appendLine("Exercise: $typeName")
                    sb.appendLine("  Start: ${ex.timestampStart}")
                    ex.timestampEnd?.let { sb.appendLine("  End: $it") }
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
                    val samples = ex.sessionDetails["samples"] as? List<*>
                    if (!samples.isNullOrEmpty()) sb.appendLine("  Data samples: ${samples.size}")
                    val route = ex.sessionDetails["route"] as? List<*>
                    if (!route.isNullOrEmpty()) sb.appendLine("  Route points: ${route.size}")
                    ex.sessionDetails.entries
                        .filter { it.key !in setOf("start_time", "end_time", "samples", "route", "swimming") }
                        .forEach { (key, value) -> sb.appendLine("  $key: $value") }
                    (ex.sessionDetails["swimming"] as? Map<*, *>)?.let { sb.appendLine("  Swimming: $it") }
                    sb.appendLine()
                }
            }

            // Device and app screen-time summary
            sb.appendLine("Screen time:")
            if (!screenTime.accessGranted) {
                sb.appendLine("  Usage Access is not granted. Use Grant Screen Time Access to enable it.")
            } else {
                sb.appendLine("  Device interactive: ${formatDuration(screenTime.totalScreenInteractiveMs)}")
                sb.appendLine("  Combined app foreground: ${formatDuration(screenTime.totalAppForegroundMs)}")
                sb.appendLine("  Combined app visible: ${formatDuration(screenTime.totalAppVisibleMs)}")
                sb.appendLine("  Combined foreground service: ${formatDuration(screenTime.totalAppForegroundServiceMs)}")
                sb.appendLine("  Apps with usage: ${screenTime.apps.size}")
                screenTime.error?.let { sb.appendLine("  Read warning: $it") }
                if (screenTime.dailyScreenInteractive.isNotEmpty()) {
                    sb.appendLine("  Device interactive by day:")
                    screenTime.dailyScreenInteractive.forEach { day ->
                        sb.appendLine("    ${day.localDate}: ${formatDuration(day.interactiveMs)}")
                    }
                }
                if (screenTime.apps.isNotEmpty()) {
                    sb.appendLine("  App usage:")
                    screenTime.apps.forEach { app ->
                        sb.appendLine("    ${app.appName} (${app.packageName})")
                        sb.appendLine("      Foreground: ${formatDuration(app.totalForegroundMs)}")
                        sb.appendLine("      Visible: ${formatDuration(app.totalVisibleMs)}")
                        if (app.totalForegroundServiceMs > 0L) {
                            sb.appendLine("      Foreground service: ${formatDuration(app.totalForegroundServiceMs)}")
                        }
                        app.lastUsedAt?.let { sb.appendLine("      Last used: $it") }
                        app.lastVisibleAt?.let { sb.appendLine("      Last visible: $it") }
                        app.lastForegroundServiceUsedAt?.let { sb.appendLine("      Last foreground service: $it") }
                        app.dailyUsage.forEach { day ->
                            sb.appendLine(
                                "      ${day.localDate}: foreground ${formatDuration(day.foregroundMs)}, " +
                                    "visible ${formatDuration(day.visibleMs)}, " +
                                    "foreground service ${formatDuration(day.foregroundServiceMs)}"
                            )
                            day.lastUsedAt?.let { sb.appendLine("        Last used: $it") }
                            day.lastVisibleAt?.let { sb.appendLine("        Last visible: $it") }
                            day.lastForegroundServiceUsedAt?.let { sb.appendLine("        Last foreground service: $it") }
                        }
                    }
                }
            }

            if (steps.isEmpty() && heartRate.isEmpty() && sleep.isEmpty() && exercise.isEmpty() &&
                screenTime.accessGranted && screenTime.totalScreenInteractiveMs == 0L && screenTime.apps.isEmpty()
            ) {
                sb.insert(0, "No Samsung Health or screen-time data available for the selected time range.\n\n")
            }

            return sb.toString().trimEnd()
        }

        private fun formatDuration(durationMs: Long): String {
            val totalMinutes = durationMs / 60000L
            val hours = totalMinutes / 60L
            val minutes = totalMinutes % 60L
            return if (hours > 0L) "${hours}h ${minutes}m" else "${minutes}m"
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
        readSteps(SyncTimeRange.fromEpochMillis(startTimeMillis, endTimeMillis), callback)
    }

    fun readSteps(
        range: SyncTimeRange,
        callback: (List<StepsRecord>) -> Unit
    ) {
        stepReader.readSteps(range, callback)
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
        readHeartRate(SyncTimeRange.fromEpochMillis(startTimeMillis, endTimeMillis), callback)
    }

    fun readHeartRate(
        range: SyncTimeRange,
        callback: (List<HeartRateRecord>) -> Unit
    ) {
        heartRateReader.readHeartRate(range, callback)
    }

    /**
     * Read sleep sessions within the specified time range.
     */
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
        sleepReader.readSleep(range, callback)
    }

    /**
     * Read exercise sessions within the specified time range.
     */
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
        exerciseReader.readExercise(range, callback)
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
        readAll(SyncTimeRange.fromEpochMillis(startTimeMillis, endTimeMillis), callback)
    }

    /**
     * Read ALL data types within [range].
     */
    fun readAll(
        range: SyncTimeRange,
        callback: (ReadAllResult) -> Unit
    ) {
        Log.i(TAG, "Requested range: start=${range.startDateTime} end=${range.endDateTime} zone=${range.zoneId}")

        val stepsRef = AtomicReference<List<StepsRecord>>(emptyList())
        val hrRef = AtomicReference<List<HeartRateRecord>>(emptyList())
        val sleepRef = AtomicReference<List<SleepRecord>>(emptyList())
        val exerciseRef = AtomicReference<List<ExerciseRecord>>(emptyList())
        val screenTimeRef = AtomicReference(ScreenTimeRecord())

        val remaining = java.util.concurrent.atomic.AtomicInteger(5)

        val checkDone = {
            if (remaining.decrementAndGet() == 0) {
                val result = ReadAllResult(
                    steps = stepsRef.get(),
                    heartRate = hrRef.get(),
                    sleep = sleepRef.get(),
                    exercise = exerciseRef.get(),
                    screenTime = screenTimeRef.get()
                )
                val stageCount = result.sleep.sumOf { it.stages.size }
                Log.i(TAG, "Steps records: ${result.steps.size}")
                Log.i(TAG, "Heart rate samples: ${result.heartRate.size}")
                Log.i(TAG, "Sleep sessions: ${result.sleep.size}")
                Log.i(TAG, "Sleep stages: $stageCount")
                Log.i(TAG, "Exercise sessions: ${result.exercise.size}")
                Log.i(TAG, "Total normalized records: ${result.totalRecords}")
                Log.i(TAG, "Screen-time apps: ${result.screenTime.apps.size}")
                Log.i(TAG, "Screen interactive milliseconds: ${result.screenTime.totalScreenInteractiveMs}")
                callback(result)
            }
        }

        readSteps(range) { records ->
            stepsRef.set(records)
            checkDone()
        }

        readHeartRate(range) { records ->
            hrRef.set(records)
            checkDone()
        }

        readSleep(range) { records ->
            sleepRef.set(records)
            checkDone()
        }

        readExercise(range) { records ->
            exerciseRef.set(records)
            checkDone()
        }

        screenTimeReader.read(range) { result ->
            screenTimeRef.set(result)
            checkDone()
        }
    }
}

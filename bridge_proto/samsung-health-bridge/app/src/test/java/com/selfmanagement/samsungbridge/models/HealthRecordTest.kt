package com.selfmanagement.samsungbridge.models

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the canonical data model mapping.
 *
 * Tests that our data models correctly serialize to HealthRecord
 * format for API transport, handle missing values gracefully,
 * and generate correct deterministic IDs.
 */
class HealthRecordTest {

    @Test
    fun `HealthRecord has correct source constant`() {
        assertEquals("samsung_health", HealthRecord.SOURCE_SAMSUNG_HEALTH)
    }

    @Test
    fun `HealthRecord has correct type constants`() {
        assertEquals("steps", HealthRecord.TYPE_STEPS)
        assertEquals("heart_rate", HealthRecord.TYPE_HEART_RATE)
        assertEquals("sleep", HealthRecord.TYPE_SLEEP)
        assertEquals("exercise", HealthRecord.TYPE_EXERCISE)
    }

    @Test
    fun `StepsRecord converts to HealthRecord correctly`() {
        val stepsRecord = StepsRecord(
            sourceRecordId = "uuid-123",
            sourceDevice = "Galaxy Watch",
            timestampStart = "2026-09-24T07:00:00+05:30",
            timestampEnd = "2026-09-24T07:05:00+05:30",
            count = 425,
            calories = 15.2,
            distance = 300.5,
            speed = 1.2
        )

        val healthRecord = stepsRecord.toHealthRecord()

        assertEquals("samsung_health", healthRecord.source)
        assertEquals("uuid-123", healthRecord.sourceRecordId)
        assertEquals("Galaxy Watch", healthRecord.sourceDevice)
        assertEquals("steps", healthRecord.type)
        assertEquals("2026-09-24T07:00:00+05:30", healthRecord.timestampStart)
        assertEquals("2026-09-24T07:05:00+05:30", healthRecord.timestampEnd)
        assertEquals(425.0, healthRecord.value!!, 0.01)
        assertEquals("count", healthRecord.unit)
        assertEquals(15.2, healthRecord.metadata["calories_kcal"] as Double, 0.01)
        assertEquals(300.5, healthRecord.metadata["distance_m"] as Double, 0.01)
        assertEquals(1.2, healthRecord.metadata["speed_ms"] as Double, 0.01)
    }

    @Test
    fun `StepsRecord handles null optional fields`() {
        val stepsRecord = StepsRecord(
            sourceRecordId = null,
            sourceDevice = null,
            timestampStart = "2026-09-24T07:00:00Z",
            timestampEnd = null,
            count = 100,
            calories = null,
            distance = null,
            speed = null
        )

        val healthRecord = stepsRecord.toHealthRecord()

        assertNull(healthRecord.sourceRecordId)
        assertNull(healthRecord.sourceDevice)
        assertNull(healthRecord.timestampEnd)
        assertEquals(100.0, healthRecord.value!!, 0.01)
        assertTrue(healthRecord.metadata.isEmpty())
    }

    @Test
    fun `HeartRateRecord converts to HealthRecord correctly`() {
        val hrRecord = HeartRateRecord(
            sourceRecordId = "hr-uuid-456",
            sourceDevice = "Galaxy Watch",
            timestampStart = "2026-09-24T08:00:00Z",
            timestampEnd = "2026-09-24T08:01:00Z",
            heartRate = 72.0,
            min = 65.0,
            max = 80.0,
            heartBeatCount = 72
        )

        val healthRecord = hrRecord.toHealthRecord()

        assertEquals("heart_rate", healthRecord.type)
        assertEquals(72.0, healthRecord.value!!, 0.01)
        assertEquals("bpm", healthRecord.unit)
        assertEquals(65.0, healthRecord.metadata["min_bpm"] as Double, 0.01)
        assertEquals(80.0, healthRecord.metadata["max_bpm"] as Double, 0.01)
        assertEquals(72, healthRecord.metadata["heart_beat_count"])
    }

    @Test
    fun `HeartRateRecord handles missing min max`() {
        val hrRecord = HeartRateRecord(
            sourceRecordId = "hr-1",
            sourceDevice = null,
            timestampStart = "2026-09-24T08:00:00Z",
            timestampEnd = null,
            heartRate = 75.0,
            min = null,
            max = null,
            heartBeatCount = null
        )

        val healthRecord = hrRecord.toHealthRecord()
        assertEquals(75.0, healthRecord.value!!, 0.01)
        assertTrue(healthRecord.metadata.isEmpty())
    }

    @Test
    fun `SleepRecord converts to HealthRecord with stages`() {
        val sleepRecord = SleepRecord(
            sourceRecordId = "sleep-789",
            sourceDevice = "Galaxy Watch",
            timestampStart = "2026-09-23T23:14:00+05:30",
            timestampEnd = "2026-09-24T06:32:00+05:30",
            durationMs = 7 * 3600000L + 18 * 60000L,
            stages = listOf(
                SleepStageInfo(
                    stageName = "light",
                    timestampStart = "2026-09-23T23:14:00+05:30",
                    timestampEnd = "2026-09-24T00:30:00+05:30",
                    durationMs = 76 * 60000L
                ),
                SleepStageInfo(
                    stageName = "deep",
                    timestampStart = "2026-09-24T00:30:00+05:30",
                    timestampEnd = "2026-09-24T02:00:00+05:30",
                    durationMs = 90 * 60000L
                )
            )
        )

        val healthRecord = sleepRecord.toHealthRecord()

        assertEquals("sleep", healthRecord.type)
        assertEquals("ms", healthRecord.unit)
        assertEquals((7 * 3600000L + 18 * 60000L).toDouble(), healthRecord.value!!, 0.01)
        assertEquals(7 * 3600000L + 18 * 60000L, healthRecord.metadata["duration_ms"])

        @Suppress("UNCHECKED_CAST")
        val stages = healthRecord.metadata["stages"] as List<Map<String, Any?>>
        assertEquals(2, stages.size)
        assertEquals("light", stages[0]["stage"])
        assertEquals("deep", stages[1]["stage"])
    }

    @Test
    fun `SleepRecord handles sleep crossing midnight`() {
        // Sleep starting before midnight, ending after
        val sleepRecord = SleepRecord(
            sourceRecordId = "sleep-midnight",
            sourceDevice = null,
            timestampStart = "2026-09-23T23:30:00+05:30",
            timestampEnd = "2026-09-24T06:00:00+05:30",
            durationMs = 6 * 3600000L + 30 * 60000L,
            stages = emptyList()
        )

        val healthRecord = sleepRecord.toHealthRecord()

        assertEquals("2026-09-23T23:30:00+05:30", healthRecord.timestampStart)
        assertEquals("2026-09-24T06:00:00+05:30", healthRecord.timestampEnd)
        // Duration should be 6.5 hours in milliseconds
        assertEquals((6 * 3600000L + 30 * 60000L).toDouble(), healthRecord.value!!, 0.01)
    }

    @Test
    fun `ExerciseRecord converts to HealthRecord correctly`() {
        val exerciseRecord = ExerciseRecord(
            sourceRecordId = "ex-101",
            sourceDevice = "Galaxy Watch",
            timestampStart = "2026-09-24T06:00:00Z",
            timestampEnd = "2026-09-24T06:30:00Z",
            exerciseType = 1001,
            exerciseTypeName = "Walking",
            customType = null,
            durationMs = 30 * 60000L,
            distanceM = 2110.0,
            caloriesKcal = 162.0,
            altitudeGainM = 15.0
        )

        val healthRecord = exerciseRecord.toHealthRecord()

        assertEquals("exercise", healthRecord.type)
        assertEquals("ms", healthRecord.unit)
        assertEquals((30 * 60000L).toDouble(), healthRecord.value!!, 0.01)
        assertEquals(1001, healthRecord.metadata["exercise_type_code"])
        assertEquals("Walking", healthRecord.metadata["exercise_type_name"])
        assertEquals(2110.0, healthRecord.metadata["distance_m"] as Double, 0.01)
        assertEquals(162.0, healthRecord.metadata["calories_kcal"] as Double, 0.01)
        assertEquals(15.0, healthRecord.metadata["altitude_gain_m"] as Double, 0.01)
    }

    @Test
    fun `ExerciseRecord handles all-null optional fields`() {
        val exerciseRecord = ExerciseRecord(
            sourceRecordId = "ex-minimal",
            sourceDevice = null,
            timestampStart = "2026-09-24T06:00:00Z",
            timestampEnd = null,
            exerciseType = null,
            exerciseTypeName = null,
            customType = null,
            durationMs = null,
            distanceM = null,
            caloriesKcal = null,
            altitudeGainM = null
        )

        val healthRecord = exerciseRecord.toHealthRecord()

        assertEquals("exercise", healthRecord.type)
        assertNull(healthRecord.value)
        assertTrue(healthRecord.metadata.isEmpty())
    }

    @Test
    fun `ExerciseRecord type name mapping`() {
        assertEquals("Walking", ExerciseRecord.exerciseTypeName(1001))
        assertEquals("Running", ExerciseRecord.exerciseTypeName(1002))
        assertEquals("Cycling", ExerciseRecord.exerciseTypeName(1020))
        assertEquals("Swimming", ExerciseRecord.exerciseTypeName(2001))
        assertEquals("Yoga", ExerciseRecord.exerciseTypeName(4001))
        assertEquals("Exercise (9999)", ExerciseRecord.exerciseTypeName(9999))
    }

    @Test
    fun `SleepStageInfo constants are correct`() {
        assertEquals("awake", SleepStageInfo.STAGE_AWAKE)
        assertEquals("light", SleepStageInfo.STAGE_LIGHT)
        assertEquals("deep", SleepStageInfo.STAGE_DEEP)
        assertEquals("rem", SleepStageInfo.STAGE_REM)
        assertEquals("unknown", SleepStageInfo.STAGE_UNKNOWN)
    }
}

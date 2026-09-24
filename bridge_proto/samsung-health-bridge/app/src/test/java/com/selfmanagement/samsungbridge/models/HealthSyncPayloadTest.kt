package com.selfmanagement.samsungbridge.models

import com.google.gson.GsonBuilder
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the HealthSyncPayload JSON serialization.
 *
 * Verifies that the payload structure matches the documented API contract.
 */
class HealthSyncPayloadTest {

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .create()

    @Test
    fun `payload serializes to correct JSON structure`() {
        val payload = HealthSyncPayload(
            source = "samsung_health",
            device = "Galaxy Watch",
            syncStartedAt = "2026-09-24T07:30:00Z",
            syncRange = SyncRange(
                start = "2026-09-23T00:00:00Z",
                end = "2026-09-24T07:30:00Z"
            ),
            records = listOf(
                HealthRecord(
                    source = "samsung_health",
                    sourceRecordId = "abc123",
                    sourceDevice = "Galaxy Watch",
                    type = "steps",
                    timestampStart = "2026-09-24T07:00:00Z",
                    timestampEnd = "2026-09-24T07:05:00Z",
                    value = 425.0,
                    unit = "count",
                    metadata = emptyMap()
                )
            )
        )

        val json = gson.toJson(payload)

        // Verify structure
        assertTrue(json.contains("\"source\": \"samsung_health\""))
        assertTrue(json.contains("\"device\": \"Galaxy Watch\""))
        assertTrue(json.contains("\"sync_started_at\""))
        assertTrue(json.contains("\"sync_range\""))
        assertTrue(json.contains("\"records\""))
        assertTrue(json.contains("\"source_record_id\": \"abc123\""))
        assertTrue(json.contains("\"timestamp_start\""))
        assertTrue(json.contains("\"timestamp_end\""))
    }

    @Test
    fun `payload with empty records produces valid JSON`() {
        val payload = HealthSyncPayload(
            source = "samsung_health",
            device = null,
            syncStartedAt = "2026-09-24T07:30:00Z",
            syncRange = SyncRange(
                start = "2026-09-24T00:00:00Z",
                end = "2026-09-24T07:30:00Z"
            ),
            records = emptyList()
        )

        val json = gson.toJson(payload)
        assertNotNull(json)
        assertTrue(json.contains("\"records\": []"))
    }

    @Test
    fun `payload with multiple record types serializes correctly`() {
        val records = listOf(
            StepsRecord(
                sourceRecordId = "step-1",
                sourceDevice = null,
                timestampStart = "2026-09-24T07:00:00Z",
                timestampEnd = "2026-09-24T07:05:00Z",
                count = 425,
                calories = 10.0,
                distance = 300.0,
                speed = null
            ).toHealthRecord(),
            HeartRateRecord(
                sourceRecordId = "hr-1",
                sourceDevice = null,
                timestampStart = "2026-09-24T07:00:00Z",
                timestampEnd = null,
                heartRate = 72.0,
                min = null,
                max = null,
                heartBeatCount = null
            ).toHealthRecord(),
            SleepRecord(
                sourceRecordId = "sleep-1",
                sourceDevice = null,
                timestampStart = "2026-09-23T23:00:00Z",
                timestampEnd = "2026-09-24T06:00:00Z",
                durationMs = 7 * 3600000L,
                stages = emptyList()
            ).toHealthRecord()
        )

        val payload = HealthSyncPayload(
            source = "samsung_health",
            device = null,
            syncStartedAt = "2026-09-24T08:00:00Z",
            syncRange = SyncRange(
                start = "2026-09-23T00:00:00Z",
                end = "2026-09-24T08:00:00Z"
            ),
            records = records
        )

        val json = gson.toJson(payload)

        // Should contain all three record types
        assertTrue(json.contains("\"type\": \"steps\""))
        assertTrue(json.contains("\"type\": \"heart_rate\""))
        assertTrue(json.contains("\"type\": \"sleep\""))
    }

    @Test
    fun `HealthRecord serialization uses snake_case field names`() {
        val record = HealthRecord(
            source = "samsung_health",
            sourceRecordId = "test-id",
            sourceDevice = "Test Device",
            type = "steps",
            timestampStart = "2026-09-24T07:00:00Z",
            timestampEnd = "2026-09-24T07:05:00Z",
            value = 100.0,
            unit = "count",
            metadata = mapOf("test_key" to "test_value")
        )

        val json = gson.toJson(record)

        // @SerializedName annotations should produce snake_case
        assertTrue(json.contains("\"source_record_id\""))
        assertTrue(json.contains("\"source_device\""))
        assertTrue(json.contains("\"timestamp_start\""))
        assertTrue(json.contains("\"timestamp_end\""))
    }

    @Test
    fun `duplicate records have same source_record_id`() {
        val record1 = StepsRecord(
            sourceRecordId = "unique-samsung-uuid",
            sourceDevice = null,
            timestampStart = "2026-09-24T07:00:00Z",
            timestampEnd = null,
            count = 100,
            calories = null,
            distance = null,
            speed = null
        ).toHealthRecord()

        val record2 = StepsRecord(
            sourceRecordId = "unique-samsung-uuid",
            sourceDevice = null,
            timestampStart = "2026-09-24T07:00:00Z",
            timestampEnd = null,
            count = 100,
            calories = null,
            distance = null,
            speed = null
        ).toHealthRecord()

        assertEquals(record1.sourceRecordId, record2.sourceRecordId)
        assertEquals("unique-samsung-uuid", record1.sourceRecordId)
    }
}

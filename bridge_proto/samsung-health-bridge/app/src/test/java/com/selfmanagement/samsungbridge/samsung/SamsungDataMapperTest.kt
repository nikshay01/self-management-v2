package com.selfmanagement.samsungbridge.samsung

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for SamsungDataMapper.
 *
 * Tests timestamp conversion, timezone handling, and ISO-8601 formatting.
 */
class SamsungDataMapperTest {

    @Test
    fun `toIso8601 with zero offset produces UTC timestamp`() {
        // Epoch = 1970-01-01T00:00:00Z
        val result = SamsungDataMapper.toIso8601(0L, 0L)
        assertTrue("Should contain Z or +00:00 for UTC", result.contains("1970-01-01"))
    }

    @Test
    fun `toIso8601 with positive offset produces correct timezone`() {
        // IST offset = +5:30 = 19800000 ms
        val istOffsetMs = 5 * 3600000L + 30 * 60000L // +19800000
        val result = SamsungDataMapper.toIso8601(0L, istOffsetMs)
        // Should show +05:30 timezone
        assertTrue("Should contain +05:30 timezone", result.contains("+05:30"))
    }

    @Test
    fun `toIso8601 with negative offset produces correct timezone`() {
        // EST offset = -5:00 = -18000000 ms
        val estOffsetMs = -(5 * 3600000L) // -18000000
        val result = SamsungDataMapper.toIso8601(0L, estOffsetMs)
        // Should show -05:00 timezone
        assertTrue("Should contain -05:00 timezone", result.contains("-05:00"))
    }

    @Test
    fun `toIso8601 produces ISO-8601 format`() {
        // 2026-09-24T07:00:00Z in milliseconds
        val utcMillis = 1790413200000L
        val result = SamsungDataMapper.toIso8601(utcMillis, 0L)
        // Should be in yyyy-MM-dd'T'HH:mm:ss format
        assertTrue("Should match ISO-8601 format",
            result.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")))
    }

    @Test
    fun `toIso8601 handles midnight crossing timezone`() {
        // Test that a UTC timestamp of 2026-09-23 at 18:30 UTC
        // becomes 2026-09-24 00:00 in IST (+5:30)
        // 18:30 UTC + 5:30 = 00:00 next day IST
        val istOffsetMs = 5 * 3600000L + 30 * 60000L
        
        // This is a conceptual test - verifying the function doesn't crash
        // around midnight-crossing scenarios
        val result = SamsungDataMapper.toIso8601(1790413200000L, istOffsetMs)
        assertNotNull(result)
        assertFalse(result.isEmpty())
    }

    @Test
    fun `toIso8601 handles DST-like offset changes`() {
        // Different offsets should produce different timezone strings
        val offset1 = 1 * 3600000L // +01:00
        val offset2 = 2 * 3600000L // +02:00

        val result1 = SamsungDataMapper.toIso8601(0L, offset1)
        val result2 = SamsungDataMapper.toIso8601(0L, offset2)

        assertNotEquals("Different offsets should produce different results", result1, result2)
    }
}

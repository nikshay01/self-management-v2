package com.selfmanagement.samsungbridge.samsung

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class SyncTimeRangeTest {

    @Test
    fun `inclusiveLocalDates uses local midnight through 23-59-59`() {
        val range = SyncTimeRange.inclusiveLocalDates(
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 9, 25),
            zoneId = ZoneOffset.ofHoursMinutes(5, 30)
        )
        assertEquals(LocalDate.of(2026, 9, 1).atStartOfDay(), range.startDateTime)
        assertEquals(23, range.endDateTime.hour)
        assertEquals(59, range.endDateTime.minute)
        assertEquals(59, range.endDateTime.second)
        assertEquals(LocalDate.of(2026, 9, 25), range.endDateTime.toLocalDate())
    }

    @Test
    fun `todayUntilNow starts at local midnight`() {
        val zone = ZoneOffset.ofHoursMinutes(5, 30)
        val range = SyncTimeRange.todayUntilNow(zoneId = zone)
        assertEquals(0, range.startDateTime.hour)
        assertEquals(0, range.startDateTime.minute)
        assertEquals(0, range.startDateTime.second)
        assertFalse(range.endDateTime.isBefore(range.startDateTime))
    }
}

package com.jobforandroid.shoplinoterelease

import com.jobforandroid.shoplinoterelease.utils.TimeManager
import org.junit.Assert.*
import org.junit.Test

class NoteTimestampTest {
    @Test fun legacyDatesCanSeedModificationOrderWithoutRewritingCreationTime() {
        val older = TimeManager.parseStoredTime("09:30:00 - 17.11.26")!!
        val newer = TimeManager.parseStoredTime("10:30:00 - 18.11.26")!!
        assertTrue(newer.time > older.time)
        assertEquals(86400000L + 3600000L, newer.time - older.time)
    }
    @Test fun malformedDatesAreRejectedInsteadOfInventingHistory() {
        assertNull(TimeManager.parseStoredTime(""))
        assertNull(TimeManager.parseStoredTime("unknown"))
        assertNull(TimeManager.parseStoredTime("09:30:00 - 31.02.26"))
        assertNull(TimeManager.parseStoredTime("09:30:00 - 17.11.26 trailing"))
    }
}

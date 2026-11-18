package com.jobforandroid.shoplinoterelease

import android.content.SharedPreferences
import com.jobforandroid.shoplinoterelease.utils.TimeManager
import java.lang.reflect.Proxy
import java.time.Instant
import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class TimeFormattingTest {
    private lateinit var previousLocale: Locale
    private lateinit var previousZone: TimeZone
    @Before fun deterministicClockFormatting() {
        previousLocale = Locale.getDefault()
        previousZone = TimeZone.getDefault()
        Locale.setDefault(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }
    @After fun restoreDefaults() { Locale.setDefault(previousLocale); TimeZone.setDefault(previousZone) }
    private fun preferences(pattern: String): SharedPreferences = Proxy.newProxyInstance(
        SharedPreferences::class.java.classLoader, arrayOf(SharedPreferences::class.java)
    ) { _, method, _ -> if (method.name == "getString") pattern else throw UnsupportedOperationException(method.name) } as SharedPreferences

    @Test fun afternoonEpochDoesNotPassThroughAmbiguousTwelveHourStorage() {
        val timestamp = Instant.parse("2026-11-18T22:06:09Z").toEpochMilli()
        assertEquals("22:06:09" to "18.11.2026", TimeManager.getTimeAndDate(timestamp, preferences("HH:mm:ss - dd.MM.yyyy")))
        assertEquals("10:06 PM" to "18.11.26", TimeManager.getTimeAndDate(timestamp, preferences("h:mm a")))
    }
    @Test fun dateFirstPreferencePreservesTheActualHour() {
        val timestamp = Instant.parse("2026-11-18T22:06:09Z").toEpochMilli()
        assertEquals("18.11.2026 - 22:06:09", TimeManager.getTimeFormat(timestamp, preferences("dd.MM.yyyy - HH:mm:ss")))
        assertEquals("22:06:09" to "18.11.2026", TimeManager.getTimeAndDate(timestamp, preferences("dd.MM.yyyy - HH:mm:ss")))
    }
    @Test fun newStorageRetainsTimeZoneAndPrecisionAndLegacyValuesStillParse() {
        val iso = "2026-11-18T22:06:09.123+05:00"
        assertEquals(Instant.parse("2026-11-18T17:06:09.123Z").toEpochMilli(), TimeManager.parseStoredTime(iso)!!.time)
        assertNotNull(TimeManager.parseStoredTime("09:30:00 - 17.11.26"))
        val before = System.currentTimeMillis()
        val stored = TimeManager.getCurrentTime()
        val parsed = TimeManager.parseStoredTime(stored)!!.time
        assertTrue(parsed in before..System.currentTimeMillis())
    }
    @Test fun legacyAndInvalidPreferencesResolveToActualAvailableChoices() {
        assertEquals(TimeManager.DEFAULT_DISPLAY_FORMAT, TimeManager.normalizedDisplayFormat(TimeManager.DEF_TIME_FORMAT))
        assertEquals(TimeManager.DEFAULT_DISPLAY_FORMAT, TimeManager.normalizedDisplayFormat("hh:mm:ss - dd.MM.yyyy"))
        assertEquals("dd.MM.yyyy - HH:mm:ss", TimeManager.normalizedDisplayFormat("dd.MM.yyyy - hh:mm:ss"))
        assertEquals(TimeManager.DEFAULT_DISPLAY_FORMAT, TimeManager.normalizedDisplayFormat("invalid-pattern"))
        assertEquals("h:mm a", TimeManager.normalizedDisplayFormat("h:mm a"))
    }
}

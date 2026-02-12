package com.arun.tankerapp

import com.arun.tankerapp.core.data.database.entity.Apartment
import com.arun.tankerapp.core.data.database.entity.TankerLog
import com.arun.tankerapp.core.data.database.entity.VacancyLog
import com.arun.tankerapp.core.data.database.TypeConverters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for Story 1.1: Project Initialization & Database Setup
 * Validates entity creation and TypeConverter correctness.
 */
class Test {

    // --- Apartment Entity Tests ---

    @Test
    fun apartment_creation_withDefaults() {
        val apartment = Apartment(number = "A-101")
        assertEquals(0L, apartment.id)
        assertEquals("A-101", apartment.number)
    }

    @Test
    fun apartment_creation_withExplicitId() {
        val apartment = Apartment(id = 5, number = "B-202")
        assertEquals(5L, apartment.id)
        assertEquals("B-202", apartment.number)
    }

    @Test
    fun apartment_equality() {
        val a1 = Apartment(id = 1, number = "A-101")
        val a2 = Apartment(id = 1, number = "A-101")
        assertEquals(a1, a2)
    }

    // --- TankerLog Entity Tests ---

    @Test
    fun tankerLog_creation() {
        val log = TankerLog(
            date = "2026-02-10",
            month = 2,
            year = 2026
        )
        assertEquals(0L, log.id)
        assertEquals("2026-02-10", log.date)
        assertEquals(2, log.month)
        assertEquals(2026, log.year)
    }

    // --- VacancyLog Entity Tests ---

    @Test
    fun vacancyLog_creation() {
        val vacancy = VacancyLog(
            apartmentId = 1,
            startDate = "2026-02-01",
            endDate = "2026-02-15"
        )
        assertEquals(0L, vacancy.id)
        assertEquals(1L, vacancy.apartmentId)
        assertEquals("2026-02-01", vacancy.startDate)
        assertEquals("2026-02-15", vacancy.endDate)
    }

    // --- TypeConverter Tests ---

    @Test
    fun typeConverter_localDateToString() {
        val converter = TypeConverters()
        val date = LocalDate.of(2026, 2, 10)
        val result = converter.fromLocalDate(date)
        assertEquals("2026-02-10", result)
    }

    @Test
    fun typeConverter_stringToLocalDate() {
        val converter = TypeConverters()
        val result = converter.toLocalDate("2026-02-10")
        assertEquals(LocalDate.of(2026, 2, 10), result)
    }

    @Test
    fun typeConverter_nullHandling() {
        val converter = TypeConverters()
        assertNull(converter.fromLocalDate(null))
        assertNull(converter.toLocalDate(null))
    }

    @Test
    fun typeConverter_roundTrip() {
        val converter = TypeConverters()
        val original = LocalDate.of(2026, 12, 31)
        val asString = converter.fromLocalDate(original)
        val backToDate = converter.toLocalDate(asString)
        assertEquals(original, backToDate)
    }
}

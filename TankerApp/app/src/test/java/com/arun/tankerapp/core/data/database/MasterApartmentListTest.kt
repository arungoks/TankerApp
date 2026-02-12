package com.arun.tankerapp.core.data.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MasterApartmentListTest {

    @Test
    fun masterList_hasCorrectSize() {
        assertEquals(68, MasterApartmentList.apartments.size)
    }

    @Test
    fun masterList_hasNoDuplicates() {
        val unique = MasterApartmentList.apartments.toSet()
        assertEquals(MasterApartmentList.apartments.size, unique.size)
    }

    @Test
    fun masterList_followsNamingConvention() {
        // Check for 3 or 4 digits
        MasterApartmentList.apartments.forEach {
            assertTrue("Invalid format: $it", it.matches(Regex("\\d{3,4}")))
            assertTrue("Floor too low: $it", it.toInt() >= 101)
            assertTrue("Floor too high: $it", it.toInt() <= 1704)
        }
    }
}

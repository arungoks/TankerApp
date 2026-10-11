package com.arun.tankerapp.core.data.repository

import com.arun.tankerapp.core.data.model.TankerPrice
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TankerPricingRepositoryTest {

    @Test
    fun getPriceForDate_emptyList_returnsZero() {
        val result = TankerPricingRepository.getPriceForDate(LocalDate.of(2026, 10, 11), emptyList())
        assertEquals(0.0, result, 0.001)
    }

    @Test
    fun getPriceForDate_singleEntry_returnsThatPriceForSameDate() {
        val prices = listOf(
            TankerPrice(id = "1", effectiveDate = LocalDate.of(2026, 10, 1), price = 4843.0)
        )
        val result = TankerPricingRepository.getPriceForDate(LocalDate.of(2026, 10, 1), prices)
        assertEquals(4843.0, result, 0.001)
    }

    @Test
    fun getPriceForDate_singleEntry_returnsThatPriceForFutureDate() {
        val prices = listOf(
            TankerPrice(id = "1", effectiveDate = LocalDate.of(2026, 10, 1), price = 4843.0)
        )
        val result = TankerPricingRepository.getPriceForDate(LocalDate.of(2026, 10, 10), prices)
        assertEquals(4843.0, result, 0.001)
    }

    @Test
    fun getPriceForDate_singleEntry_fallsBackToEarliestForPrecedingDate() {
        val prices = listOf(
            TankerPrice(id = "1", effectiveDate = LocalDate.of(2026, 10, 1), price = 4843.0)
        )
        val result = TankerPricingRepository.getPriceForDate(LocalDate.of(2026, 9, 28), prices)
        assertEquals(4843.0, result, 0.001)
    }

    @Test
    fun getPriceForDate_twoEntries_userScenario_correctlyPicksOldRateBeforeChangeAndNewRateOnOrAfter() {
        // Oct 1: 4843, Oct 11: 5083
        val prices = listOf(
            TankerPrice(id = "2", effectiveDate = LocalDate.of(2026, 10, 11), price = 5083.0),
            TankerPrice(id = "1", effectiveDate = LocalDate.of(2026, 10, 1), price = 4843.0)
        )

        // Yesterday (Oct 10) must be 4843
        val priceYesterday = TankerPricingRepository.getPriceForDate(LocalDate.of(2026, 10, 10), prices)
        assertEquals(4843.0, priceYesterday, 0.001)

        // Earlier in cycle (Oct 5) must be 4843
        val priceOct5 = TankerPricingRepository.getPriceForDate(LocalDate.of(2026, 10, 5), prices)
        assertEquals(4843.0, priceOct5, 0.001)

        // Exactly on effective date (Oct 11) must be 5083
        val priceToday = TankerPricingRepository.getPriceForDate(LocalDate.of(2026, 10, 11), prices)
        assertEquals(5083.0, priceToday, 0.001)

        // Future in cycle (Oct 15) must be 5083
        val priceFuture = TankerPricingRepository.getPriceForDate(LocalDate.of(2026, 10, 15), prices)
        assertEquals(5083.0, priceFuture, 0.001)
    }

    @Test
    fun getPriceForDate_multipleEntries_selectsMostRecentEffectiveDateOnOrBeforeTarget() {
        val prices = listOf(
            TankerPrice(id = "3", effectiveDate = LocalDate.of(2026, 11, 1), price = 5500.0),
            TankerPrice(id = "2", effectiveDate = LocalDate.of(2026, 10, 11), price = 5083.0),
            TankerPrice(id = "1", effectiveDate = LocalDate.of(2026, 10, 1), price = 4843.0)
        )

        assertEquals(4843.0, TankerPricingRepository.getPriceForDate(LocalDate.of(2026, 10, 10), prices), 0.001)
        assertEquals(5083.0, TankerPricingRepository.getPriceForDate(LocalDate.of(2026, 10, 11), prices), 0.001)
        assertEquals(5083.0, TankerPricingRepository.getPriceForDate(LocalDate.of(2026, 10, 31), prices), 0.001)
        assertEquals(5500.0, TankerPricingRepository.getPriceForDate(LocalDate.of(2026, 11, 1), prices), 0.001)
        assertEquals(5500.0, TankerPricingRepository.getPriceForDate(LocalDate.of(2026, 11, 5), prices), 0.001)
    }
}

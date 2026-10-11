package com.arun.tankerapp.ui.admin.pricing

import com.arun.tankerapp.MainDispatcherRule
import com.arun.tankerapp.core.data.model.TankerPrice
import com.arun.tankerapp.core.data.repository.TankerPricingRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

class ManageTankerPricingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val pricingRepository: TankerPricingRepository = mock()

    @Test
    fun loadPricing_populatesHistoryAndCurrentPrice() = runTest {
        val today = LocalDate.now()
        val history = listOf(
            TankerPrice(id = "1", effectiveDate = today, price = 5083.0)
        )
        whenever(pricingRepository.getPricingHistory()).thenReturn(flowOf(history))

        val viewModel = ManageTankerPricingViewModel(pricingRepository)
        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertEquals(1, state.priceHistory.size)
        assertEquals(5083.0, state.currentPrice ?: 0.0, 0.001)
    }

    @Test
    fun savePrice_withInvalidInput_setsErrorAndDoesNotCallRepo() = runTest {
        whenever(pricingRepository.getPricingHistory()).thenReturn(flowOf(emptyList()))

        val viewModel = ManageTankerPricingViewModel(pricingRepository)
        viewModel.onPriceInputChange("abc")
        viewModel.savePrice()

        val state = viewModel.uiState.value
        assertNotNull(state.priceInputError)
        assertEquals("Enter a valid price greater than 0", state.priceInputError)
    }

    @Test
    fun savePrice_withZeroOrNegative_setsError() = runTest {
        whenever(pricingRepository.getPricingHistory()).thenReturn(flowOf(emptyList()))

        val viewModel = ManageTankerPricingViewModel(pricingRepository)
        viewModel.onPriceInputChange("0")
        viewModel.savePrice()

        val state = viewModel.uiState.value
        assertNotNull(state.priceInputError)

        viewModel.onPriceInputChange("-100")
        viewModel.savePrice()
        assertNotNull(viewModel.uiState.value.priceInputError)
    }

    @Test
    fun savePrice_withValidInput_callsRepositoryAndResetsInput() = runTest {
        whenever(pricingRepository.getPricingHistory()).thenReturn(flowOf(emptyList()))

        val viewModel = ManageTankerPricingViewModel(pricingRepository)
        val targetDate = LocalDate.of(2026, 10, 11)
        viewModel.onEffectiveDateChange(targetDate)
        viewModel.onPriceInputChange("5083.0")
        viewModel.savePrice()

        verify(pricingRepository).setTankerPrice(targetDate, 5083.0)
        val state = viewModel.uiState.value
        assertEquals("", state.priceInput)
        assertNull(state.priceInputError)
    }

    @Test
    fun deletePrice_callsRepository() = runTest {
        whenever(pricingRepository.getPricingHistory()).thenReturn(flowOf(emptyList()))

        val viewModel = ManageTankerPricingViewModel(pricingRepository)
        val dateToDelete = LocalDate.of(2026, 10, 1)
        viewModel.deletePrice(dateToDelete)

        verify(pricingRepository).deleteTankerPrice(dateToDelete)
    }
}

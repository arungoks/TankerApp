package com.arun.tankerapp.ui.admin

import com.arun.tankerapp.MainDispatcherRule
import com.arun.tankerapp.core.data.model.firestore.BillingCycleDocument
import com.arun.tankerapp.core.data.repository.BillingRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

class EditBillingCycleViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val billingRepository: BillingRepository = mock()

    @Test
    fun initial_state_loads_cycle_and_sets_submit_disabled() = runTest {
        val cycle = BillingCycleDocument(
            id = "doc123",
            startDate = "2026-09-01",
            endDate = "2026-09-20",
            totalTankers = 42,
            ownerId = "Global"
        )
        whenever(billingRepository.getLatestBillingCycle()).thenReturn(flowOf(cycle))

        val viewModel = EditBillingCycleViewModel(billingRepository)
        val state = viewModel.uiState.value

        assertTrue(state.hasCycle)
        assertFalse(state.isLoading)
        assertEquals(LocalDate.of(2026, 9, 1), state.startDate)
        assertEquals(LocalDate.of(2026, 9, 20), state.originalEndDate)
        assertEquals(LocalDate.of(2026, 9, 20), state.selectedEndDate)
        assertEquals(42, state.totalTankers)
        assertFalse(state.isModified)
        assertFalse(state.canSubmit)
    }

    @Test
    fun empty_cycle_sets_hasCycle_false_and_submit_disabled() = runTest {
        whenever(billingRepository.getLatestBillingCycle()).thenReturn(flowOf(null))

        val viewModel = EditBillingCycleViewModel(billingRepository)
        val state = viewModel.uiState.value

        assertFalse(state.hasCycle)
        assertFalse(state.isLoading)
        assertFalse(state.canSubmit)
    }

    @Test
    fun selecting_different_valid_date_enables_submit() = runTest {
        val cycle = BillingCycleDocument(
            id = "doc123",
            startDate = "2026-09-01",
            endDate = "2026-09-20",
            totalTankers = 42,
            ownerId = "Global"
        )
        whenever(billingRepository.getLatestBillingCycle()).thenReturn(flowOf(cycle))

        val viewModel = EditBillingCycleViewModel(billingRepository)
        val newDate = LocalDate.of(2026, 9, 15)
        viewModel.onDateSelected(newDate)

        val state = viewModel.uiState.value
        assertEquals(newDate, state.selectedEndDate)
        assertTrue(state.isModified)
        assertTrue(state.canSubmit)
    }

    @Test
    fun reselecting_original_date_disables_submit() = runTest {
        val cycle = BillingCycleDocument(
            id = "doc123",
            startDate = "2026-09-01",
            endDate = "2026-09-20",
            totalTankers = 42,
            ownerId = "Global"
        )
        whenever(billingRepository.getLatestBillingCycle()).thenReturn(flowOf(cycle))

        val viewModel = EditBillingCycleViewModel(billingRepository)
        viewModel.onDateSelected(LocalDate.of(2026, 9, 15))
        assertTrue(viewModel.uiState.value.canSubmit)

        viewModel.onDateSelected(LocalDate.of(2026, 9, 20))
        assertFalse(viewModel.uiState.value.isModified)
        assertFalse(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun selecting_date_before_start_date_is_rejected() = runTest {
        val cycle = BillingCycleDocument(
            id = "doc123",
            startDate = "2026-09-05",
            endDate = "2026-09-20",
            totalTankers = 42,
            ownerId = "Global"
        )
        whenever(billingRepository.getLatestBillingCycle()).thenReturn(flowOf(cycle))

        val viewModel = EditBillingCycleViewModel(billingRepository)
        viewModel.onDateSelected(LocalDate.of(2026, 9, 4))

        val state = viewModel.uiState.value
        assertEquals(LocalDate.of(2026, 9, 20), state.selectedEndDate)
        assertFalse(state.isModified)
        assertFalse(state.canSubmit)
    }

    @Test
    fun selecting_date_in_future_is_rejected() = runTest {
        val cycle = BillingCycleDocument(
            id = "doc123",
            startDate = "2026-09-01",
            endDate = "2026-09-20",
            totalTankers = 42,
            ownerId = "Global"
        )
        whenever(billingRepository.getLatestBillingCycle()).thenReturn(flowOf(cycle))

        val viewModel = EditBillingCycleViewModel(billingRepository)
        viewModel.onDateSelected(LocalDate.now().plusDays(2))

        val state = viewModel.uiState.value
        assertEquals(LocalDate.of(2026, 9, 20), state.selectedEndDate)
        assertFalse(state.isModified)
        assertFalse(state.canSubmit)
    }

    @Test
    fun submit_success_updates_repository_and_resets_dirty_state() = runTest {
        val cycle = BillingCycleDocument(
            id = "doc123",
            startDate = "2026-09-01",
            endDate = "2026-09-20",
            totalTankers = 42,
            ownerId = "Global"
        )
        whenever(billingRepository.getLatestBillingCycle()).thenReturn(flowOf(cycle))

        val viewModel = EditBillingCycleViewModel(billingRepository)
        val newDate = LocalDate.of(2026, 9, 18)
        viewModel.onDateSelected(newDate)

        viewModel.onSubmit()

        verify(billingRepository).updateBillingCycleEndDate("doc123", newDate)

        val state = viewModel.uiState.value
        assertEquals(newDate, state.originalEndDate)
        assertEquals(newDate, state.selectedEndDate)
        assertFalse(state.isSaving)
        assertFalse(state.isModified)
        assertFalse(state.canSubmit)
    }
}

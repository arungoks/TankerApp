package com.arun.tankerapp.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arun.tankerapp.core.data.repository.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class EditBillingCycleUiState(
    val isLoading: Boolean = true,
    val hasCycle: Boolean = false,
    val cycleId: String = "",
    val startDate: LocalDate? = null,
    val originalEndDate: LocalDate? = null,
    val selectedEndDate: LocalDate? = null,
    val totalTankers: Int = 0,
    val isSaving: Boolean = false
) {
    val isModified: Boolean
        get() = hasCycle && selectedEndDate != null && selectedEndDate != originalEndDate

    val canSubmit: Boolean
        get() = isModified && !isSaving && !isLoading
}

@HiltViewModel
class EditBillingCycleViewModel @Inject constructor(
    private val billingRepository: BillingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditBillingCycleUiState())
    val uiState: StateFlow<EditBillingCycleUiState> = _uiState.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    init {
        loadLatestCycle()
    }

    fun loadLatestCycle() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            billingRepository.getLatestBillingCycle().collect { cycleDoc ->
                if (cycleDoc != null && cycleDoc.startDate.isNotBlank() && cycleDoc.endDate.isNotBlank()) {
                    val sDate = try { LocalDate.parse(cycleDoc.startDate) } catch (e: Exception) { null }
                    val eDate = try { LocalDate.parse(cycleDoc.endDate) } catch (e: Exception) { null }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasCycle = true,
                            cycleId = cycleDoc.id ?: cycleDoc.startDate,
                            startDate = sDate,
                            originalEndDate = eDate,
                            selectedEndDate = eDate,
                            totalTankers = cycleDoc.totalTankers
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasCycle = false,
                            cycleId = "",
                            startDate = null,
                            originalEndDate = null,
                            selectedEndDate = null,
                            totalTankers = 0
                        )
                    }
                }
            }
        }
    }

    fun onDateSelected(date: LocalDate) {
        val state = _uiState.value
        val startDate = state.startDate ?: return
        val today = LocalDate.now()

        if (date.isBefore(startDate)) {
            viewModelScope.launch {
                _snackbarMessage.emit("End date cannot be before cycle start date ($startDate)")
            }
            return
        }
        if (date.isAfter(today)) {
            viewModelScope.launch {
                _snackbarMessage.emit("End date cannot be in the future")
            }
            return
        }

        _uiState.update { it.copy(selectedEndDate = date) }
    }

    fun onSubmit() {
        val state = _uiState.value
        if (!state.canSubmit) return

        val cycleId = state.cycleId
        val newEndDate = state.selectedEndDate ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                billingRepository.updateBillingCycleEndDate(cycleId, newEndDate)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        originalEndDate = newEndDate,
                        selectedEndDate = newEndDate
                    )
                }
                _snackbarMessage.emit("Billing cycle end date updated successfully")
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                _snackbarMessage.emit("Failed to update: ${e.message ?: "Unknown error"}")
            }
        }
    }
}

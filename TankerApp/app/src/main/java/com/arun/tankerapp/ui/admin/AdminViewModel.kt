package com.arun.tankerapp.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arun.tankerapp.core.data.repository.VacancyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AdminUiState {
    object Idle : AdminUiState
    object Loading : AdminUiState
    object Success : AdminUiState
    data class ApartmentLoaded(val apartmentNumber: String, val currentOccupancy: Int) : AdminUiState
    data class Error(val message: String) : AdminUiState
}

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val vacancyRepository: VacancyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminUiState>(AdminUiState.Idle)
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    fun validatePin(pin: String): Boolean {
        return if (pin == AdminConstants.ADMIN_PIN) {
            _uiState.update { AdminUiState.Success }
            true
        } else {
            _uiState.update { AdminUiState.Error("Incorrect PIN") }
            false
        }
    }
    
    fun resetState() {
        _uiState.update { AdminUiState.Idle }
    }

    fun validateApartmentNumber(apartmentNumber: String) {
        val trimmed = apartmentNumber.trim()
        // val regex = Regex("^[A-Za-z]-\\d{1,4}$")
        val regex = Regex("""^\d{3,4}$""")
        if (regex.matches(trimmed)) {
            fetchApartmentData(trimmed.uppercase())
        } else {
            _uiState.update { AdminUiState.Error("Invalid format. Use 3 or 4 digits (e.g., 101, 1001)") }
        }
    }

    private fun fetchApartmentData(apartmentNumber: String) {
        _uiState.update { AdminUiState.Loading }
        viewModelScope.launch {
            val result = vacancyRepository.getApartmentOccupancy(apartmentNumber)
            result.onSuccess { occupancy ->
                _uiState.update { AdminUiState.ApartmentLoaded(apartmentNumber, occupancy) }
            }.onFailure { error ->
                _uiState.update { AdminUiState.Error(error.localizedMessage ?: "Failed to fetch data") }
            }
        }
    }

    fun updateOccupancy(apartmentNumber: String, newCount: Int) {
        val currentState = _uiState.value
        _uiState.update { AdminUiState.Loading } // Emit loading state while preserving data visually handled in UI if needed, but standard is loading
        viewModelScope.launch {
            val result = vacancyRepository.updateDefaultOccupancy(apartmentNumber, newCount)
            result.onSuccess {
                _snackbarMessage.emit("Occupancy for $apartmentNumber updated successfully")
                _uiState.update { AdminUiState.Idle } // Return to search state
            }.onFailure { error ->
                _snackbarMessage.emit(error.localizedMessage ?: "Failed to update occupancy")
                // Revert to loaded state to let them try again without re-entering apartment
                _uiState.update { currentState }
            }
        }
    }
}

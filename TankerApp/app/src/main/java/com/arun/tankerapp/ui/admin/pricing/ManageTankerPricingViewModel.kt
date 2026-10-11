package com.arun.tankerapp.ui.admin.pricing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arun.tankerapp.core.data.model.TankerPrice
import com.arun.tankerapp.core.data.repository.TankerPricingRepository
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

data class ManageTankerPricingUiState(
    val priceHistory: List<TankerPrice> = emptyList(),
    val currentPrice: Double? = null,
    val priceInput: String = "",
    val effectiveDate: LocalDate = LocalDate.now(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val priceInputError: String? = null
)

@HiltViewModel
class ManageTankerPricingViewModel @Inject constructor(
    private val pricingRepository: TankerPricingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManageTankerPricingUiState())
    val uiState: StateFlow<ManageTankerPricingUiState> = _uiState.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    init {
        loadPricing()
    }

    private fun loadPricing() {
        viewModelScope.launch {
            pricingRepository.getPricingHistory().collect { history ->
                val today = LocalDate.now()
                val current = if (history.isNotEmpty()) {
                    TankerPricingRepository.getPriceForDate(today, history)
                } else null

                _uiState.update {
                    it.copy(
                        priceHistory = history,
                        currentPrice = current,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onPriceInputChange(input: String) {
        _uiState.update {
            it.copy(
                priceInput = input,
                priceInputError = null
            )
        }
    }

    fun onEffectiveDateChange(date: LocalDate) {
        _uiState.update {
            it.copy(effectiveDate = date)
        }
    }

    fun savePrice() {
        val currentState = _uiState.value
        val priceVal = currentState.priceInput.toDoubleOrNull()
        if (priceVal == null || priceVal <= 0.0) {
            _uiState.update {
                it.copy(priceInputError = "Enter a valid price greater than 0")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                pricingRepository.setTankerPrice(currentState.effectiveDate, priceVal)
                _snackbarMessage.emit("Tanker price for ${currentState.effectiveDate} saved successfully")
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        priceInput = "",
                        effectiveDate = LocalDate.now(),
                        priceInputError = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                _snackbarMessage.emit(e.localizedMessage ?: "Failed to save tanker price")
            }
        }
    }

    fun deletePrice(effectiveDate: LocalDate) {
        viewModelScope.launch {
            try {
                pricingRepository.deleteTankerPrice(effectiveDate)
                _snackbarMessage.emit("Price entry for $effectiveDate deleted")
            } catch (e: Exception) {
                _snackbarMessage.emit(e.localizedMessage ?: "Failed to delete price entry")
            }
        }
    }
}

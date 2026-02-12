package com.arun.tankerapp.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arun.tankerapp.core.data.database.entity.BillingCycle
import com.arun.tankerapp.core.data.repository.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import com.arun.tankerapp.ui.base.BaseViewModel
import com.arun.tankerapp.core.ui.SnackbarManager
import kotlinx.coroutines.flow.catch
import timber.log.Timber

data class HistoryUiState(
    val cycles: List<BillingCycle> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val billingRepository: BillingRepository,
    private val snackbarManager: SnackbarManager
) : BaseViewModel(snackbarManager) {

    val uiState: StateFlow<HistoryUiState> = billingRepository.getBillingHistory()
        .map { HistoryUiState(cycles = it, isLoading = false) }
        .catch { e ->
            Timber.e(e, "Error loading history")
            launchCatching { snackbarManager.showMessage("Error loading history: ${e.message}") }
            emit(HistoryUiState(isLoading = false))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HistoryUiState(isLoading = true)
        )
}

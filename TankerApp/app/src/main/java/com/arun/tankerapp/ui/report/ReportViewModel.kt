package com.arun.tankerapp.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arun.tankerapp.core.data.repository.ApartmentBill
import com.arun.tankerapp.core.data.repository.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.arun.tankerapp.core.data.repository.UserPreferencesRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

import androidx.lifecycle.SavedStateHandle
import com.arun.tankerapp.ui.base.BaseViewModel
import com.arun.tankerapp.core.ui.SnackbarManager
import kotlinx.coroutines.flow.catch
import timber.log.Timber

data class ReportUiState(
    val bills: List<ApartmentBill> = emptyList(),
    val totalTankers: Int = 0,
    val isLoading: Boolean = true,
    val isHistoryMode: Boolean = false
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val billingRepository: BillingRepository,
    private val reportGenerator: com.arun.tankerapp.core.domain.ReportGenerator,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val savedStateHandle: SavedStateHandle,
    private val snackbarManager: SnackbarManager
) : BaseViewModel(snackbarManager) {

    private val _shareEvent = kotlinx.coroutines.flow.MutableSharedFlow<ShareEvent>()
    val shareEvent = _shareEvent.asSharedFlow()

    sealed class ShareEvent {
        data class ShareText(val text: String) : ShareEvent()
        data class ShareCsv(val uri: android.net.Uri) : ShareEvent()
        object CycleReset : ShareEvent()
    }

    private val startDateArg: String? = savedStateHandle["startDate"]
    private val endDateArg: String? = savedStateHandle["endDate"]
    private val isHistoryMode = startDateArg != null

    val uiState: StateFlow<ReportUiState> = (if (isHistoryMode) {
        val start = LocalDate.parse(startDateArg)
        val end = endDateArg?.let { LocalDate.parse(it) }
        billingRepository.getBillingReport(fromDate = start, toDate = end).map { bills ->
            val total = bills.firstOrNull()?.totalTankersInCycle ?: 0
            ReportUiState(
                bills = bills,
                totalTankers = total,
                isLoading = false,
                isHistoryMode = true
            )
        }
    } else {
        userPreferencesRepository.getLastReportDate()
            .flatMapLatest { lastDate ->
                billingRepository.getBillingReport(fromDate = lastDate).map { bills ->
                    val total = bills.firstOrNull()?.totalTankersInCycle ?: 0
                    ReportUiState(
                        bills = bills,
                        totalTankers = total,
                        isLoading = false,
                        isHistoryMode = false
                    )
                }
            }
    })
        .onStart { emit(ReportUiState(isLoading = true)) } // Initial loading state
        .catch { e ->
             Timber.e(e, "Error loading report")
             launchCatching { snackbarManager.showMessage("Error loading report: ${e.message}") }
             emit(ReportUiState(isLoading = false))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ReportUiState(isLoading = true)
        )

    fun onShareText() {
        val state = uiState.value
        if (!state.isLoading) {
            val text = reportGenerator.generateTextReport(state.bills, state.totalTankers)
            launchCatching {
                _shareEvent.emit(ShareEvent.ShareText(text))
            }
        }
    }

    fun onShareCsv() {
        val state = uiState.value
        if (!state.isLoading) {
            launchCatching {
                // Get Last Report Date (From Date)
                val lastReportDate = userPreferencesRepository.getLastReportDate().firstOrNull() 
                    ?: LocalDate.now().withDayOfMonth(1) // Default to 1st of current month if null
                
                val currentDate = LocalDate.now() // To Date
                
                val file = reportGenerator.generateCsvReport(
                    bills = state.bills, 
                    totalTankers = state.totalTankers,
                    fromDate = lastReportDate,
                    toDate = currentDate
                )
                
                // TODO: Update Last Report Date *after* successful share/cycle reset? 
                // For now, per requirement, just using it. Preserving/Updating might belong to 'Reset' action.
                
                val uri = reportGenerator.getUriForFile(file)
                _shareEvent.emit(ShareEvent.ShareCsv(uri))
            }
        }
    }

    fun onResetCycle() {
        launchCatching {
            val currentState = uiState.value
            if (!currentState.isLoading) {
                // Get Last Report Date (Start of Current Cycle)
                val lastReportDate = userPreferencesRepository.getLastReportDate().firstOrNull() 
                    ?: LocalDate.now().withDayOfMonth(1)
                
                // End of Current Cycle is Today
                val currentDate = LocalDate.now()
                
                // Archive the cycle
                billingRepository.archiveCurrentCycle(
                    startDate = lastReportDate,
                    endDate = currentDate,
                    totalTankers = currentState.totalTankers
                )
                
                // Reset to start new cycle from today
                userPreferencesRepository.setLastReportDate(currentDate)
                _shareEvent.emit(ShareEvent.CycleReset)
            }
        }
    }
}

package com.arun.tankerapp.ui.calendar

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

import androidx.lifecycle.viewModelScope
import com.arun.tankerapp.core.data.repository.ApartmentStatus
import com.arun.tankerapp.core.data.repository.TankerRepository
import com.arun.tankerapp.core.data.repository.VacancyRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.arun.tankerapp.ui.base.BaseViewModel
import com.arun.tankerapp.core.ui.SnackbarManager
import com.arun.tankerapp.core.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.firstOrNull

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val vacancyRepository: VacancyRepository,
    private val tankerRepository: TankerRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val snackbarManager: SnackbarManager
) : BaseViewModel(snackbarManager) {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val apartmentStatuses: StateFlow<List<ApartmentStatus>> = _uiState
        .map { it.selectedDate }
        .distinctUntilChanged()
        .flatMapLatest { date ->
            vacancyRepository.getApartmentStatuses(date)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val tankerCount: StateFlow<Int> = _uiState
        .map { it.selectedDate }
        .distinctUntilChanged()
        .flatMapLatest { date ->
            tankerRepository.getTankerCount(date)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val vacanciesInMonth: StateFlow<Set<LocalDate>> = _uiState
        .map { it.currentMonth }
        .distinctUntilChanged()
        .flatMapLatest { month ->
            vacancyRepository.getVacanciesForMonth(month).map { logs ->
                val monthStart = month.atDay(1)
                val monthEnd = month.atEndOfMonth()
                
                logs.flatMap { log ->
                    // Parse dates
                    // Parse dates safely
                    val start = try {
                        if (log.startDate.isBlank()) return@flatMap emptyList()
                        LocalDate.parse(log.startDate)
                    } catch (e: Exception) {
                        return@flatMap emptyList()
                    }

                    val end = if (log.endDate.isBlank()) {
                        monthEnd // Treat open-ended vacancy as extending to end of month
                    } else {
                        try {
                            LocalDate.parse(log.endDate)
                        } catch (e: Exception) {
                            monthEnd
                        }
                    }
                    
                    // Clamp to current month view
                    val clampedStart = if (start.isBefore(monthStart)) monthStart else start
                    val clampedEnd = if (end.isAfter(monthEnd)) monthEnd else end
                    
                    if (clampedStart.isAfter(clampedEnd)) {
                        emptyList()
                    } else {
                        val dates = mutableListOf<LocalDate>()
                        var d = clampedStart
                        while (!d.isAfter(clampedEnd)) {
                            dates.add(d)
                            d = d.plusDays(1)
                        }
                        dates
                    }
                }.toSet()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    @OptIn(ExperimentalCoroutinesApi::class)
    val tankerDatesInMonth: StateFlow<Set<LocalDate>> = _uiState
        .map { it.currentMonth }
        .distinctUntilChanged()
        .flatMapLatest { month ->
            tankerRepository.getTankersForMonth(month).map { logs ->
                logs.filter { it.count > 0 && it.date.isNotBlank() }
                    .mapNotNull { 
                        try { LocalDate.parse(it.date) } catch (e: Exception) { null }
                    }
                    .toSet()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    /**
     * Total tanker count for the current billing cycle.
     * This is displayed in the sticky header as "X/8".
     */
    val currentCycleTankerCount: StateFlow<Int> = tankerRepository
        .getCurrentCycleTankerCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun onPreviousMonth() {
        _uiState.update { currentState ->
            currentState.copy(currentMonth = currentState.currentMonth.minusMonths(1))
        }
    }

    fun onNextMonth() {
        _uiState.update { currentState ->
            currentState.copy(currentMonth = currentState.currentMonth.plusMonths(1))
        }
    }

    fun onToday() {
        val today = LocalDate.now()
        _uiState.update { currentState ->
            currentState.copy(
                currentMonth = YearMonth.from(today),
                selectedDate = today,
                showBottomSheet = true 
            )
        }
    }

    fun onDateSelected(date: LocalDate) {
        _uiState.update { currentState ->
            currentState.copy(
                selectedDate = date,
                showBottomSheet = true
            )
        }
    }

    fun onDismissBottomSheet() {
        _uiState.update { it.copy(showBottomSheet = false) }
    }

    val isEditAllowed: StateFlow<Boolean> = kotlinx.coroutines.flow.combine(
        _uiState.map { it.selectedDate }.distinctUntilChanged(),
        userPreferencesRepository.getLastReportDate()
    ) { date, lastReportDate ->
        val limit = lastReportDate ?: LocalDate.MIN
        !date.isBefore(limit)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun onToggleVacancy(apartmentId: Long, isVacant: Boolean) {
        launchCatching {
            if (!isEditAllowed.value) {
                snackbarManager.showMessage("Cannot edit past records (Report Generated)")
                return@launchCatching
            }
            val date = _uiState.value.selectedDate
            vacancyRepository.toggleVacancy(apartmentId, date, isVacant)
        }
    }

    fun onIncrementTanker() {
        launchCatching {
            if (!isEditAllowed.value) {
                snackbarManager.showMessage("Cannot edit past records (Report Generated)")
                return@launchCatching
            }
            val date = _uiState.value.selectedDate
            tankerRepository.incrementTankerCount(date)
        }
    }

    fun onDecrementTanker() {
        launchCatching {
            if (!isEditAllowed.value) {
                snackbarManager.showMessage("Cannot edit past records (Report Generated)")
                return@launchCatching
            }
            val date = _uiState.value.selectedDate
            tankerRepository.decrementTankerCount(date)
        }
    }

    fun onOccupancyChanged(apartmentId: Long, count: Int) {
        launchCatching {
            if (!isEditAllowed.value) {
                snackbarManager.showMessage("Cannot edit past records (Report Generated)")
                return@launchCatching
            }
            if (count < 0) {
                 snackbarManager.showMessage("Occupancy cannot be negative")
                 return@launchCatching
            }
            val date = _uiState.value.selectedDate
            vacancyRepository.updateOccupancy(apartmentId, date, count)
        }
    }
}

package com.arun.tankerapp.ui.calendar

import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val showBottomSheet: Boolean = false
)

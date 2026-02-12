package com.arun.tankerapp.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateToAbout: () -> Unit,
    onNavigateToReport: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val apartmentStatuses by viewModel.apartmentStatuses.collectAsState()
    val vacanciesInMonth by viewModel.vacanciesInMonth.collectAsState()
    val tankerCount by viewModel.tankerCount.collectAsState()
    val tankerDatesInMonth by viewModel.tankerDatesInMonth.collectAsState()
    val currentCycleTankerCount by viewModel.currentCycleTankerCount.collectAsState()

    if (uiState.showBottomSheet) {
        VacancyEntryBottomSheet(
            date = uiState.selectedDate,
            apartmentStatuses = apartmentStatuses,
            tankerCount = tankerCount,
            onToggleVacancy = viewModel::onToggleVacancy,
            onIncrementTanker = viewModel::onIncrementTanker,
            onDecrementTanker = viewModel::onDecrementTanker,
            onDismiss = viewModel::onDismissBottomSheet
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TankerApp") },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                    IconButton(onClick = onNavigateToAbout) {
                        Icon(Icons.Default.Info, contentDescription = "About")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tanker Cycle Counter - Sticky Header
            TankerCounterHeader(
                currentCount = currentCycleTankerCount,
                onGenerateBill = onNavigateToReport
            )

            // Month Navigation Header
            CalendarHeader(
                currentMonth = uiState.currentMonth,
                onPreviousMonth = viewModel::onPreviousMonth,
                onNextMonth = viewModel::onNextMonth,
                onToday = viewModel::onToday
            )

            // Days of Week Header
            DaysOfWeekHeader()

            // Calendar Grid
            CalendarGrid(
                currentMonth = uiState.currentMonth,
                selectedDate = uiState.selectedDate,
                vacantDates = vacanciesInMonth,
                tankerDates = tankerDatesInMonth,
                onDateSelected = viewModel::onDateSelected
            )
        }
    }
}

@Composable
fun CalendarHeader(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${currentMonth.year}",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row {
            IconButton(onClick = onNextMonth) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
            }
            IconButton(onClick = onToday) {
                Text("Today", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun DaysOfWeekHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        DayOfWeek.values().forEach { dayOfWeek ->
            Text(
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate?,
    vacantDates: Set<LocalDate> = emptySet(),
    tankerDates: Set<LocalDate> = emptySet(),
    onDateSelected: (LocalDate) -> Unit
) {
    val days = generateCalendarDays(currentMonth)

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.fillMaxSize()
    ) {
        items(days) { day ->
            if (day != null) {
                DayCell(
                    date = day,
                    isSelected = day == selectedDate,
                    isToday = day == LocalDate.now(),
                    hasVacancy = vacantDates.contains(day),
                    hasTanker = tankerDates.contains(day),
                    onClick = { onDateSelected(day) }
                )
            } else {
                Box(modifier = Modifier.aspectRatio(1f)) // Placeholder for empty cells
            }
        }
    }
}

@Composable
fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    hasVacancy: Boolean,
    hasTanker: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }

    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .background(backgroundColor, shape = MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${date.dayOfMonth}",
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium
        )
        
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (hasVacancy) {
                 Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Color.Red)
                        .semantics { contentDescription = "Vacancy Indicator" }
                )
            }
            if (hasTanker) {
                 Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Color.Blue)
                        .semantics { contentDescription = "Tanker Indicator" }
                )
            }
        }
    }
}

/**
 * Generates a list of LocalDate objects for the grid view.
 * returns null for empty cells before the first day of the month.
 */
fun generateCalendarDays(yearMonth: YearMonth): List<LocalDate?> {
    val firstDayOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    
    // DayOfWeek value: 1 (Mon) - 7 (Sun)
    // Grid starts at Mon (1). So offset is dayOfWeek - 1.
    // E.g. if start is Tue (2), offset is 1 empty cell.
    val startOffset = firstDayOfMonth.dayOfWeek.value - 1
    
    val days = mutableListOf<LocalDate?>()
    
    // Add nulls for padding
    repeat(startOffset) { days.add(null) }
    
    // Add actual days
    for (i in 1..daysInMonth) {
        days.add(yearMonth.atDay(i))
    }
    
    return days
}

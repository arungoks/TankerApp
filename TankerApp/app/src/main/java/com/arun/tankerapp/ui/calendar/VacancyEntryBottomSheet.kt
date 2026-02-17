package com.arun.tankerapp.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arun.tankerapp.core.data.repository.ApartmentStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VacancyEntryBottomSheet(
    date: LocalDate,
    apartmentStatuses: List<ApartmentStatus>,
    tankerCount: Int,
    isEditAllowed: Boolean = true,
    onToggleVacancy: (Long, Boolean) -> Unit,
    onOccupancyChange: (Long, Int) -> Unit,
    onIncrementTanker: () -> Unit,
    onDecrementTanker: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Log Entry for ${date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            if (!isEditAllowed) {
                Text(
                    text = "Editing is disabled for past reports.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            } else {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(bottom = 16.dp))
            }

            // Tanker Counter with +/- buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Water Tankers Received",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Decrement button
                    Button(
                        onClick = onDecrementTanker,
                        enabled = isEditAllowed && tankerCount > 0,
                        modifier = Modifier.size(48.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "−",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    
                    // Count display
                    Text(
                        text = tankerCount.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    // Increment button
                    Button(
                        onClick = onIncrementTanker,
                        enabled = isEditAllowed,
                        modifier = Modifier.size(48.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "+",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
            
            // Divider or Spacer
            Text(
                text = "Apartment Vacancies & Occupancy",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            LazyColumn {
                items(
                    items = apartmentStatuses,
                    key = { it.apartment.id }
                ) { status ->
                    ApartmentItem(
                        status = status,
                        isEnabled = isEditAllowed,
                        onToggle = { isChecked ->
                            onToggleVacancy(status.apartment.id, isChecked)
                        },
                        onOccupancyChange = { newCount ->
                            onOccupancyChange(status.apartment.id, newCount)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ApartmentItem(
    status: ApartmentStatus,
    isEnabled: Boolean = true,
    onToggle: (Boolean) -> Unit,
    onOccupancyChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Apt ${status.apartment.number}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Occupancy Controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(end = 8.dp)
        ) {
             Button(
                 onClick = { onOccupancyChange(status.occupancy - 1) },
                 enabled = isEnabled && !status.isVacant && status.occupancy > 0,
                 modifier = Modifier.size(32.dp),
                 shape = androidx.compose.foundation.shape.CircleShape,
                 contentPadding = PaddingValues(0.dp)
             ) {
                 Text("-", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
             }
             
             androidx.compose.foundation.text.BasicTextField(
                 value = status.occupancy.toString(),
                 onValueChange = { 
                     if (isEnabled && !status.isVacant) {
                         val newVal = it.toIntOrNull()
                         if (newVal != null && newVal >= 0) {
                             onOccupancyChange(newVal)
                         } else if (it.isEmpty()) {
                             onOccupancyChange(0)
                         }
                     }
                 },
                 keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                 modifier = Modifier
                     .size(width = 40.dp, height = 32.dp)
                     .background(
                         if (!isEnabled || status.isVacant) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface, 
                         MaterialTheme.shapes.small
                     )
                     .padding(top = 6.dp), // Center text vertically
                 enabled = isEnabled && !status.isVacant,
                 textStyle = androidx.compose.ui.text.TextStyle(
                     textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                     fontWeight = FontWeight.Bold,
                     color = if (!isEnabled || status.isVacant) androidx.compose.ui.graphics.Color.Gray else MaterialTheme.colorScheme.onSurface 
                 )
             )
             
             Button(
                 onClick = { onOccupancyChange(status.occupancy + 1) },
                 enabled = isEnabled && !status.isVacant,
                 modifier = Modifier.size(32.dp),
                 shape = androidx.compose.foundation.shape.CircleShape,
                 contentPadding = PaddingValues(0.dp)
             ) {
                 Text("+", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
             }
        }

        Checkbox(
            checked = status.isVacant,
            onCheckedChange = onToggle,
            enabled = isEnabled
        )
    }
}

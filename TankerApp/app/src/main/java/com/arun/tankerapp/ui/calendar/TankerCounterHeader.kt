package com.arun.tankerapp.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Sticky header that displays the current tanker cycle count.
 * Shows "Tankers: X/8" where X is the current count.
 * Visual styling changes when count reaches 8 (complete cycle).
 * Displays a "Generate Bill" button when cycle is complete.
 */
@Composable
fun TankerCounterHeader(
    currentCount: Int,
    onGenerateBill: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Enable button as soon as 1 tanker is there
    val isComplete = currentCount >= 1
    val isCycleComplete = currentCount >= 8
    
    val backgroundColor = if (isCycleComplete) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = if (isCycleComplete) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Current Billing Cycle",
                style = MaterialTheme.typography.titleMedium,
                color = textColor
            )
            
            Text(
                text = "Tankers: $currentCount/8",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
        
        // Show "Preview" button when at least 1 tanker exists
        if (isComplete) {
            FilledTonalButton(
                onClick = onGenerateBill,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Text("Preview")
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

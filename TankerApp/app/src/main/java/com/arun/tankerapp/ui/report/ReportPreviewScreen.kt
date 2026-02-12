package com.arun.tankerapp.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arun.tankerapp.core.data.repository.ApartmentBill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportPreviewScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showZeroBillable by rememberSaveable { mutableStateOf(false) }
    
    val context = LocalContext.current
    var showShareSheet by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(Unit) {
        viewModel.shareEvent.collect { event ->
            showShareSheet = false
            when (event) {
                is ReportViewModel.ShareEvent.CycleReset -> {
                     onNavigateBack() // Go back to home after reset
                }
                is ReportViewModel.ShareEvent.ShareText -> {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, event.text)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Report via"))
                }
                is ReportViewModel.ShareEvent.ShareCsv -> {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        type = "text/csv"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share CSV Report"))
                }
            }
        }
    }

    if (showShareSheet) {
        ModalBottomSheet(
            onDismissRequest = { showShareSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Share Report",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                androidx.compose.material3.Button(
                    onClick = { viewModel.onShareText() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Share as Text (WhatsApp)")
                }
                androidx.compose.material3.OutlinedButton(
                    onClick = { viewModel.onShareCsv() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export as CSV")
                }
                
                // Reset Cycle Button
                if (!uiState.isHistoryMode) {
                    androidx.compose.material3.Button(
                        onClick = { 
                            // Trigger dialog
                            showShareSheet = false 
                            showResetDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Reset & Close Cycle")
                    }
                }

                // Spacer for navigation bar
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(bottom = 32.dp))
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(text = "Reset Billing Cycle?") },
            text = { Text("This will reset the tanker counter to 0/8 and start a new billing cycle. Current cycle data will be archived. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onResetCycle()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Billing Report") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (!uiState.isLoading && uiState.bills.isNotEmpty()) {
                androidx.compose.material3.ExtendedFloatingActionButton(
                    onClick = { showShareSheet = true },
                    icon = { Icon(Icons.Default.Info, contentDescription = null) }, // Placeholder icon, maybe change to Download/Share
                    text = { Text("Generate Report") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(32.dp))
            } else {
                // Summary Header
                ReportSummaryHeader(
                    totalTankers = uiState.totalTankers,
                    totalApartments = uiState.bills.size,
                    billableApartments = uiState.bills.count { it.billableTankers > 0 }
                )

                // Filter Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = showZeroBillable,
                        onCheckedChange = { showZeroBillable = it }
                    )
                    Text(
                        text = "Show apartments with 0 billable tankers",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                HorizontalDivider()

                // List
                val filteredBills = if (showZeroBillable) {
                    uiState.bills
                } else {
                    uiState.bills.filter { it.billableTankers > 0 }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
                ) {
                    items(filteredBills) { bill ->
                        ApartmentBillItem(bill)
                    }
                    

                }
            }
        }
    }
}

@Composable
fun ReportSummaryHeader(
    totalTankers: Int,
    totalApartments: Int,
    billableApartments: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Cycle Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Total Tankers", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = totalTankers.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column {
                    Text(text = "Billable Units", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "$billableApartments / $totalApartments",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }
    }
}

@Composable
fun ApartmentBillItem(bill: ApartmentBill) {
    val isZero = bill.billableTankers == 0
    val containerColor = if (isZero) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface
    val contentColor = if (isZero) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isZero) 0.dp else 2.dp),
        border = if (isZero) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = bill.apartment.number,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
                if (bill.totalTankersInCycle > 0 && bill.billableTankers < bill.totalTankersInCycle) {
                    Text(
                        text = "Vacant for ${bill.totalTankersInCycle - bill.billableTankers} tankers",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Text(
                text = "${bill.billableTankers}",
                style = MaterialTheme.typography.headlineMedium,
                color = if (isZero) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

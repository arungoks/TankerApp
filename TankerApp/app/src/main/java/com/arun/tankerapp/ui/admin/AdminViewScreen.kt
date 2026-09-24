package com.arun.tankerapp.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class AdminMenuItem(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminViewScreen(
    onNavigateBack: () -> Unit,
    onNavigateToModifyApartment: () -> Unit,
    onNavigateToEditBillingCycle: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val latestCycle by viewModel.latestBillingCycle.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.fetchLatestBillingCycle()
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
            if (message.contains("successfully")) {
                showDeleteDialog = false
            }
        }
    }
    val menuItems = listOf(
        AdminMenuItem(
            title = "Modify Apartment Data",
            icon = Icons.Default.Home,
            onClick = onNavigateToModifyApartment
        ),
        AdminMenuItem(
            title = "Edit Last Billing Cycle",
            icon = Icons.Default.DateRange,
            onClick = onNavigateToEditBillingCycle
        ),
        AdminMenuItem(
            title = "Delete Last Billing Cycle",
            icon = Icons.Default.Delete,
            onClick = { showDeleteDialog = true }
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(menuItems) { item ->
                AdminMenuCard(item = item)
            }
        }
    }

    if (showDeleteDialog) {
        if (latestCycle != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Billing Cycle") },
                text = {
                    Column {
                        Text("Are you sure you want to delete this billing cycle? This action cannot be undone.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Dates: ${latestCycle?.startDate} to ${latestCycle?.endDate}")
                        Text("Total Tankers: ${latestCycle?.totalTankers}")
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.deleteLatestBillingCycle() },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Billing Cycle") },
                text = { Text("No Billing Cycles Found.") },
                confirmButton = {
                    TextButton(onClick = {}, enabled = false) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun AdminMenuCard(item: AdminMenuItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = item.onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

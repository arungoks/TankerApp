package com.arun.tankerapp.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModifyApartmentScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    var apartmentNumber by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    val snacks = viewModel.snackbarMessage
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snacks) {
        snacks.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lookup Apartment") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Enter Apartment Number",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = apartmentNumber,
                onValueChange = {
                    apartmentNumber = it.uppercase()
                    if (uiState !is AdminUiState.Idle) {
                        viewModel.resetState()
                    }
                },
                label = { Text("Apartment (e.g. A-101)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Search
                ),
                isError = uiState is AdminUiState.Error,
                supportingText = {
                    if (uiState is AdminUiState.Error) {
                        Text(
                            text = (uiState as AdminUiState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.validateApartmentNumber(apartmentNumber) },
                modifier = Modifier.fillMaxWidth(),
                enabled = apartmentNumber.isNotBlank()
            ) {
                Text("Search")
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (uiState is AdminUiState.Loading) {
                CircularProgressIndicator()
            }

            // Display success message for validation of 7.2
            if (uiState is AdminUiState.ApartmentLoaded) {
                val loadedState = uiState as AdminUiState.ApartmentLoaded
                
                var newOccupancy by remember(loadedState.apartmentNumber) { 
                    mutableStateOf(loadedState.currentOccupancy.toString()) 
                }
                var showDialog by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Apartment: ${loadedState.apartmentNumber}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Current Default Occupancy: ${loadedState.currentOccupancy}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = newOccupancy,
                            onValueChange = { 
                                // Only allow digits
                                if (it.all { char -> char.isDigit() }) {
                                    newOccupancy = it
                                } 
                            },
                            label = { Text("New Occupancy Count") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val isChanged = newOccupancy.isNotBlank() && newOccupancy != loadedState.currentOccupancy.toString()
                        Button(
                            onClick = { showDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isChanged
                        ) {
                            Text("Save Changes")
                        }
                    }
                }
                
                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        title = { Text("Confirm Change") },
                        text = { 
                            Text("Change occupancy for ${loadedState.apartmentNumber} from ${loadedState.currentOccupancy} to $newOccupancy?") 
                        },
                        confirmButton = {
                            TextButton(onClick = { 
                                showDialog = false 
                                viewModel.updateOccupancy(
                                    apartmentNumber = loadedState.apartmentNumber,
                                    newCount = newOccupancy.toIntOrNull() ?: loadedState.currentOccupancy
                                )
                            }) {
                                Text("Confirm")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}

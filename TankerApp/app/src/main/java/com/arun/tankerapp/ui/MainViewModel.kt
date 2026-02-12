package com.arun.tankerapp.ui

import androidx.lifecycle.ViewModel
import com.arun.tankerapp.core.ui.SnackbarManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val snackbarManager: SnackbarManager
) : ViewModel()

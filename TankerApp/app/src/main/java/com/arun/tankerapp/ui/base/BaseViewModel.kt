package com.arun.tankerapp.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arun.tankerapp.core.ui.SnackbarManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

open class BaseViewModel(
    private val snackbarManager: SnackbarManager
) : ViewModel() {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "Coroutine failed")
        viewModelScope.launch {
            snackbarManager.showMessage("An unexpected error occurred.")
        }
    }

    protected fun launchCatching(
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return viewModelScope.launch(exceptionHandler) {
            try {
                block()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Timber.e(e, "Error during execution")
                snackbarManager.showMessage("Error: ${e.message ?: "Unknown error"}")
            }
        }
    }
}

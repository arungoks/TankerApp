package com.arun.tankerapp.ui

import androidx.lifecycle.ViewModel
import com.arun.tankerapp.core.ui.SnackbarManager
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val snackbarManager: SnackbarManager,
    private val auth: FirebaseAuth
) : ViewModel() {
    
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}

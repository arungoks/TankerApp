package com.arun.tankerapp.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.arun.tankerapp.ui.about.AboutScreen
import com.arun.tankerapp.ui.calendar.CalendarScreen
import com.arun.tankerapp.ui.navigation.Screen
import com.arun.tankerapp.ui.splash.SplashScreen

import com.arun.tankerapp.ui.report.ReportPreviewScreen

import com.arun.tankerapp.ui.login.LoginScreen
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.arun.tankerapp.ui.history.HistoryScreen
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.arun.tankerapp.ui.MainViewModel

@Composable
fun TankerApp(
    viewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarManager.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onSplashComplete = {
                        val startDestination = if (viewModel.isUserLoggedIn()) {
                            Screen.Home.route
                        } else {
                            Screen.Login.route
                        }
                        navController.navigate(startDestination) {
                             popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                CalendarScreen(
                    onNavigateToAbout = { 
                        navController.navigate(Screen.About.route)
                    },
                    onNavigateToReport = {
                        navController.navigate(Screen.Report.createRoute())
                    },
                    onNavigateToHistory = {
                        navController.navigate(Screen.History.route)
                    }
                )
            }
            
            composable(Screen.About.route) {
                AboutScreen()
            }

            composable(
                route = Screen.Report.route,
                arguments = listOf(
                    navArgument("startDate") { 
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("endDate") { 
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) {
                ReportPreviewScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onCycleClick = { cycle ->
                        val route = Screen.Report.createRoute(
                            startDate = cycle.startDate.toString(),
                            endDate = cycle.endDate.toString()
                        )
                        navController.navigate(route)
                    }
                )
            }
        }
    }
}

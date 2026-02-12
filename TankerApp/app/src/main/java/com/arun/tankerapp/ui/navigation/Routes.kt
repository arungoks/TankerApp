package com.arun.tankerapp.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object About : Screen("about")
    object Report : Screen("report?startDate={startDate}&endDate={endDate}") {
        fun createRoute(startDate: String? = null, endDate: String? = null): String {
            return if (startDate != null && endDate != null) {
                "report?startDate=$startDate&endDate=$endDate"
            } else {
                "report"
            }
        }
    }
    object History : Screen("history")
}

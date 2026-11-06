package com.docs.app.ui.navigation

/** Route table for the single-activity Compose navigation graph. */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object History : Screen("history")
    data object Settings : Screen("settings")
    data object Tool : Screen("tool/{toolId}") {
        fun create(toolId: String) = "tool/$toolId"
    }

    companion object {
        val TOP_LEVEL = setOf(Home.route, History.route, Settings.route)
    }
}

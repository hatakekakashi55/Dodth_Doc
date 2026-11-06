package com.docs.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.docs.app.data.ThemeRepository
import com.docs.app.ui.navigation.DocsNavHost
import com.docs.app.ui.navigation.Screen
import com.docs.app.ui.theme.DocsTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeRepository.init(this)
        enableEdgeToEdge()
        setContent {
            val mode by ThemeRepository.mode.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val dark = when (mode) {
                ThemeRepository.MODE_DARK -> true
                ThemeRepository.MODE_LIGHT -> false
                else -> systemDark
            }
            DocsTheme(darkTheme = dark) {
                DocsRoot(isDark = dark)
            }
        }
    }
}

@Composable
private fun DocsRoot(isDark: Boolean) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val toggleTheme = {
        // Quick-toggle mirrors the web header button; Settings offers System/Light/Dark.
        ThemeRepository.setMode(if (isDark) ThemeRepository.MODE_LIGHT else ThemeRepository.MODE_DARK)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentRoute in Screen.TOP_LEVEL) {
                DocsBottomBar(navController = navController, currentRoute = currentRoute)
            }
        }
    ) { padding ->
        DocsNavHost(
            navController = navController,
            modifier = Modifier.padding(padding),
            isDark = isDark,
            onToggleTheme = toggleTheme
        )
    }
}

@Composable
private fun DocsBottomBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        val items = listOf(
            Triple(Screen.Home.route, Icons.Filled.Home, "Tools"),
            Triple(Screen.History.route, Icons.Filled.History, "Recent"),
            Triple(Screen.Settings.route, Icons.Filled.Settings, "Settings")
        )
        items.forEach { (route, icon, label) ->
            NavigationBarItem(
                selected = currentRoute == route,
                onClick = {
                    navController.navigate(route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(Screen.Home.route) { saveState = true }
                    }
                },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}

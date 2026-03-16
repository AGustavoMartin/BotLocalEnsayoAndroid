package com.agustavomartin.botlocalensayoandroid.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.agustavomartin.botlocalensayoandroid.data.auth.AppSession
import com.agustavomartin.botlocalensayoandroid.ui.screens.HomeScreen
import com.agustavomartin.botlocalensayoandroid.ui.screens.LibraryScreen
import com.agustavomartin.botlocalensayoandroid.ui.screens.LoadingScreen
import com.agustavomartin.botlocalensayoandroid.ui.screens.LoginScreen
import com.agustavomartin.botlocalensayoandroid.ui.screens.MembersScreen
import com.agustavomartin.botlocalensayoandroid.ui.screens.PaymentsScreen

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit
)

@Composable
fun BotLocalEnsayoApp() {
    var session by remember { mutableStateOf<AppSession?>(null) }
    var loadingSession by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        session = AppContainer.authRepository.restoreSession()
        loadingSession = false
    }

    if (loadingSession) {
        AppBackground { LoadingScreen() }
        return
    }

    if (session == null) {
        AppBackground {
            LoginScreen(
                authRepository = AppContainer.authRepository,
                onAuthenticated = { authenticated -> session = authenticated }
            )
        }
        return
    }

    val navController = rememberNavController()
    val destinations = listOf(
        TopLevelDestination("home", "Inicio") { Icon(Icons.Outlined.SpaceDashboard, contentDescription = null) },
        TopLevelDestination("library", "Biblioteca") { Icon(Icons.Outlined.LibraryMusic, contentDescription = null) },
        TopLevelDestination("payments", "Pagos") { Icon(Icons.Outlined.Payments, contentDescription = null) },
        TopLevelDestination("members", "Miembros") { Icon(Icons.Outlined.People, contentDescription = null) }
    )

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                NavigationBar(containerColor = Color(0xCC111827)) {
                    destinations.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = destination.icon,
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(paddingValues)
            ) {
                composable("home") { HomeScreen(repository = AppContainer.repository) }
                composable("library") { LibraryScreen(repository = AppContainer.repository) }
                composable("payments") { PaymentsScreen(repository = AppContainer.repository) }
                composable("members") { MembersScreen(repository = AppContainer.repository) }
            }
        }
    }
}

@Composable
private fun AppBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF081018), Color(0xFF111827), Color(0xFF17212B))
                )
            )
    ) {
        content()
    }
}

package com.agustavomartin.botlocalensayoandroid.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.agustavomartin.botlocalensayoandroid.data.auth.AppMeta
import com.agustavomartin.botlocalensayoandroid.data.auth.AppSession
import com.agustavomartin.botlocalensayoandroid.notifications.AppFirebaseMessagingService
import com.agustavomartin.botlocalensayoandroid.ui.screens.HomeScreen
import com.agustavomartin.botlocalensayoandroid.ui.screens.LibraryScreen
import com.agustavomartin.botlocalensayoandroid.ui.screens.LoadingScreen
import com.agustavomartin.botlocalensayoandroid.ui.screens.LoginScreen
import com.agustavomartin.botlocalensayoandroid.ui.screens.MembersScreen
import com.agustavomartin.botlocalensayoandroid.ui.screens.PaymentsScreen
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit
)

@Composable
fun BotLocalEnsayoApp() {
    var session by remember { mutableStateOf<AppSession?>(null) }
    var loadingSession by remember { mutableStateOf(true) }
    var playbackUiState by remember { mutableStateOf(AppContainer.playbackManager.uiState.value) }
    var appMeta by remember { mutableStateOf<AppMeta?>(null) }
    var dismissedUpdateVersion by remember { mutableStateOf("") }
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            registerPushTokenSafely()
        }
    }

    LaunchedEffect(Unit) {
        AppContainer.playbackManager.uiState.collectLatest { state ->
            playbackUiState = state
        }
    }

    LaunchedEffect(playbackUiState.currentAudio, playbackUiState.isPlaying, playbackUiState.isBuffering) {
        while (playbackUiState.currentAudio != null) {
            AppContainer.playbackManager.syncProgress()
            delay(500)
        }
    }

    LaunchedEffect(Unit) {
        session = AppContainer.authRepository.restoreSession()
        loadingSession = false
    }

    LaunchedEffect(session?.phone) {
        if (session != null) {
            AppFirebaseMessagingService.ensureChannel(context)
            maybeRequestNotificationPermission(
                onNeedsPermission = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                onGranted = { registerPushTokenSafely() }
            )
            appMeta = AppContainer.authRepository.fetchAppMeta()
        }
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

    val currentVersion = remember { readInstalledVersionName(context) }
    val forceUpdate = appMeta?.minimumSupportedVersion?.takeIf { it.isNotBlank() }?.let { compareVersions(currentVersion, it) < 0 } == true
    val showUpdateDialog = appMeta?.currentVersion?.takeIf { it.isNotBlank() }?.let {
        compareVersions(currentVersion, it) < 0 && dismissedUpdateVersion != it
    } == true

    if (showUpdateDialog || forceUpdate) {
        AlertDialog(
            onDismissRequest = {
                if (!forceUpdate) {
                    dismissedUpdateVersion = appMeta?.currentVersion.orEmpty()
                }
            },
            title = { Text(if (forceUpdate) "Actualizacion requerida" else "Nueva version disponible") },
            text = {
                Text(
                    buildString {
                        append("Version instalada: ")
                        append(currentVersion)
                        append("\nVersion disponible: ")
                        append(appMeta?.currentVersion ?: "N/D")
                        if (!appMeta?.releaseNotes.isNullOrBlank()) {
                            append("\n\n")
                            append(appMeta?.releaseNotes)
                        }
                    }
                )
            },
            confirmButton = {
                OutlinedButton(onClick = {
                    val url = appMeta?.updateUrl.orEmpty()
                    if (url.isNotBlank()) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                }) {
                    Text("Actualizar")
                }
            },
            dismissButton = if (forceUpdate) null else ({
                OutlinedButton(onClick = {
                    dismissedUpdateVersion = appMeta?.currentVersion.orEmpty()
                }) {
                    Text("Mas tarde")
                }
            })
        )
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
                Column {
                    if (playbackUiState.currentAudio != null) {
                        PlaybackDock(
                            state = playbackUiState,
                            onTogglePlayback = { AppContainer.playbackManager.togglePlayback() },
                            onSeekBack = { AppContainer.playbackManager.seekBy(-10_000L) },
                            onSeekForward = { AppContainer.playbackManager.seekBy(10_000L) },
                            onSeekTo = { AppContainer.playbackManager.seekTo(it) },
                            onClose = { AppContainer.playbackManager.stopAndClear() }
                        )
                    }

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
private fun PlaybackDock(
    state: PlaybackUiState,
    onTogglePlayback: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onClose: () -> Unit
) {
    var sliderPosition by remember(state.positionMs, state.durationMs) {
        mutableFloatStateOf(
            if (state.durationMs > 0) state.positionMs.toFloat() else 0f
        )
    }

    Surface(
        color = Color.Transparent,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xF0192331), Color(0xF0101723))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF10283A), shape = MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.GraphicEq,
                        contentDescription = null,
                        tint = Color(0xFF6EE7D8)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.currentAudio?.title ?: "Sin reproduccion",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            !state.errorMessage.isNullOrBlank() -> state.errorMessage ?: "Error de reproduccion"
                            state.isBuffering -> "Cargando audio..."
                            state.isPlaying -> state.currentAudio?.dateLabel ?: "Reproduciendo"
                            else -> "Pausado"
                        },
                        color = if (state.errorMessage.isNullOrBlank()) Color(0xFFB8C1CC) else Color(0xFFFF9D9D),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(onClick = onClose) {
                    Icon(Icons.Outlined.Close, contentDescription = "Cerrar reproductor")
                }
            }

            androidx.compose.material3.Slider(
                value = sliderPosition,
                onValueChange = { sliderPosition = it },
                onValueChangeFinished = { onSeekTo(sliderPosition.toLong()) },
                valueRange = 0f..(state.durationMs.takeIf { it > 0 }?.toFloat() ?: 1f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(formatMs(state.positionMs), color = Color(0xFFB8C1CC))
                Text(formatMs(state.durationMs), color = Color(0xFFB8C1CC))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onSeekBack) {
                    Icon(Icons.Outlined.Replay10, contentDescription = "Retroceder 10 segundos")
                }
                FilledIconButton(onClick = onTogglePlayback, modifier = Modifier.size(64.dp)) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Outlined.PauseCircle else Icons.Outlined.PlayCircle,
                        contentDescription = if (state.isPlaying) "Pausar" else "Reproducir",
                        modifier = Modifier.size(34.dp)
                    )
                }
                OutlinedButton(onClick = onSeekForward) {
                    Icon(Icons.Outlined.Forward10, contentDescription = "Avanzar 10 segundos")
                }
            }
        }
    }
}

private fun maybeRequestNotificationPermission(
    onNeedsPermission: () -> Unit,
    onGranted: () -> Unit
) {
    val context = AppContainer.context
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        onGranted()
        return
    }

    val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED

    if (granted) onGranted() else onNeedsPermission()
}

private fun registerPushTokenSafely() {
    runCatching {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (token.isNullOrBlank()) return@addOnSuccessListener
                CoroutineScope(Dispatchers.IO).launch {
                    AppContainer.authRepository.registerPushToken(token, Build.MODEL)
                }
            }
    }
}

private fun readInstalledVersionName(context: android.content.Context): String {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    return info.versionName ?: "0.0.0"
}

private fun compareVersions(left: String, right: String): Int {
    val leftParts = left.split('.').mapNotNull { it.toIntOrNull() }
    val rightParts = right.split('.').mapNotNull { it.toIntOrNull() }
    val size = maxOf(leftParts.size, rightParts.size)
    for (index in 0 until size) {
        val l = leftParts.getOrElse(index) { 0 }
        val r = rightParts.getOrElse(index) { 0 }
        if (l != r) return l.compareTo(r)
    }
    return 0
}

private fun formatMs(value: Long): String {
    if (value <= 0L) return "0:00"
    val totalSeconds = value / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
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



package com.agustavomartin.botlocalensayoandroid.ui.screens

import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.agustavomartin.botlocalensayoandroid.BuildConfig
import com.agustavomartin.botlocalensayoandroid.data.AudioItem
import com.agustavomartin.botlocalensayoandroid.data.BotRepository
import com.agustavomartin.botlocalensayoandroid.ui.AppContainer

@Composable
fun LibraryScreen(repository: BotRepository) {
    val items = produceState(initialValue = emptyList(), producer = { value = repository.getLibrary() }).value
    val session = produceState(initialValue = null as String?, producer = {
        value = AppContainer.authRepository.restoreSession()?.accessToken
    }).value
    val context = LocalContext.current
    var selectedAudio by remember { mutableStateOf<AudioItem?>(null) }

    val player = remember(session) {
        val requestProperties = buildMap {
            if (!session.isNullOrBlank()) {
                put("Authorization", "Bearer $session")
            }
        }
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(requestProperties)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    DisposableEffect(selectedAudio, session) {
        val audio = selectedAudio
        if (audio?.driveUrl.isNullOrBlank()) {
            onDispose { }
        } else {
            val uri = BuildConfig.API_BASE_URL.trim().trimEnd('/') + audio!!.driveUrl
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            player.playWhenReady = true
            onDispose { }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                eyebrow = "BIBLIOTECA",
                title = "Audios",
                subtitle = "Ensayos, riffs y canciones sincronizados desde la base remota."
            )
        }
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                InfoCard(
                    title = "Reproductor",
                    value = selectedAudio?.title ?: "Selecciona un audio",
                    supporting = selectedAudio?.dateLabel ?: "Pulsa en Reproducir para empezar a escuchar."
                )
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = true
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                        }
                    },
                    update = { view -> view.player = player },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        items(items) { item ->
            AudioRow(
                item = item,
                onPlay = { selectedAudio = item }
            )
        }
    }
}

@Composable
private fun AudioRow(item: AudioItem, onPlay: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
    ) {
        DataRow(
            primary = item.title,
            secondary = item.dateLabel,
            trailing = "id ${item.id}"
        )
        Button(
            onClick = onPlay,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0E948B),
                contentColor = Color(0xFFF6F3EC)
            )
        ) {
            Text("Reproducir", style = MaterialTheme.typography.titleMedium)
        }
    }
}

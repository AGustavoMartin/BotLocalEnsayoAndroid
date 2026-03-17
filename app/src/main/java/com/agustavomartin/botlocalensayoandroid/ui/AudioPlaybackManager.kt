package com.agustavomartin.botlocalensayoandroid.ui

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.agustavomartin.botlocalensayoandroid.BuildConfig
import com.agustavomartin.botlocalensayoandroid.data.AudioItem
import com.agustavomartin.botlocalensayoandroid.data.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking


data class PlaybackUiState(
    val currentAudio: AudioItem? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val errorMessage: String? = null
)

class AudioPlaybackManager(
    context: Context,
    private val authRepository: AuthRepository
) {
    private val appContext = context.applicationContext

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    val player: ExoPlayer = ExoPlayer.Builder(appContext).build().also { exoPlayer ->
        exoPlayer.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    publishState(currentAudio = _uiState.value.currentAudio, errorMessage = null)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    publishState(currentAudio = _uiState.value.currentAudio, errorMessage = null)
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    publishState(currentAudio = _uiState.value.currentAudio, errorMessage = null)
                }

                override fun onPlayerError(error: PlaybackException) {
                    publishState(
                        currentAudio = _uiState.value.currentAudio,
                        errorMessage = error.message ?: "No se ha podido reproducir el audio"
                    )
                }
            }
        )
    }

    suspend fun play(audio: AudioItem) {
        val mediaSource = ProgressiveMediaSource.Factory(buildDataSourceFactory())
            .createMediaSource(MediaItem.fromUri(apiBaseUrl() + (audio.driveUrl ?: "")))

        if (_uiState.value.currentAudio != audio) {
            player.setMediaSource(mediaSource)
            player.prepare()
        }

        player.playWhenReady = true
        publishState(currentAudio = audio, errorMessage = null)
    }

    fun togglePlayback() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
        publishState(currentAudio = _uiState.value.currentAudio, errorMessage = null)
    }

    fun seekBy(deltaMs: Long) {
        val target = (player.currentPosition + deltaMs).coerceIn(0L, player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE)
        player.seekTo(target)
        publishState(currentAudio = _uiState.value.currentAudio, errorMessage = null)
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
        publishState(currentAudio = _uiState.value.currentAudio, errorMessage = null)
    }

    fun stopAndClear() {
        player.stop()
        player.clearMediaItems()
        _uiState.value = PlaybackUiState()
    }

    fun syncProgress() {
        if (_uiState.value.currentAudio != null) {
            publishState(currentAudio = _uiState.value.currentAudio, errorMessage = _uiState.value.errorMessage)
        }
    }

    private fun buildDataSourceFactory(): DefaultHttpDataSource.Factory {
        val requestProperties = buildMap {
            val session = runBlocking { authRepository.restoreSession() }
            if (!session?.accessToken.isNullOrBlank()) {
                put("Authorization", "Bearer ${session!!.accessToken}")
            }
        }

        return DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(requestProperties)
    }

    private fun publishState(currentAudio: AudioItem?, errorMessage: String?) {
        val duration = player.duration.takeIf { it > 0 } ?: 0L
        val position = player.currentPosition.coerceAtLeast(0L).coerceAtMost(duration.takeIf { it > 0 } ?: Long.MAX_VALUE)
        _uiState.value = PlaybackUiState(
            currentAudio = currentAudio,
            isPlaying = player.isPlaying,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            durationMs = duration,
            positionMs = position,
            errorMessage = errorMessage
        )
    }

    private fun apiBaseUrl(): String = BuildConfig.API_BASE_URL.trim().trimEnd('/')
}

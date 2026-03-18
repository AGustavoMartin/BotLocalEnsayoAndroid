package com.agustavomartin.botlocalensayoandroid.ui

import android.content.Context
import android.content.Intent
import com.agustavomartin.botlocalensayoandroid.MainActivityIntentKeys
import com.agustavomartin.botlocalensayoandroid.BuildConfig
import com.agustavomartin.botlocalensayoandroid.data.BotRepository
import com.agustavomartin.botlocalensayoandroid.data.FakeBotRepository
import com.agustavomartin.botlocalensayoandroid.data.RealBotRepository
import com.agustavomartin.botlocalensayoandroid.data.auth.AppAuthRepository
import com.agustavomartin.botlocalensayoandroid.data.auth.AuthRepository
import com.agustavomartin.botlocalensayoandroid.data.auth.SessionStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

data class PendingPlaybackRequest(
    val screen: String? = null,
    val itemId: Int? = null,
    val itemType: String? = null
)

object AppContainer {
    private lateinit var appContext: Context
    private lateinit var sessionStoreInstance: SessionStore
    private lateinit var authRepositoryInstance: AuthRepository
    private lateinit var botRepositoryInstance: BotRepository
    private lateinit var playbackManagerInstance: AudioPlaybackManager
    private val sessionExpiredEventsInternal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val pendingPlaybackRequestInternal = MutableStateFlow<PendingPlaybackRequest?>(null)

    fun initialize(context: Context) {
        if (!::sessionStoreInstance.isInitialized) {
            appContext = context.applicationContext
            sessionStoreInstance = SessionStore(appContext)
            authRepositoryInstance = AppAuthRepository(sessionStoreInstance)
            botRepositoryInstance = if (BuildConfig.API_BASE_URL.isBlank()) {
                FakeBotRepository()
            } else {
                RealBotRepository(
                    authRepository = authRepositoryInstance,
                    onSessionExpired = { sessionExpiredEventsInternal.tryEmit(Unit) }
                )
            }
            playbackManagerInstance = AudioPlaybackManager(appContext, authRepositoryInstance)
        }
    }

    fun handleLaunchIntent(intent: Intent?) {
        val source = intent?.getStringExtra(MainActivityIntentKeys.EXTRA_SCREEN)
        val itemId = intent?.getIntExtra(MainActivityIntentKeys.EXTRA_ITEM_ID, -1)?.takeIf { it >= 0 }
        val itemType = intent?.getStringExtra(MainActivityIntentKeys.EXTRA_ITEM_TYPE)?.takeIf { it.isNotBlank() }
        if (source != null || itemId != null || itemType != null) {
            pendingPlaybackRequestInternal.value = PendingPlaybackRequest(
                screen = source,
                itemId = itemId,
                itemType = itemType
            )
        }
    }

    fun clearPendingPlaybackRequest() {
        pendingPlaybackRequestInternal.value = null
    }

    val context: Context
        get() = appContext

    val repository: BotRepository
        get() = botRepositoryInstance

    val authRepository: AuthRepository
        get() = authRepositoryInstance

    val playbackManager: AudioPlaybackManager
        get() = playbackManagerInstance

    val sessionExpiredEvents: SharedFlow<Unit>
        get() = sessionExpiredEventsInternal.asSharedFlow()

    val pendingPlaybackRequest: StateFlow<PendingPlaybackRequest?>
        get() = pendingPlaybackRequestInternal.asStateFlow()
}

package com.agustavomartin.botlocalensayoandroid.ui

import android.content.Context
import com.agustavomartin.botlocalensayoandroid.BuildConfig
import com.agustavomartin.botlocalensayoandroid.data.BotRepository
import com.agustavomartin.botlocalensayoandroid.data.FakeBotRepository
import com.agustavomartin.botlocalensayoandroid.data.RealBotRepository
import com.agustavomartin.botlocalensayoandroid.data.auth.AppAuthRepository
import com.agustavomartin.botlocalensayoandroid.data.auth.AuthRepository
import com.agustavomartin.botlocalensayoandroid.data.auth.SessionStore

object AppContainer {
    private lateinit var appContext: Context
    private lateinit var sessionStoreInstance: SessionStore
    private lateinit var authRepositoryInstance: AuthRepository
    private lateinit var botRepositoryInstance: BotRepository
    private lateinit var playbackManagerInstance: AudioPlaybackManager

    fun initialize(context: Context) {
        if (!::sessionStoreInstance.isInitialized) {
            appContext = context.applicationContext
            sessionStoreInstance = SessionStore(appContext)
            authRepositoryInstance = AppAuthRepository(sessionStoreInstance)
            botRepositoryInstance = if (BuildConfig.API_BASE_URL.isBlank()) {
                FakeBotRepository()
            } else {
                RealBotRepository(authRepositoryInstance)
            }
            playbackManagerInstance = AudioPlaybackManager(appContext, authRepositoryInstance)
        }
    }

    val context: Context
        get() = appContext

    val repository: BotRepository
        get() = botRepositoryInstance

    val authRepository: AuthRepository
        get() = authRepositoryInstance

    val playbackManager: AudioPlaybackManager
        get() = playbackManagerInstance
}

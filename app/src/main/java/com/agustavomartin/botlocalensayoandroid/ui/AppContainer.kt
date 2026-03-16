package com.agustavomartin.botlocalensayoandroid.ui

import android.content.Context
import com.agustavomartin.botlocalensayoandroid.data.BotRepository
import com.agustavomartin.botlocalensayoandroid.data.FakeBotRepository
import com.agustavomartin.botlocalensayoandroid.data.auth.AppAuthRepository
import com.agustavomartin.botlocalensayoandroid.data.auth.AuthRepository
import com.agustavomartin.botlocalensayoandroid.data.auth.SessionStore

object AppContainer {
    private lateinit var sessionStoreInstance: SessionStore
    private lateinit var authRepositoryInstance: AuthRepository
    private val botRepositoryInstance: BotRepository = FakeBotRepository()

    fun initialize(context: Context) {
        if (!::sessionStoreInstance.isInitialized) {
            sessionStoreInstance = SessionStore(context.applicationContext)
            authRepositoryInstance = AppAuthRepository(sessionStoreInstance)
        }
    }

    val repository: BotRepository
        get() = botRepositoryInstance

    val authRepository: AuthRepository
        get() = authRepositoryInstance
}

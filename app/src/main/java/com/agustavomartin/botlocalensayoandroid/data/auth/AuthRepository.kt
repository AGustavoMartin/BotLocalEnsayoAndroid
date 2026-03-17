package com.agustavomartin.botlocalensayoandroid.data.auth

interface AuthRepository {
    suspend fun restoreSession(): AppSession?
    suspend fun start(phone: String): AuthStartResult
    suspend fun registerPin(phone: String, pin: String, deviceLabel: String): AppSession
    suspend fun login(phone: String, pin: String, deviceLabel: String): AppSession
    suspend fun registerPushToken(pushToken: String, deviceLabel: String): Boolean
    suspend fun fetchAppMeta(): AppMeta?
    suspend fun logout()
}

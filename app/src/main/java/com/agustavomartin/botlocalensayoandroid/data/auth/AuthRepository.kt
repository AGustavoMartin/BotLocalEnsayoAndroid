package com.agustavomartin.botlocalensayoandroid.data.auth

interface AuthRepository {
    suspend fun restoreSession(): AppSession?
    suspend fun start(phone: String): AuthStartResult
    suspend fun registerPin(phone: String, pin: String, deviceLabel: String): AppSession
    suspend fun login(phone: String, pin: String, deviceLabel: String): AppSession
    suspend fun logout()
}

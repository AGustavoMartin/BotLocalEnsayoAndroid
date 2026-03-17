package com.agustavomartin.botlocalensayoandroid.data.auth

data class AppSession(
    val phone: String,
    val memberName: String,
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresAt: Long,
    val refreshExpiresAt: Long
)

data class AppMeta(
    val currentVersion: String,
    val minimumSupportedVersion: String,
    val updateUrl: String,
    val releaseNotes: String
)

sealed interface AuthStartResult {
    data class CreatePin(val phone: String, val memberName: String) : AuthStartResult
    data class EnterPin(val phone: String, val memberName: String) : AuthStartResult
    data object NotAllowed : AuthStartResult
}

package com.agustavomartin.botlocalensayoandroid.data.auth

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException

private val Context.appSessionDataStore by preferencesDataStore(name = "app_session")

class SessionStore(private val context: Context) {
    private object Keys {
        val phone = stringPreferencesKey("phone")
        val memberName = stringPreferencesKey("member_name")
        val accessToken = stringPreferencesKey("access_token")
        val refreshToken = stringPreferencesKey("refresh_token")
        val accessExpiresAt = longPreferencesKey("access_expires_at")
        val refreshExpiresAt = longPreferencesKey("refresh_expires_at")
    }

    suspend fun read(): AppSession? {
        val preferences = context.appSessionDataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }
            .first()

        val phone = preferences[Keys.phone] ?: return null
        val memberName = preferences[Keys.memberName] ?: return null
        val accessToken = preferences[Keys.accessToken] ?: return null
        val refreshToken = preferences[Keys.refreshToken] ?: return null
        val accessExpiresAt = preferences[Keys.accessExpiresAt] ?: return null
        val refreshExpiresAt = preferences[Keys.refreshExpiresAt] ?: return null

        return AppSession(
            phone = phone,
            memberName = memberName,
            accessToken = accessToken,
            refreshToken = refreshToken,
            accessExpiresAt = accessExpiresAt,
            refreshExpiresAt = refreshExpiresAt
        )
    }

    suspend fun save(session: AppSession) {
        context.appSessionDataStore.edit { preferences ->
            preferences[Keys.phone] = session.phone
            preferences[Keys.memberName] = session.memberName
            preferences[Keys.accessToken] = session.accessToken
            preferences[Keys.refreshToken] = session.refreshToken
            preferences[Keys.accessExpiresAt] = session.accessExpiresAt
            preferences[Keys.refreshExpiresAt] = session.refreshExpiresAt
        }
    }

    suspend fun clear() {
        context.appSessionDataStore.edit { preferences -> preferences.clear() }
    }
}


package com.agustavomartin.botlocalensayoandroid.data.auth

import com.agustavomartin.botlocalensayoandroid.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

class AppAuthRepository(private val sessionStore: SessionStore) : AuthRepository {
    private val fakePins = ConcurrentHashMap<String, String>()
    private val fakeNames = mapOf(
        "34650529835" to "rafael gordillo",
        "34671101191" to "Rodrigo",
        "34678036031" to "Gustavo",
        "34690379248" to "Antonio Espejo"
    )

    override suspend fun restoreSession(): AppSession? {
        val saved = sessionStore.read() ?: return null
        val now = System.currentTimeMillis()

        if (saved.refreshExpiresAt <= now) {
            sessionStore.clear()
            return null
        }

        if (saved.accessExpiresAt > now) {
            return saved
        }

        if (apiBaseUrl().isBlank()) {
            return saved
        }

        return try {
            val response = postJson(
                path = "/api/app-auth/refresh",
                body = JSONObject().put("refreshToken", saved.refreshToken)
            )
            if (!response.optBoolean("ok")) {
                sessionStore.clear()
                null
            } else {
                val session = parseSession(response.getJSONObject("session"))
                sessionStore.save(session)
                session
            }
        } catch (_: Exception) {
            sessionStore.clear()
            null
        }
    }

    override suspend fun start(phone: String): AuthStartResult {
        val normalizedPhone = normalizePhone(phone)
        if (normalizedPhone.isBlank()) return AuthStartResult.NotAllowed

        if (apiBaseUrl().isBlank()) {
            val name = fakeNames[normalizedPhone] ?: return AuthStartResult.NotAllowed
            return if (fakePins.containsKey(normalizedPhone)) {
                AuthStartResult.EnterPin(normalizedPhone, name)
            } else {
                AuthStartResult.CreatePin(normalizedPhone, name)
            }
        }

        val response = postJson(
            path = "/api/app-auth/start",
            body = JSONObject().put("phone", normalizedPhone)
        )

        if (!response.optBoolean("ok")) return AuthStartResult.NotAllowed

        val memberName = response.getString("memberName")
        val status = response.getString("status")
        return if (status == "create_pin") {
            AuthStartResult.CreatePin(normalizedPhone, memberName)
        } else {
            AuthStartResult.EnterPin(normalizedPhone, memberName)
        }
    }

    override suspend fun registerPin(phone: String, pin: String, deviceLabel: String): AppSession {
        val normalizedPhone = normalizePhone(phone)
        if (apiBaseUrl().isBlank()) {
            val memberName = fakeNames[normalizedPhone] ?: throw IllegalStateException("Telefono no autorizado")
            fakePins[normalizedPhone] = pin
            val session = fakeSession(normalizedPhone, memberName)
            sessionStore.save(session)
            return session
        }

        val response = postJson(
            path = "/api/app-auth/register-pin",
            body = JSONObject()
                .put("phone", normalizedPhone)
                .put("pin", pin)
                .put("deviceLabel", deviceLabel)
        )

        if (!response.optBoolean("ok")) {
            throw IllegalStateException(response.optString("error", "REGISTER_PIN_FAILED"))
        }

        val session = parseSession(response.getJSONObject("session"))
        sessionStore.save(session)
        return session
    }

    override suspend fun login(phone: String, pin: String, deviceLabel: String): AppSession {
        val normalizedPhone = normalizePhone(phone)
        if (apiBaseUrl().isBlank()) {
            val expectedPin = fakePins[normalizedPhone] ?: throw IllegalStateException("PIN no registrado")
            if (expectedPin != pin) throw IllegalStateException("PIN incorrecto")
            val memberName = fakeNames[normalizedPhone] ?: throw IllegalStateException("Telefono no autorizado")
            val session = fakeSession(normalizedPhone, memberName)
            sessionStore.save(session)
            return session
        }

        val response = postJson(
            path = "/api/app-auth/login",
            body = JSONObject()
                .put("phone", normalizedPhone)
                .put("pin", pin)
                .put("deviceLabel", deviceLabel)
        )

        if (!response.optBoolean("ok")) {
            throw IllegalStateException(response.optString("error", "LOGIN_FAILED"))
        }

        val session = parseSession(response.getJSONObject("session"))
        sessionStore.save(session)
        return session
    }

    override suspend fun registerPushToken(pushToken: String, deviceLabel: String): Boolean {
        val session = restoreSession() ?: return false
        if (apiBaseUrl().isBlank()) return false

        return runCatching {
            val response = postJsonAuthorized(
                path = "/api/app/push/register",
                accessToken = session.accessToken,
                body = JSONObject()
                    .put("pushToken", pushToken)
                    .put("deviceLabel", deviceLabel)
            )
            response.optBoolean("ok")
        }.getOrDefault(false)
    }

    override suspend fun fetchAppMeta(): AppMeta? {
        if (apiBaseUrl().isBlank()) return null

        return runCatching {
            val response = getJson("/api/app/meta")
            AppMeta(
                currentVersion = response.optString("currentVersion"),
                minimumSupportedVersion = response.optString("minimumSupportedVersion"),
                updateUrl = response.optString("updateUrl"),
                releaseNotes = response.optString("releaseNotes")
            )
        }.getOrNull()
    }

    override suspend fun logout() {
        val current = sessionStore.read()
        if (current != null && apiBaseUrl().isNotBlank()) {
            runCatching {
                postJson(
                    path = "/api/app-auth/logout",
                    body = JSONObject().put("refreshToken", current.refreshToken)
                )
            }
        }
        sessionStore.clear()
    }

    private fun fakeSession(phone: String, memberName: String): AppSession {
        val now = System.currentTimeMillis()
        return AppSession(
            phone = phone,
            memberName = memberName,
            accessToken = "fake-access-$phone",
            refreshToken = "fake-refresh-$phone",
            accessExpiresAt = now + 12 * 60 * 60 * 1000,
            refreshExpiresAt = now + 90L * 24L * 60L * 60L * 1000L
        )
    }

    private fun parseSession(json: JSONObject): AppSession = AppSession(
        phone = json.getString("phone"),
        memberName = json.getString("memberName"),
        accessToken = json.getString("accessToken"),
        refreshToken = json.getString("refreshToken"),
        accessExpiresAt = json.getLong("accessExpiresAt"),
        refreshExpiresAt = json.getLong("refreshExpiresAt")
    )

    private suspend fun getJson(path: String): JSONObject = withContext(Dispatchers.IO) {
        val url = URL(apiBaseUrl() + path)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 10000
        }

        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (text.isBlank()) JSONObject() else JSONObject(text)
    }

    private suspend fun postJson(path: String, body: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val url = URL(apiBaseUrl() + path)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("Content-Type", "application/json")
        }

        connection.outputStream.use { output ->
            output.write(body.toString().toByteArray())
        }

        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (text.isBlank()) {
            JSONObject().put("ok", false).put("error", "EMPTY_RESPONSE")
        } else {
            JSONObject(text)
        }
    }

    private suspend fun postJsonAuthorized(path: String, accessToken: String, body: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val url = URL(apiBaseUrl() + path)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $accessToken")
        }

        connection.outputStream.use { output ->
            output.write(body.toString().toByteArray())
        }

        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (text.isBlank()) {
            JSONObject().put("ok", false).put("error", "EMPTY_RESPONSE")
        } else {
            JSONObject(text)
        }
    }

    private fun apiBaseUrl(): String = BuildConfig.API_BASE_URL.trim().trimEnd('/')

    private fun normalizePhone(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) return ""
        return when {
            digits.length == 9 -> "34$digits"
            digits.startsWith("0034") -> digits.drop(2)
            else -> digits
        }
    }
}

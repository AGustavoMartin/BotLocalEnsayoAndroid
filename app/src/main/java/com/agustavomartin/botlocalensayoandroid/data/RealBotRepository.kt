package com.agustavomartin.botlocalensayoandroid.data

import com.agustavomartin.botlocalensayoandroid.BuildConfig
import com.agustavomartin.botlocalensayoandroid.data.auth.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class RealBotRepository(
    private val authRepository: AuthRepository
) : BotRepository {

    override suspend fun getDashboard(): DashboardSnapshot {
        val response = authorizedGet("/api/app/dashboard")
        val data = response.getJSONObject("data")
        val recentAudios = data.getJSONArray("recentAudios").toAudioItems()

        return DashboardSnapshot(
            totalAudios = data.optInt("totalAudios"),
            totalEnsayos = data.optInt("totalEnsayos"),
            totalRiffs = data.optInt("totalRiffs"),
            totalCanciones = data.optInt("totalCanciones"),
            activeMembers = data.optInt("activeMembers"),
            lastPayment = data.optJSONObject("lastPayment")?.let {
                PaymentSummary(
                    monthLabel = it.optString("monthLabel"),
                    payerName = it.optString("payerName"),
                    amountLabel = it.optString("amountLabel"),
                    confirmed = it.optBoolean("confirmed")
                )
            },
            nextPayerName = data.optString("nextPayerName", "N/D"),
            nextPayerMonthLabel = data.optString("nextPayerMonthLabel", "N/D"),
            recentAudios = recentAudios
        )
    }

    override suspend fun getLibrary(): List<AudioItem> {
        val response = authorizedGet("/api/app/library")
        return response.getJSONArray("data").toAudioItems()
    }

    override suspend fun getPayments(): List<PaymentSummary> {
        val response = authorizedGet("/api/app/payments")
        val array = response.getJSONArray("data")
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    PaymentSummary(
                        monthLabel = item.optString("monthLabel"),
                        payerName = item.optString("payerName"),
                        amountLabel = item.optString("amountLabel"),
                        confirmed = item.optBoolean("confirmed")
                    )
                )
            }
        }
    }

    override suspend fun getMembers(): List<MemberSummary> {
        val response = authorizedGet("/api/app/members")
        val array = response.getJSONArray("data")
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    MemberSummary(
                        name = item.optString("name"),
                        phone = item.optString("phone"),
                        lastSeenLabel = item.optString("lastSeenLabel")
                    )
                )
            }
        }
    }

    private suspend fun authorizedGet(path: String): JSONObject = withContext(Dispatchers.IO) {
        val session = authRepository.restoreSession() ?: throw IllegalStateException("Sesion no disponible")
        val url = URL(apiBaseUrl() + path)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("Authorization", "Bearer ${session.accessToken}")
        }

        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (text.isBlank()) throw IllegalStateException("Respuesta vacia del servidor")

        val json = JSONObject(text)
        if (!json.optBoolean("ok", false)) {
            throw IllegalStateException(json.optString("error", "SERVER_ERROR"))
        }
        json
    }

    private fun JSONArray.toAudioItems(): List<AudioItem> = buildList {
        for (index in 0 until length()) {
            val item = getJSONObject(index)
            add(
                AudioItem(
                    id = item.optInt("id"),
                    type = item.optString("type").toAudioType(),
                    title = item.optString("title"),
                    dateLabel = item.optString("dateLabel"),
                    driveUrl = item.optString("driveUrl").ifBlank { null }
                )
            )
        }
    }

    private fun String.toAudioType(): AudioType = when (lowercase()) {
        "ensayo" -> AudioType.ENSAYO
        "riff" -> AudioType.RIFF
        else -> AudioType.CANCION
    }

    private fun apiBaseUrl(): String = BuildConfig.API_BASE_URL.trim().trimEnd('/')
}

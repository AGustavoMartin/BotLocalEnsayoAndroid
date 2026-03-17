package com.agustavomartin.botlocalensayoandroid.data

data class AudioItem(
    val id: Int,
    val type: AudioType,
    val title: String,
    val dateLabel: String,
    val rawDateKey: String? = null,
    val driveUrl: String? = null
)

enum class AudioType { ENSAYO, RIFF, CANCION }

data class PaymentSummary(
    val monthLabel: String,
    val payerName: String,
    val amountLabel: String,
    val confirmed: Boolean
)

data class MemberSummary(
    val name: String,
    val phone: String,
    val lastSeenLabel: String
)

data class DashboardSnapshot(
    val totalAudios: Int,
    val totalEnsayos: Int,
    val totalRiffs: Int,
    val totalCanciones: Int,
    val activeMembers: Int,
    val lastPayment: PaymentSummary?,
    val nextPayerName: String,
    val nextPayerMonthLabel: String,
    val recentAudios: List<AudioItem>
)

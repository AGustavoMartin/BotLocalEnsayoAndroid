package com.agustavomartin.botlocalensayoandroid.data

class FakeBotRepository : BotRepository {
    private val audios = listOf(
        AudioItem(15, AudioType.CANCION, "Cancion_02_20260306", "06/03/2026", rawDateKey = "20260306"),
        AudioItem(14, AudioType.ENSAYO, "Ensayo_2_20260306", "06/03/2026", rawDateKey = "20260306"),
        AudioItem(13, AudioType.CANCION, "Cancion_01_20260306", "06/03/2026", rawDateKey = "20260306"),
        AudioItem(5, AudioType.RIFF, "Riff_10_20260306", "06/03/2026", rawDateKey = "20260306")
    )

    override suspend fun getDashboard(): DashboardSnapshot = DashboardSnapshot(
        totalAudios = 20,
        totalEnsayos = 11,
        totalRiffs = 5,
        totalCanciones = 4,
        activeMembers = 4,
        lastPayment = PaymentSummary("Marzo 2026", "rafael gordillo", "80 EUR", confirmed = true),
        nextPayerName = "Rodrigo",
        nextPayerMonthLabel = "Abril 2026",
        recentAudios = audios
    )

    override suspend fun getLibrary(): List<AudioItem> = audios

    override suspend fun getPayments(): List<PaymentSummary> = listOf(
        PaymentSummary("Marzo 2026", "rafael gordillo", "80 EUR", true),
        PaymentSummary("Febrero 2026", "Antonio Espejo", "80 EUR", true),
        PaymentSummary("Enero 2026", "Gustavo", "80 EUR", true)
    )

    override suspend fun getMembers(): List<MemberSummary> = listOf(
        MemberSummary("rafael gordillo", "34650529835", "hoy a las 17:07"),
        MemberSummary("Rodrigo", "34671101191", "hace 4 dias"),
        MemberSummary("Gustavo", "34678036031", "hoy a las 21:58"),
        MemberSummary("Antonio Espejo", "34690379248", "hoy a las 17:34")
    )
}

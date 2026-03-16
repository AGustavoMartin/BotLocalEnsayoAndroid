package com.agustavomartin.botlocalensayoandroid.data

interface BotRepository {
    suspend fun getDashboard(): DashboardSnapshot
    suspend fun getLibrary(): List<AudioItem>
    suspend fun getPayments(): List<PaymentSummary>
    suspend fun getMembers(): List<MemberSummary>
}

package com.agustavomartin.botlocalensayoandroid.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agustavomartin.botlocalensayoandroid.data.BotRepository
import com.agustavomartin.botlocalensayoandroid.data.DashboardSnapshot

@Composable
fun HomeScreen(repository: BotRepository) {
    val snapshot = produceState<DashboardSnapshot?>(initialValue = null) {
        value = repository.getDashboard()
    }.value

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ScreenHeader(
                eyebrow = "BOT",
                title = "Dashboard",
                subtitle = "Acceso rapido a audios, pagos y miembros del local."
            )
        }

        if (snapshot == null) {
            item { LoadingPanel("Cargando dashboard...") }
            return@LazyColumn
        }

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                InfoCard("Biblioteca", snapshot.totalAudios.toString(), "${snapshot.totalEnsayos} ensayos, ${snapshot.totalRiffs} riffs y ${snapshot.totalCanciones} canciones.")
                InfoCard("Miembros", snapshot.activeMembers.toString(), "${snapshot.activeMembers} miembros activos sincronizados.")
                snapshot.lastPayment?.let {
                    InfoCard("Ultimo pago", it.monthLabel, "${it.payerName} - ${it.amountLabel}")
                }
                InfoCard("Proximo pagador", snapshot.nextPayerName, snapshot.nextPayerMonthLabel)
            }
        }
        item {
            ScreenHeader(
                eyebrow = "ACTIVIDAD",
                title = "Reciente",
                subtitle = "Ultimos audios visibles para reproducir o abrir luego en biblioteca."
            )
        }
        items(snapshot.recentAudios) { item ->
            DataRow(
                primary = item.title,
                secondary = item.dateLabel,
                trailing = item.type.name.lowercase()
            )
        }
    }
}

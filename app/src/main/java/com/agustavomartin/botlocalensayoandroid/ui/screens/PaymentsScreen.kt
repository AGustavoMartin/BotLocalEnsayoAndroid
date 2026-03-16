package com.agustavomartin.botlocalensayoandroid.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agustavomartin.botlocalensayoandroid.data.BotRepository

@Composable
fun PaymentsScreen(repository: BotRepository) {
    val items = produceState(initialValue = emptyList(), producer = { value = repository.getPayments() }).value

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                eyebrow = "PAGOS",
                title = "Historial de pagos",
                subtitle = "Lectura remota de los pagos sincronizados desde la Raspberry."
            )
        }
        items(items) { item ->
            DataRow(
                primary = item.monthLabel,
                secondary = "${item.payerName} · ${item.amountLabel}",
                trailing = if (item.confirmed) "ok" else "pendiente"
            )
        }
    }
}

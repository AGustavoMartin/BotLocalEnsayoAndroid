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
import com.agustavomartin.botlocalensayoandroid.data.MemberSummary

@Composable
fun MembersScreen(repository: BotRepository) {
    val items = produceState<List<MemberSummary>?>(initialValue = null, producer = {
        value = repository.getMembers()
    }).value

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                eyebrow = "MIEMBROS",
                title = "Actividad",
                subtitle = "Resumen rapido de miembros, telefono y fecha de actualizacion sincronizada."
            )
        }

        if (items == null) {
            item { LoadingPanel("Cargando miembros...") }
            return@LazyColumn
        }

        items(items) { item ->
            DataRow(
                primary = item.name,
                secondary = item.lastSeenLabel,
                trailing = item.phone
            )
        }
    }
}

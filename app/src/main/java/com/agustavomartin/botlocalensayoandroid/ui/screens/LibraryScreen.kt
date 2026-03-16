package com.agustavomartin.botlocalensayoandroid.ui.screens

import androidx.compose.foundation.layout.Arrangement
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

@Composable
fun LibraryScreen(repository: BotRepository) {
    val items = produceState(initialValue = emptyList(), producer = { value = repository.getLibrary() }).value

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                eyebrow = "BIBLIOTECA",
                title = "Audios",
                subtitle = "Ensayos, riffs y canciones sincronizados desde la base remota."
            )
        }
        items(items) { item ->
            DataRow(
                primary = item.title,
                secondary = item.dateLabel,
                trailing = "id ${item.id}"
            )
        }
    }
}

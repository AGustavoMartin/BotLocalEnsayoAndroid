package com.agustavomartin.botlocalensayoandroid.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.agustavomartin.botlocalensayoandroid.data.AudioItem
import com.agustavomartin.botlocalensayoandroid.data.AudioType
import com.agustavomartin.botlocalensayoandroid.data.BotRepository
import com.agustavomartin.botlocalensayoandroid.data.formatDateForChip
import com.agustavomartin.botlocalensayoandroid.data.parseFlexibleLocalDate
import com.agustavomartin.botlocalensayoandroid.ui.AppContainer
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val FILTER_ALL = "Todo"
private const val FILTER_ENSAYO = "Ensayo"
private const val FILTER_RIFF = "Riff"
private const val FILTER_CANCION = "Cancion"

@Composable
fun LibraryScreen(repository: BotRepository) {
    val state = produceState(initialValue = RemoteLoadState<List<AudioItem>>()) {
        value = runCatching { repository.getLibrary() }
            .fold(
                onSuccess = { RemoteLoadState(data = it) },
                onFailure = { RemoteLoadState(error = mapLoadError(it)) }
            )
    }.value
    val items = state.data
    val playbackState by produceState(initialValue = AppContainer.playbackManager.uiState.value) {
        AppContainer.playbackManager.uiState.collect { value = it }
    }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf(FILTER_ALL) }
    var titleQuery by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }

    val filteredItems = remember(items, selectedType, titleQuery, startDate, endDate) {
        items.orEmpty().filter { item ->
            matchesType(item, selectedType) &&
                item.title.contains(titleQuery.trim(), ignoreCase = true) &&
                matchesDateRange(item, startDate, endDate)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                eyebrow = "BIBLIOTECA",
                title = "Audios",
                subtitle = "Ensayos, riffs y canciones sincronizados desde la base remota."
            )
        }
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                InfoCard(
                    title = "Reproductor",
                    value = playbackState.currentAudio?.title ?: "Selecciona un audio",
                    supporting = when {
                        !playbackState.errorMessage.isNullOrBlank() -> playbackState.errorMessage ?: "Error de reproduccion"
                        playbackState.isBuffering -> "Cargando audio..."
                        playbackState.currentAudio != null -> playbackState.currentAudio?.dateLabel ?: "Reproduccion en curso"
                        else -> "Pulsa en Reproducir para empezar a escuchar."
                    }
                )
            }
        }

        if (items == null && state.error == null) {
            item { LoadingPanel("Cargando biblioteca...") }
            return@LazyColumn
        }

        if (items == null && state.error != null) {
            item { ErrorPanel(state.error) }
            return@LazyColumn
        }

        item {
            FilterChipRow(
                labels = listOf(FILTER_ALL, FILTER_ENSAYO, FILTER_RIFF, FILTER_CANCION),
                selectedLabel = selectedType,
                onSelected = { selectedType = it }
            )
        }
        item {
            FilterField(
                value = titleQuery,
                onValueChange = { titleQuery = it },
                label = "Filtrar por nombre"
            )
        }
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Text("Filtrar por rango de fechas", style = MaterialTheme.typography.titleMedium, color = Color(0xFFF4EFE6))
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            openDatePicker(context, startDate) { selected -> startDate = selected }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Desde: ${formatDateForChip(startDate)}")
                    }
                    OutlinedButton(
                        onClick = {
                            openDatePicker(context, endDate) { selected -> endDate = selected }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Hasta: ${formatDateForChip(endDate)}")
                    }
                }
                if (startDate != null || endDate != null) {
                    OutlinedButton(onClick = {
                        startDate = null
                        endDate = null
                    }) {
                        Text("Limpiar rango")
                    }
                }
            }
        }
        item {
            InfoCard(
                title = "Resultados",
                value = filteredItems.size.toString(),
                supporting = if (filteredItems.isEmpty()) "No hay audios que coincidan con los filtros." else "Audios visibles con los filtros actuales."
            )
        }

        items(filteredItems) { item ->
            AudioRow(
                item = item,
                isCurrent = playbackState.currentAudio == item,
                onPlay = {
                    scope.launch {
                        AppContainer.playbackManager.play(item)
                    }
                }
            )
        }
    }
}

private fun matchesType(item: AudioItem, selectedType: String): Boolean = when (selectedType) {
    FILTER_ENSAYO -> item.type == AudioType.ENSAYO
    FILTER_RIFF -> item.type == AudioType.RIFF
    FILTER_CANCION -> item.type == AudioType.CANCION
    else -> true
}

private fun matchesDateRange(item: AudioItem, startDate: LocalDate?, endDate: LocalDate?): Boolean {
    val itemDate = parseFlexibleLocalDate(item.rawDateKey) ?: parseFlexibleLocalDate(item.dateLabel) ?: parseFlexibleLocalDate(item.title)
    if (startDate == null && endDate == null) return true
    if (itemDate == null) return false
    val afterStart = startDate?.let { !itemDate.isBefore(it) } ?: true
    val beforeEnd = endDate?.let { !itemDate.isAfter(it) } ?: true
    return afterStart && beforeEnd
}

private fun openDatePicker(
    context: android.content.Context,
    initialDate: LocalDate?,
    onSelected: (LocalDate) -> Unit
) {
    val seed = initialDate ?: LocalDate.now()
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            onSelected(LocalDate.of(year, month + 1, dayOfMonth))
        },
        seed.year,
        seed.monthValue - 1,
        seed.dayOfMonth
    ).show()
}

@Composable
private fun AudioRow(
    item: AudioItem,
    isCurrent: Boolean,
    onPlay: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
    ) {
        DataRow(
            primary = item.title,
            secondary = item.dateLabel,
            trailing = "id ${item.id}"
        )
        Button(
            onClick = onPlay,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isCurrent) Color(0xFF1D4ED8) else Color(0xFF0E948B),
                contentColor = Color(0xFFF6F3EC)
            )
        ) {
            Text(
                if (isCurrent) "Reproduciendo" else "Reproducir",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

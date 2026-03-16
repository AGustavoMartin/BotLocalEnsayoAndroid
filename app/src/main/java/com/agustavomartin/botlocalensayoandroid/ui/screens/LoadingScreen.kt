package com.agustavomartin.botlocalensayoandroid.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun LoadingScreen() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        ScreenHeader(
            eyebrow = "BOT",
            title = "Cargando sesion",
            subtitle = "Comprobando si ya existe una sesion valida en este dispositivo."
        )
        CircularProgressIndicator(color = Color(0xFF47D7D1))
        Text(
            text = "Si la sesion sigue valida, entraremos directamente.",
            color = Color(0xFFC8D1DB),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

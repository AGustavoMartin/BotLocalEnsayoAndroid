package com.agustavomartin.botlocalensayoandroid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ScreenHeader(eyebrow: String, title: String, subtitle: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = eyebrow,
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF47D7D1)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFFF6F3EC),
            fontWeight = FontWeight.Black
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFFC5D0DB)
        )
    }
}

@Composable
fun InfoCard(
    title: String,
    value: String,
    supporting: String,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xCC1A2230), RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Text(text = title, color = Color(0xFF47D7D1), style = MaterialTheme.typography.labelLarge)
        Text(text = value, color = Color(0xFFF8F3E7), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(text = supporting, color = Color(0xFFC8D1DB), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun DataRow(primary: String, secondary: String, trailing: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xB2263140), RoundedCornerShape(20.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            Text(primary, color = Color(0xFFF4EFE6), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(secondary, color = Color(0xFFB9C7D4), style = MaterialTheme.typography.bodyMedium)
        }
        if (trailing != null) {
            Text(trailing, color = Color(0xFF47D7D1), style = MaterialTheme.typography.labelLarge)
        }
    }
}

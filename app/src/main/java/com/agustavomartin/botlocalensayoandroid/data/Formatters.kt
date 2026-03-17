package com.agustavomartin.botlocalensayoandroid.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val displayDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("es", "ES"))

fun formatCompactDateLabel(raw: String): String {
    val trimmed = raw.trim()
    return parseFlexibleLocalDate(trimmed)?.format(displayDateFormatter) ?: trimmed
}

fun parseFlexibleLocalDate(raw: String?): LocalDate? {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isBlank()) return null

    if (trimmed.matches(Regex("\\d{8}"))) {
        return try {
            LocalDate.parse(trimmed, DateTimeFormatter.BASIC_ISO_DATE)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    if (trimmed.matches(Regex("\\d{2}/\\d{2}/\\d{4}"))) {
        return try {
            LocalDate.parse(trimmed, displayDateFormatter)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    val embeddedCompact = Regex("(\\d{8})").find(trimmed)?.value
    if (!embeddedCompact.isNullOrBlank()) {
        return parseFlexibleLocalDate(embeddedCompact)
    }

    return null
}

fun formatDateForChip(date: LocalDate?): String = date?.format(displayDateFormatter) ?: "Seleccionar"

fun millisToLocalDate(value: Long): LocalDate = Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).toLocalDate()

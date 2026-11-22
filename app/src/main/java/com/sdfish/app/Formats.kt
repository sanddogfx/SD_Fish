package com.sdfish.app

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formats {
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")

    fun dateTime(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(dateTimeFormatter)

    fun coords(lat: Double, lon: Double): String = String.format(Locale.US, "%.5f, %.5f", lat, lon)

    fun number(value: Double?): String = when {
        value == null -> ""
        value % 1.0 == 0.0 -> value.toLong().toString()
        else -> value.toString()
    }

    fun parseNumber(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()
}

package com.sdfish.app

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formats {
    fun dateTime(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", AppLanguage.locale()))

    fun coords(lat: Double, lon: Double): String = String.format(Locale.US, "%.5f, %.5f", lat, lon)

    /** 2.4 -> "2,4" in Russian or Polish, "2.4" in English; whole numbers without decimals. */
    fun number(value: Double?): String =
        if (value == null) "" else DecimalFormat("0.##", DecimalFormatSymbols.getInstance(AppLanguage.locale())).format(value)

    fun parseNumber(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()
}

package com.nickrankin.traktapp.helper

import org.apache.commons.lang3.time.DateFormatUtils
import org.threeten.bp.LocalDate
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import java.util.*

fun getFormattedDateTime(date: OffsetDateTime, datePattern: String?, timePattern: String?): String {
    return if(timePattern != null) {
        date.atZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(datePattern + " " + timePattern))
    } else {
        date.atZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(datePattern))

    }
}

fun getFormattedDateTime(date: Date, datePattern: String?, timePattern: String?): String {
    return DateFormatUtils.format(date, datePattern + " " + timePattern)
}

fun getFormattedDate(date: LocalDate, datePattern: String?): String {
    return date.format(DateTimeFormatter.ofPattern(datePattern))
}

fun getFormattedDateTime(date: LocalDate, datePattern: String?, timePattern: String?): String {
    return date.format(DateTimeFormatter.ofPattern(datePattern + " " + timePattern))
}
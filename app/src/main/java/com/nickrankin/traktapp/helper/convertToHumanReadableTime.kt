package com.nickrankin.traktapp.helper

import android.text.format.DateUtils
import android.util.Log
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import java.util.*

private val TIME_FORMAT = "HH:mm"
private const val TAG = "convertToHumanReadableT"
fun convertToHumanReadableTime(dateTime: OffsetDateTime?): String {
    if(dateTime == null) {
        return ""
    }

    val zonedDateTime = dateTime.atZoneSameInstant(
        ZoneId.systemDefault())

    val calendar = Calendar.getInstance()
    calendar.set(Calendar.YEAR, zonedDateTime.year)
    calendar.set(Calendar.MONTH, zonedDateTime.month.value - 1)
    calendar.set(Calendar.DAY_OF_MONTH, zonedDateTime.dayOfMonth)
    calendar.set(Calendar.HOUR_OF_DAY, zonedDateTime.hour)
    calendar.set(Calendar.MINUTE, zonedDateTime.minute)

    val calendarToday = Calendar.getInstance(Locale.getDefault())

    Log.d(
        TAG, "convertToHumanReadableTime: $zonedDateTime  // ${DateUtils.getRelativeTimeSpanString(calendar.timeInMillis, calendarToday.timeInMillis, DateUtils.DAY_IN_MILLIS, DateUtils.FORMAT_SHOW_WEEKDAY).toString()} " +
            "${zonedDateTime.format(DateTimeFormatter.ofPattern(TIME_FORMAT))} ", )

    return "${DateUtils.getRelativeTimeSpanString(calendar.timeInMillis, calendarToday.timeInMillis, DateUtils.DAY_IN_MILLIS, DateUtils.FORMAT_SHOW_WEEKDAY)} at ${zonedDateTime.format(
        DateTimeFormatter.ofPattern(TIME_FORMAT))}"
}
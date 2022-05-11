package com.nickrankin.traktapp.helper

import java.util.concurrent.TimeUnit


fun calculateRuntimeWithDays(timeInMinutes: Long): String {
    // https://stackoverflow.com/questions/11357945/java-convert-seconds-into-day-hour-minute-and-seconds-using-timeunit
    val days = TimeUnit.MINUTES.toDays(timeInMinutes).toInt()
    val hours = TimeUnit.MINUTES.toHours(timeInMinutes) - days * 24
    val minute = TimeUnit.MINUTES.toMinutes(timeInMinutes) - TimeUnit.MINUTES.toHours(timeInMinutes) * 60
    return "Watched $days days, $hours hours, $minute minutes"
}
package com.nickrankin.traktapp.helper

fun calculateRuntime(timeInMinutes: Int): String {
    val hours = (timeInMinutes / 60)
    val minutes = timeInMinutes % 60

    return "${hours}H ${minutes}M"
}
package com.nickrankin.traktapp.helper

fun calculateProgress(completed: Double, aired: Double): Int {

    return (completed * 100 / aired).toInt()
}
package com.personaltracker.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

object DateUtils {
    val displayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    val monthYearFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy")
    val yearMonthKey: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    fun LocalDate.toDisplayString(): String = format(displayFormatter)
    fun LocalDate.toYearMonthKey(): String = format(yearMonthKey)
    fun YearMonth.toDisplayString(): String = atDay(1).format(monthYearFormatter)
    fun YearMonth.toKey(): String = atDay(1).format(yearMonthKey)

    fun greeting(): String {
        val hour = LocalDateTime.now().hour
        return when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    fun LocalDate.daysUntil(other: LocalDate): Long =
        java.time.temporal.ChronoUnit.DAYS.between(this, other)
}

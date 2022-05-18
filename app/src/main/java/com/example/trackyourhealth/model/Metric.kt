package com.example.trackyourhealth.model

import java.text.SimpleDateFormat
import java.util.*

typealias Metrics = List<Metric>

data class Metric(
    val id: Long,
    val title: String?,
    var listQualitativeMetric: List<DailyRecords>?
) {
    constructor(): this(0, "", emptyList())
}

data class DailyRecords(
    val metric: String?,
    val date: Date?,
    var valueInRelationPrevious: Double = 0.0
) {
    constructor(): this("", Date(), 0.0)

    fun formattedDeadline(): String = date?.let { formatDate(date) } ?: ""

    companion object {
        private val format = SimpleDateFormat("MM/dd/yyyy", Locale.US)

        fun parseDate(date: String): Date? = format.parse(date)

        fun formatDate(date: Date): String = format.format(date)

        fun simplifyDate(date: Date): Date? = parseDate(formatDate(date))
    }
}

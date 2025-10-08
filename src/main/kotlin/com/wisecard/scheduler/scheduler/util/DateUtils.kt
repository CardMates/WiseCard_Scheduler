package com.wisecard.scheduler.scheduler.util

import com.google.protobuf.Timestamp
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object DateUtils {

    private val formatters = listOf(
        DateTimeFormatter.ofPattern("yyyy.M.d"),
        DateTimeFormatter.ofPattern("yyyy.MM.dd"),
        DateTimeFormatter.ofPattern("yyyy-M-d"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd")
    )

    fun parseDateRange(dateText: String): Pair<LocalDate, LocalDate> {
        val parts = dateText.split("~").map { it.trim() }
        if (parts.size != 2) {
            throw IllegalArgumentException("잘못된 날짜 형식: $dateText")
        }

        val startPart = parts[0].replace(Regex("\\s+"), "").replace("/", ".")
        val endPart = parts[1].replace(Regex("\\s+"), "").replace("/", ".")

        for (formatter in formatters) {
            try {
                val startDate = LocalDate.parse(startPart, formatter)
                val endDate = LocalDate.parse(endPart, formatter)
                return Pair(startDate, endDate)
            } catch (_: DateTimeParseException) {
                continue
            }
        }

        throw IllegalArgumentException("날짜 변환 오류: $dateText")
    }

    fun toProtoTimestamp(localDate: LocalDate?): Timestamp {
        if (localDate == null) return Timestamp.getDefaultInstance()

        val instant = localDate.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant()
        return Timestamp.newBuilder()
            .setSeconds(instant.epochSecond)
            .setNanos(instant.nano)
            .build()
    }
}
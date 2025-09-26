package com.wisecard.scheduler.scheduler.dto

import java.time.LocalDate

data class Promotion(
    val cardCompany: CardCompany,
    val description: String,
    val imgUrl: String,
    val url: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
)
package com.wisecard.scheduler.domain.promotion

import com.wisecard.scheduler.domain.card.CardCompany
import java.time.LocalDate

data class PromotionInfo(
    val cardCompany: CardCompany,
    val description: String,
    val imgUrl: String,
    val url: String,
    val startDate: LocalDate?,
    val endDate: LocalDate,
)
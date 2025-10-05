package com.wisecard.scheduler.scheduler.dto

data class CardInfo(
    val cardId: Int? = null,
    val cardUrl: String,
    val cardCompany: CardCompany,
    val cardName: String,
    val imgUrl: String,
    val cardType: CardType,
    val benefits: String? = null,
)
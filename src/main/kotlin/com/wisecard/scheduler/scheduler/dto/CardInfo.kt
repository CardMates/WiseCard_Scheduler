package com.wisecard.scheduler.scheduler.dto

data class CardInfo (
    val cardCompany: CardCompany,
    val cardName: String,
    val imageUrl: String,
    val cardType: CardType,
    val benefits: String
)
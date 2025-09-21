package com.wisecard.scheduler.scheduler.dto

data class CardInfo (
    val cardBank: CardBank,
    val cardName: String,
    val imageUrl: String,
    val cardType: CardType,
    val benefits: String
)
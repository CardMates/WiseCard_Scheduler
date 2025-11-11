package com.wisecard.scheduler.application.ports.out

import com.wisecard.scheduler.domain.card.CardInfo

interface CardStoragePort {
    fun loadStoredCards(): List<CardInfo>
    fun saveCards(cards: List<CardInfo>)
    fun filterNewCards(crawledCards: List<CardInfo>): List<CardInfo>
    fun assignIdsToNewCards(newCards: List<CardInfo>): List<CardInfo>
    fun mergeCards(existingCards: List<CardInfo>, newCards: List<CardInfo>): List<CardInfo>
}
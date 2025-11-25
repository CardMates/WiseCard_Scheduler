package com.wisecard.scheduler.infrastructure.storage.adapter

import com.wisecard.scheduler.application.ports.out.CardStoragePort
import com.wisecard.scheduler.domain.card.CardInfo
import com.wisecard.scheduler.infrastructure.storage.CardDataStorageService
import org.springframework.stereotype.Component

@Component
class CardStorageAdapter(
    private val cardDataStorageService: CardDataStorageService
) : CardStoragePort {
    override fun loadStoredCards(): List<CardInfo> = cardDataStorageService.loadStoredCards()
    override fun saveCards(cards: List<CardInfo>) = cardDataStorageService.saveCards(cards)
    override fun filterNewCards(crawledCards: List<CardInfo>): List<CardInfo> =
        cardDataStorageService.filterNewCards(crawledCards)

    override fun assignIdsToNewCards(newCards: List<CardInfo>): List<CardInfo> =
        cardDataStorageService.assignIdsToNewCards(newCards)

    override fun mergeCards(existingCards: List<CardInfo>, newCards: List<CardInfo>): List<CardInfo> =
        cardDataStorageService.mergeCards(existingCards, newCards)
}
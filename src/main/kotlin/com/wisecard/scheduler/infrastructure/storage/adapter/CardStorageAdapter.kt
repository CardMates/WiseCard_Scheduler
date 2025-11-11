package com.wisecard.scheduler.infrastructure.storage.adapter

import com.wisecard.scheduler.application.ports.out.CardStoragePort
import com.wisecard.scheduler.domain.card.CardInfo
import com.wisecard.scheduler.infrastructure.storage.CardDataStorageService
import org.springframework.stereotype.Component

@Component
class CardStorageAdapter(
    private val delegate: CardDataStorageService
) : CardStoragePort {
    override fun loadStoredCards(): List<CardInfo> = delegate.loadStoredCards()
    override fun saveCards(cards: List<CardInfo>) = delegate.saveCards(cards)
    override fun filterNewCards(crawledCards: List<CardInfo>): List<CardInfo> = delegate.filterNewCards(crawledCards)
    override fun assignIdsToNewCards(newCards: List<CardInfo>): List<CardInfo> = delegate.assignIdsToNewCards(newCards)
    override fun mergeCards(existingCards: List<CardInfo>, newCards: List<CardInfo>): List<CardInfo> =
        delegate.mergeCards(existingCards, newCards)
}
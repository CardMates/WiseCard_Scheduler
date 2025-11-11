package com.wisecard.scheduler.infrastructure.storage

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.wisecard.scheduler.domain.card.CardInfo
import org.springframework.stereotype.Service
import java.io.File

@Service
class CardDataStorageService {

    private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
    private val dataFile = File("data/crawled_cards.json")

    fun loadStoredCards(): List<CardInfo> {
        return if (dataFile.exists()) {
            try {
                objectMapper.readValue<List<CardInfo>>(dataFile)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    fun saveCards(cards: List<CardInfo>) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(dataFile, cards)
        } catch (_: Exception) {
        }
    }

    fun filterNewCards(crawledCards: List<CardInfo>): List<CardInfo> {
        val storedCards = loadStoredCards()
        val storedCardKeys = storedCards.map { "${it.cardCompany}_${it.cardName}" }.toSet()

        return crawledCards.filter { card ->
            val cardKey = "${card.cardCompany}_${card.cardName}"
            !storedCardKeys.contains(cardKey)
        }
    }

    fun assignIdsToNewCards(newCards: List<CardInfo>): List<CardInfo> {
        val storedCards = loadStoredCards()
        var nextId = storedCards.maxOfOrNull { it.cardId ?: 0 } ?: 0

        return newCards.map { card ->
            nextId += 1
            card.copy(cardId = nextId, benefits = card.benefits ?: "")
        }
    }

    fun mergeCards(existingCards: List<CardInfo>, newCards: List<CardInfo>): List<CardInfo> {
        return existingCards + newCards
    }
}
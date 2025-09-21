package com.wisecard.scheduler.scheduler.service

import Card
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component


@Component
class JsonToProtoService(private val objectMapper: ObjectMapper) {

    fun parseJsonToProto(json: String): List<Card.Benefit> {
        val root = objectMapper.readTree(json)
        val benefitsArray = root.get("benefits")
        val cardBenefitList = benefitsArray.map { benefitNode ->
            val discounts = benefitNode.get("discounts").map {
                Card.DiscountBenefit.newBuilder()
                    .setRate(it.get("rate")?.asDouble() ?: 0.0)
                    .setAmount(it.get("amount")?.asInt() ?: 0)
                    .setMinimumAmount(it.get("minimum_amount")?.asInt() ?: 0)
                    .setBenefitLimit(it.get("benefit_limit")?.asInt() ?: 0)
                    .setMinimumSpending(it.get("minimum_spending")?.asInt() ?: 0)
                    .setChannel(channelTypeFromString(it.get("channel").asText()))
                    .build()
            }

            val points = benefitNode.get("points").map {
                Card.PointBenefit.newBuilder()
                    .setName(it.get("name")?.asText() ?: "")
                    .setAmount(it.get("amount")?.asInt() ?: 0)
                    .setRate(it.get("rate")?.asDouble() ?: 0.0)
                    .setMinimumAmount(it.get("minimum_amount")?.asInt() ?: 0)
                    .setBenefitLimit(it.get("benefit_limit")?.asInt() ?: 0)
                    .setMinimumSpending(it.get("minimum_spending")?.asInt() ?: 0)
                    .setChannel(channelTypeFromString(it.get("channel").asText()))
                    .build()
            }

            val cashbacks = benefitNode.get("cashbacks").map {
                Card.CashbackBenefit.newBuilder()
                    .setRate(it.get("rate")?.asDouble() ?: 0.0)
                    .setAmount(it.get("amount")?.asInt() ?: 0)
                    .setMinimumAmount(it.get("minimum_amount")?.asInt() ?: 0)
                    .setBenefitLimit(it.get("benefit_limit")?.asInt() ?: 0)
                    .setMinimumSpending(it.get("minimum_spending")?.asInt() ?: 0)
                    .setChannel(channelTypeFromString(it.get("channel").asText()))
                    .build()
            }

            Card.Benefit.newBuilder()
                .addAllDiscounts(discounts)
                .addAllPoints(points)
                .addAllCashbacks(cashbacks)
                .addAllCategories(benefitNode.get("categories").map { it.asText() })
                .addAllTargets(benefitNode.get("targets").map { it.asText() })
                .setSummary(benefitNode.get("summary")?.asText() ?: "")
                .build()
        }

        return cardBenefitList
    }

    private fun channelTypeFromString(channel: String) = when (channel.uppercase()) {
        "ONLINE" -> Card.ChannelType.ONLINE
        "OFFLINE" -> Card.ChannelType.OFFLINE
        else -> Card.ChannelType.BOTH
    }
}
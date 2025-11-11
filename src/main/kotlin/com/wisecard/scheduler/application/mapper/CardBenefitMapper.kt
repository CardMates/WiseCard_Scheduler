package com.wisecard.scheduler.application.mapper

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.sub.grpc.CardData
import com.wisecard.scheduler.util.logger

object CardBenefitMapper {

    private val objectMapper = ObjectMapper()

    fun parseJsonToProto(json: String): List<CardData.Benefit> {
        return try {
            objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true)
            val root = try {
                objectMapper.readTree(json)
            } catch (e: Exception) {
                logger.error("JSON 파싱 실패: ${e.message}")
                return emptyList()
            }

            val benefitsArray = root.get("benefits") ?: return emptyList()

            benefitsArray.mapNotNull { benefitNode ->
                try {
                    val discounts = benefitNode.get("discounts")?.map {
                        CardData.DiscountBenefit.newBuilder()
                            .setRate(it.get("rate")?.asDouble() ?: 0.0)
                            .setAmount(it.get("amount")?.asInt() ?: 0)
                            .setMinimumAmount(it.get("minimum_amount")?.asInt() ?: 0)
                            .setBenefitLimit(it.get("benefit_limit")?.asInt() ?: 0)
                            .setMinimumSpending(it.get("minimum_spending")?.asInt() ?: 0)
                            .setChannel(channelTypeFromString(it.get("channel")?.asText() ?: ""))
                            .build()
                    } ?: emptyList()

                    val points = benefitNode.get("points")?.map {
                        CardData.PointBenefit.newBuilder()
                            .setName(it.get("name")?.asText() ?: "")
                            .setAmount(it.get("amount")?.asInt() ?: 0)
                            .setRate(it.get("rate")?.asDouble() ?: 0.0)
                            .setMinimumAmount(it.get("minimum_amount")?.asInt() ?: 0)
                            .setBenefitLimit(it.get("benefit_limit")?.asInt() ?: 0)
                            .setMinimumSpending(it.get("minimum_spending")?.asInt() ?: 0)
                            .setChannel(channelTypeFromString(it.get("channel")?.asText() ?: ""))
                            .build()
                    } ?: emptyList()

                    val cashbacks = benefitNode.get("cashbacks")?.map {
                        CardData.CashbackBenefit.newBuilder()
                            .setRate(it.get("rate")?.asDouble() ?: 0.0)
                            .setAmount(it.get("amount")?.asInt() ?: 0)
                            .setMinimumAmount(it.get("minimum_amount")?.asInt() ?: 0)
                            .setBenefitLimit(it.get("benefit_limit")?.asInt() ?: 0)
                            .setMinimumSpending(it.get("minimum_spending")?.asInt() ?: 0)
                            .setChannel(channelTypeFromString(it.get("channel")?.asText() ?: ""))
                            .build()
                    } ?: emptyList()

                    CardData.Benefit.newBuilder()
                        .addAllDiscounts(discounts)
                        .addAllPoints(points)
                        .addAllCashbacks(cashbacks)
                        .addAllCategories(benefitNode.get("categories")?.map { it.asText() } ?: emptyList())
                        .addAllTargets(benefitNode.get("targets")?.map { it.asText() } ?: emptyList())
                        .setSummary(benefitNode.get("summary")?.asText() ?: "")
                        .build()
                } catch (e: Exception) {
                    logger.error("proto message 변환 중 오류 발생: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            logger.error("전체 JSON 파싱 실패: ${e.message}")
            emptyList()
        }
    }

    private fun channelTypeFromString(channel: String) = when (channel.uppercase()) {
        "ONLINE" -> CardData.ChannelType.ONLINE
        "OFFLINE" -> CardData.ChannelType.OFFLINE
        else -> CardData.ChannelType.BOTH
    }
}
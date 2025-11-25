package com.wisecard.scheduler.application.validator

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.wisecard.scheduler.util.logger

object BenefitsValidator {

    private val objectMapper = ObjectMapper()

    private val VALID_CATEGORY_CODES = setOf(
        "MT1", "CS2", "PS3", "SC4", "AC5", "PK6", "OL7",
        "CT1", "AG2", "PO3", "AT4", "AD5", "FD6", "CE7", "HP8", "PM9"
    )

    private val VALID_CHANNEL_VALUES = setOf("ONLINE", "OFFLINE", "BOTH")

    fun validate(refinedJson: String): ValidationResult {
        if (refinedJson.isBlank()) {
            return ValidationResult(false, listOf("정제된 JSON이 비어있습니다."))
        }

        val errors = mutableListOf<String>()

        try {
            val root = objectMapper.readTree(refinedJson)

            val benefitsArray = root.get("benefits")
            if (benefitsArray == null || !benefitsArray.isArray) {
                errors.add("'benefits' 배열이 필수입니다.")
                return ValidationResult(false, errors)
            }

            benefitsArray.forEachIndexed { index, benefitNode ->
                if (!benefitNode.isObject) {
                    errors.add("benefits[$index]는 객체여야 합니다.")
                    return@forEachIndexed
                }

                validateDiscounts(benefitNode, index, errors)
                validatePoints(benefitNode, index, errors)
                validateCashbacks(benefitNode, index, errors)
                validateCategories(benefitNode, index, errors)
                validateTargets(benefitNode, index, errors)
                validateSummary(benefitNode, index, errors)
            }

        } catch (e: Exception) {
            logger.error("JSON 파싱 실패: ${e.message}")
            errors.add("JSON 파싱 실패: ${e.message}")
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    private fun validateDiscounts(benefitNode: JsonNode, index: Int, errors: MutableList<String>) {
        val discounts = benefitNode.get("discounts")
        if (discounts != null && discounts.isArray) {
            discounts.forEachIndexed { discountIndex, discount ->
                if (discount.isObject) {
                    validateBenefitItem(discount, "benefits[$index].discounts[$discountIndex]", errors)
                }
            }
        }
    }

    private fun validatePoints(benefitNode: JsonNode, index: Int, errors: MutableList<String>) {
        val points = benefitNode.get("points")
        if (points != null && points.isArray) {
            points.forEachIndexed { pointIndex, point ->
                if (point.isObject) {
                    validateBenefitItem(point, "benefits[$index].points[$pointIndex]", errors)
                }
            }
        }
    }

    private fun validateCashbacks(benefitNode: JsonNode, index: Int, errors: MutableList<String>) {
        val cashbacks = benefitNode.get("cashbacks")
        if (cashbacks != null && cashbacks.isArray) {
            cashbacks.forEachIndexed { cashbackIndex, cashback ->
                if (cashback.isObject) {
                    validateBenefitItem(cashback, "benefits[$index].cashbacks[$cashbackIndex]", errors)
                }
            }
        }
    }

    private fun validateBenefitItem(item: JsonNode, path: String, errors: MutableList<String>) {
        val channel = item.get("channel")?.asText()
        if (channel != null && !VALID_CHANNEL_VALUES.contains(channel.uppercase())) {
            errors.add("$path.channel은 ONLINE, OFFLINE, BOTH 중 하나여야 합니다. (현재: $channel)")
        }

        val numericFields = listOf("rate", "amount", "minimum_amount", "benefit_limit", "minimum_spending")
        numericFields.forEach { field ->
            val fieldNode = item.get(field)
            if (fieldNode != null && !fieldNode.isNull && !fieldNode.isNumber) {
                errors.add("$path.$field 는 숫자 또는 null이어야 합니다.")
            }
        }
    }

    private fun validateCategories(benefitNode: JsonNode, index: Int, errors: MutableList<String>) {
        val categories = benefitNode.get("categories")
        if (categories != null && categories.isArray) {
            categories.forEachIndexed { categoryIndex, category ->
                val categoryCode = category.asText()
                if (!VALID_CATEGORY_CODES.contains(categoryCode)) {
                    errors.add("benefits[$index].categories[$categoryIndex]는 유효한 카카오맵 업종 코드여야 합니다. (현재: $categoryCode, 허용: ${VALID_CATEGORY_CODES.joinToString()})")
                }
            }
        }
    }

    private fun validateTargets(benefitNode: JsonNode, index: Int, errors: MutableList<String>) {
        val targets = benefitNode.get("targets")
        if (targets != null && targets.isArray) {
            targets.forEachIndexed { targetIndex, target ->
                if (!target.isTextual) {
                    errors.add("benefits[$index].targets[$targetIndex]는 문자열이어야 합니다.")
                }
            }
        }
    }

    private fun validateSummary(benefitNode: JsonNode, index: Int, errors: MutableList<String>) {
        val summary = benefitNode.get("summary")
        if (summary != null && !summary.isTextual) {
            errors.add("benefits[$index].summary는 문자열이어야 합니다.")
        }
    }

    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String>
    )
}
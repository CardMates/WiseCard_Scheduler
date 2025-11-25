package com.wisecard.scheduler.application.validator

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BenefitsValidatorTest {

    @Test
    fun `유효한 JSON은 검증 통과`() {
        val validJson = """
        {
          "benefits": [
            {
              "discounts": [
                {
                  "rate": 10.0,
                  "amount": null,
                  "minimum_amount": 10000,
                  "benefit_limit": 50000,
                  "minimum_spending": null,
                  "channel": "ONLINE"
                }
              ],
              "points": [],
              "cashbacks": [],
              "categories": ["FD6", "CE7"],
              "targets": ["스타벅스", "투썸플레이스"],
              "summary": "카페 할인 혜택"
            }
          ]
        }
        """.trimIndent()

        val result = BenefitsValidator.validate(validJson)

        assertThat(result.isValid).isTrue
        assertThat(result.errors).isEmpty()
    }

    @Test
    fun `benefits 배열이 없으면 검증 실패`() {
        val invalidJson = """
        {
          "data": []
        }
        """.trimIndent()

        val result = BenefitsValidator.validate(invalidJson)

        assertThat(result.isValid).isFalse
        assertThat(result.errors).contains("'benefits' 배열이 필수입니다.")
    }

    @Test
    fun `유효하지 않은 카테고리 코드는 검증 실패`() {
        val invalidJson = """
        {
          "benefits": [
            {
              "discounts": [],
              "points": [],
              "cashbacks": [],
              "categories": ["INVALID_CODE", "FD6"],
              "targets": [],
              "summary": "테스트"
            }
          ]
        }
        """.trimIndent()

        val result = BenefitsValidator.validate(invalidJson)

        assertThat(result.isValid).isFalse
        assertThat(result.errors).anyMatch { it.contains("유효한 카카오맵 업종 코드") && it.contains("INVALID_CODE") }
    }

    @Test
    fun `유효하지 않은 채널 값은 검증 실패`() {
        val invalidJson = """
        {
          "benefits": [
            {
              "discounts": [
                {
                  "rate": 10.0,
                  "amount": null,
                  "minimum_amount": null,
                  "benefit_limit": null,
                  "minimum_spending": null,
                  "channel": "INVALID"
                }
              ],
              "points": [],
              "cashbacks": [],
              "categories": [],
              "targets": [],
              "summary": "테스트"
            }
          ]
        }
        """.trimIndent()

        val result = BenefitsValidator.validate(invalidJson)

        assertThat(result.isValid).isFalse
        assertThat(result.errors).anyMatch { it.contains("ONLINE, OFFLINE, BOTH 중 하나") && it.contains("INVALID") }
    }

    @Test
    fun `모든 유효한 카테고리 코드 검증`() {
        val validCategories = listOf(
            "MT1", "CS2", "PS3", "SC4", "AC5", "PK6", "OL7",
            "CT1", "AG2", "PO3", "AT4", "AD5", "FD6", "CE7", "HP8", "PM9"
        )

        validCategories.forEach { category ->
            val json = """
            {
              "benefits": [
                {
                  "discounts": [],
                  "points": [],
                  "cashbacks": [],
                  "categories": ["$category"],
                  "targets": [],
                  "summary": "테스트"
                }
              ]
            }
            """.trimIndent()

            val result = BenefitsValidator.validate(json)
            assertThat(result.isValid).isTrue()
        }
    }

    @Test
    fun `빈 JSON은 검증 실패`() {
        val result = BenefitsValidator.validate("")

        assertThat(result.isValid).isFalse
        assertThat(result.errors).contains("정제된 JSON이 비어있습니다.")
    }

    @Test
    fun `잘못된 JSON 형식은 검증 실패`() {
        val invalidJson = "{ invalid json }"

        val result = BenefitsValidator.validate(invalidJson)

        assertThat(result.isValid).isFalse
        assertThat(result.errors).anyMatch { it.contains("JSON 파싱 실패") }
    }

    @Test
    fun `여러 benefit 객체 모두 검증`() {
        val json = """
        {
          "benefits": [
            {
              "discounts": [],
              "points": [],
              "cashbacks": [],
              "categories": ["FD6"],
              "targets": ["스타벅스"],
              "summary": "첫 번째 혜택"
            },
            {
              "discounts": [],
              "points": [],
              "cashbacks": [],
              "categories": ["INVALID"],
              "targets": [],
              "summary": "두 번째 혜택"
            }
          ]
        }
        """.trimIndent()

        val result = BenefitsValidator.validate(json)

        assertThat(result.isValid).isFalse
        assertThat(result.errors).anyMatch { it.contains("benefits[1]") && it.contains("INVALID") }
    }
}
package com.wisecard.scheduler.application.mapper

import com.sub.grpc.CardData
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CardBenefitMapperTest {

    @Test
    fun `유효한 JSON을 Proto로 변환`() {
        val json = """
        {
          "benefits": [
            {
              "discounts": [
                {
                  "rate": 10.5,
                  "amount": 1000,
                  "minimum_amount": 5000,
                  "benefit_limit": 50000,
                  "minimum_spending": 100000,
                  "channel": "ONLINE"
                }
              ],
              "points": [
                {
                  "name": "적립 포인트",
                  "amount": 500,
                  "rate": 1.0,
                  "minimum_amount": 10000,
                  "benefit_limit": null,
                  "minimum_spending": null,
                  "channel": "BOTH"
                }
              ],
              "cashbacks": [],
              "categories": ["FD6", "CE7"],
              "targets": ["스타벅스", "카페"],
              "summary": "카페 할인 및 포인트 적립"
            }
          ]
        }
        """.trimIndent()

        val result = CardBenefitMapper.parseJsonToProto(json)

        assertThat(result).hasSize(1)
        val benefit = result[0]

        // Discounts 검증
        assertThat(benefit.discountsList).hasSize(1)
        val discount = benefit.discountsList[0]
        assertThat(discount.rate).isEqualTo(10.5)
        assertThat(discount.amount).isEqualTo(1000)
        assertThat(discount.minimumAmount).isEqualTo(5000)
        assertThat(discount.benefitLimit).isEqualTo(50000)
        assertThat(discount.minimumSpending).isEqualTo(100000)
        assertThat(discount.channel).isEqualTo(CardData.ChannelType.ONLINE)

        // Points 검증
        assertThat(benefit.pointsList).hasSize(1)
        val point = benefit.pointsList[0]
        assertThat(point.name).isEqualTo("적립 포인트")
        assertThat(point.amount).isEqualTo(500)
        assertThat(point.rate).isEqualTo(1.0)
        assertThat(point.channel).isEqualTo(CardData.ChannelType.BOTH)

        // Categories 검증
        assertThat(benefit.categoriesList).containsExactly("FD6", "CE7")

        // Targets 검증
        assertThat(benefit.targetsList).containsExactly("스타벅스", "카페")

        // Summary 검증
        assertThat(benefit.summary).isEqualTo("카페 할인 및 포인트 적립")
    }

    @Test
    fun `null 값은 기본값으로 변환`() {
        val json = """
        {
          "benefits": [
            {
              "discounts": [
                {
                  "rate": null,
                  "amount": null,
                  "minimum_amount": null,
                  "benefit_limit": null,
                  "minimum_spending": null,
                  "channel": "OFFLINE"
                }
              ],
              "points": [],
              "cashbacks": [],
              "categories": [],
              "targets": [],
              "summary": ""
            }
          ]
        }
        """.trimIndent()

        val result = CardBenefitMapper.parseJsonToProto(json)

        assertThat(result).hasSize(1)
        val discount = result[0].discountsList[0]
        assertThat(discount.rate).isEqualTo(0.0)
        assertThat(discount.amount).isEqualTo(0)
        assertThat(discount.channel).isEqualTo(CardData.ChannelType.OFFLINE)
    }

    @Test
    fun `빈 benefits 배열은 빈 리스트 반환`() {
        val json = """
        {
          "benefits": []
        }
        """.trimIndent()

        val result = CardBenefitMapper.parseJsonToProto(json)

        assertThat(result).isEmpty()
    }

    @Test
    fun `benefits 필드가 없으면 빈 리스트 반환`() {
        val json = """
        {
          "data": []
        }
        """.trimIndent()

        val result = CardBenefitMapper.parseJsonToProto(json)

        assertThat(result).isEmpty()
    }

    @Test
    fun `잘못된 JSON 형식은 빈 리스트 반환`() {
        val json = "{ invalid json }"

        val result = CardBenefitMapper.parseJsonToProto(json)

        assertThat(result).isEmpty()
    }

    @Test
    fun `여러 benefit 객체 변환`() {
        val json = """
        {
          "benefits": [
            {
              "discounts": [],
              "points": [],
              "cashbacks": [],
              "categories": ["FD6"],
              "targets": ["스타벅스"],
              "summary": "첫 번째"
            },
            {
              "discounts": [],
              "points": [],
              "cashbacks": [],
              "categories": ["CE7"],
              "targets": ["카페"],
              "summary": "두 번째"
            }
          ]
        }
        """.trimIndent()

        val result = CardBenefitMapper.parseJsonToProto(json)

        assertThat(result).hasSize(2)
        assertThat(result[0].summary).isEqualTo("첫 번째")
        assertThat(result[1].summary).isEqualTo("두 번째")
    }

    @Test
    fun `채널 값 대소문자 무시하고 변환`() {
        val json = """
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
                  "channel": "online"
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

        val result = CardBenefitMapper.parseJsonToProto(json)

        assertThat(result[0].discountsList[0].channel).isEqualTo(CardData.ChannelType.ONLINE)
    }

    @Test
    fun `알 수 없는 채널 값은 BOTH로 변환`() {
        val json = """
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
                  "channel": "UNKNOWN"
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

        val result = CardBenefitMapper.parseJsonToProto(json)

        assertThat(result[0].discountsList[0].channel).isEqualTo(CardData.ChannelType.BOTH)
    }
}
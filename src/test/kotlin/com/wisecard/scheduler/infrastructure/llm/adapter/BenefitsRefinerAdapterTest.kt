package com.wisecard.scheduler.infrastructure.llm.adapter

import com.wisecard.scheduler.infrastructure.llm.LlmClient
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BenefitsRefinerAdapterTest {

    @Test
    fun `유효한 정제 결과는 그대로 반환`() {
        val llmClient = mockk<LlmClient>()
        val adapter = BenefitsRefinerAdapter(llmClient)

        val validJson = """
        {
          "benefits": [
            {
              "discounts": [],
              "points": [],
              "cashbacks": [],
              "categories": ["FD6"],
              "targets": ["스타벅스"],
              "summary": "테스트"
            }
          ]
        }
        """.trimIndent()

        every { llmClient.refine(any()) } returns validJson

        val result = adapter.refine("raw text")

        assertThat(result).isEqualTo(validJson)
    }

    @Test
    fun `검증 실패 시 빈 JSON 반환`() {
        val llmClient = mockk<LlmClient>()
        val adapter = BenefitsRefinerAdapter(llmClient)

        val invalidJson = """
        {
          "benefits": [
            {
              "discounts": [],
              "categories": ["INVALID"]
            }
          ]
        }
        """.trimIndent()

        every { llmClient.refine(any()) } returns invalidJson

        val result = adapter.refine("raw text")

        assertThat(result).isEqualTo("""{"benefits": []}""")
    }

    @Test
    fun `빈 JSON은 빈 JSON 반환`() {
        val llmClient = mockk<LlmClient>()
        val adapter = BenefitsRefinerAdapter(llmClient)

        every { llmClient.refine(any()) } returns ""

        val result = adapter.refine("raw text")

        assertThat(result).isEqualTo("""{"benefits": []}""")
    }
}
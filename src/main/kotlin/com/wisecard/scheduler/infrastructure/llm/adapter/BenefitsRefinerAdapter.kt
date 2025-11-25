package com.wisecard.scheduler.infrastructure.llm.adapter

import com.wisecard.scheduler.application.ports.out.BenefitsRefiner
import com.wisecard.scheduler.application.validator.BenefitsValidator
import com.wisecard.scheduler.infrastructure.llm.LlmClient
import com.wisecard.scheduler.util.logger
import org.springframework.stereotype.Component

@Component
class BenefitsRefinerAdapter(
    private val llmClient: LlmClient
) : BenefitsRefiner {
    override fun refine(raw: String): String {
        val refined = llmClient.refine(raw)

        val validationResult = BenefitsValidator.validate(refined)
        if (!validationResult.isValid) {
            logger.warn("LLM 정제 결과 검증 실패: ${validationResult.errors.joinToString(", ")}")
            return """{"benefits": []}"""
        }

        return refined
    }
}
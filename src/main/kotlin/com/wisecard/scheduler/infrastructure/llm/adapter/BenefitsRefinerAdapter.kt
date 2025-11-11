package com.wisecard.scheduler.infrastructure.llm.adapter

import com.wisecard.scheduler.application.ports.out.BenefitsRefiner
import com.wisecard.scheduler.infrastructure.llm.LlmClient
import org.springframework.stereotype.Component

@Component
class BenefitsRefinerAdapter(
    private val llmClient: LlmClient
) : BenefitsRefiner {
    override fun refine(raw: String): String = llmClient.refine(raw)
}
package com.wisecard.scheduler.infrastructure.llm

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "llm")
class LlmProperties {
    lateinit var apiKey: String
}
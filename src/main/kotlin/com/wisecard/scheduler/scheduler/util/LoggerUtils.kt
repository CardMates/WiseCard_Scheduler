package com.wisecard.scheduler.scheduler.util

import com.wisecard.scheduler.scheduler.dto.CardCompany
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val <T : Any> T.logger: Logger
    get() = LoggerFactory.getLogger(this::class.java)

object LoggerUtils {
    fun logCrawlingStart(cardCompany: CardCompany, info: String) {
        logger.info("[${cardCompany.name}] $info 크롤링 시작")
    }

    fun logCrawlingError(cardCompany: CardCompany, info: String, e: Exception) {
        logger.error("[${cardCompany.name}] $info 크롤링 에러: ${e.message}")
    }
}
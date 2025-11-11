package com.wisecard.scheduler.application.scheduler

import com.wisecard.scheduler.application.usecase.CrawlRefineAndSendCardsUseCase
import com.wisecard.scheduler.application.usecase.SendPromotionsUseCase
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class CrawlingAndRefiningScheduler(
    private val crawlRefineAndSendCardsUseCase: CrawlRefineAndSendCardsUseCase,
    private val sendPromotionsUseCase: SendPromotionsUseCase
) {
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    fun scheduler() {
        sendPromotionsUseCase.execute()
        crawlRefineAndSendCardsUseCase.execute()
    }
}
package com.wisecard.scheduler.infrastructure.crawler.promotion

import com.wisecard.scheduler.domain.card.CardCompany
import com.wisecard.scheduler.domain.promotion.PromotionInfo
import com.wisecard.scheduler.util.DateUtils.parseDateRange
import com.wisecard.scheduler.util.LoggerUtils.logCrawlingError
import com.wisecard.scheduler.util.LoggerUtils.logCrawlingStart
import io.github.bonigarcia.wdm.WebDriverManager
import org.jsoup.Jsoup
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class KookminPromotionCrawler : PromotionCrawler {
    override val cardCompany = CardCompany.KOOKMIN

    override fun crawlPromotions(): List<PromotionInfo> {
        logCrawlingStart(cardCompany, "프로모션")
        val promotions = mutableListOf<PromotionInfo>()
        var driver: WebDriver? = null

        try {
            WebDriverManager.chromedriver().setup()
            val options = ChromeOptions()
            options.addArguments("--headless")
            options.addArguments("--no-sandbox")
            options.addArguments("--disable-dev-shm-usage")
            options.addArguments("--disable-gpu")
            options.addArguments("--window-size=1920,1080")

            driver = ChromeDriver(options)
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30))

            val url = "https://card.kbcard.com/BON/DVIEW/HBBMCXCRVNEC0002"
            driver.get(url)

            val html = driver.pageSource
            val soup = Jsoup.parse(html!!)

            val promotionElements = soup.select("ul#ajaxMakeArea li")

            for (element in promotionElements) {
                try {
                    val dateStatus = element.select("em.date__txt").text()
                    if (dateStatus == "진행 예정") continue

                    val store = element.select("span.store").text().trim()
                    val subject = element.select("span.subject").text().trim()
                    val description = "$store $subject".trim()
                    if (description.isEmpty()) continue

                    val imgUrl = element.select("span.icon img").attr("src").trim()
                    if (imgUrl.isEmpty()) continue

                    val href = element.select("a").attr("href").trim()
                    val eventId = href.replace("javascript:goDetail('", "").replace("')", "")
                    val fullUrl = if (eventId.isNotEmpty()) {
                        "https://card.kbcard.com/BON/DVIEW/HBBMCXCRVNEC0002?mainCC=a&eventNum=$eventId"
                    } else {
                        ""
                    }
                    if (fullUrl.isEmpty()) continue

                    val dateText = element.select("span.date").html()
                    val cleanDate = dateText
                        .replace(Regex("<em.*?</em>"), "")
                        .replace(Regex("\\s+"), " ")
                        .trim()
                    val (startDate, endDate) = parseDateRange(cleanDate)

                    promotions.add(
                        PromotionInfo(
                            cardCompany = cardCompany,
                            description = description,
                            imgUrl = imgUrl,
                            url = fullUrl,
                            startDate = startDate,
                            endDate = endDate
                        )
                    )
                } catch (e: Exception) {
                    logCrawlingError(cardCompany, "프로모션", e)
                }
            }
        } catch (e: Exception) {
            logCrawlingError(cardCompany, "프로모션", e)
        } finally {
            driver?.quit()
        }

        println(promotions)
        return promotions
    }
}
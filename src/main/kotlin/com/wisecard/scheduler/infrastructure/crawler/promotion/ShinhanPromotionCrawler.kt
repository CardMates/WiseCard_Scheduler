package com.wisecard.scheduler.infrastructure.crawler.promotion

import com.wisecard.scheduler.domain.card.CardCompany
import com.wisecard.scheduler.domain.promotion.PromotionInfo
import com.wisecard.scheduler.util.LoggerUtils.logCrawlingStart
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class ShinhanPromotionCrawler : PromotionCrawler {
    override val cardCompany = CardCompany.SHINHAN

    override fun crawlPromotions(): List<PromotionInfo> {
        logCrawlingStart(cardCompany, "프로모션")
        val options = ChromeOptions()
        options.addArguments("--headless", "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage")
        val driver: WebDriver = ChromeDriver(options)
        val promotions = mutableListOf<PromotionInfo>()

        try {
            val url = "https://www.shinhancard.com/mob/MOBFM026N/MOBFM026C01.shc"
            driver.get(url)

            val wait = WebDriverWait(driver, Duration.ofSeconds(10))
            wait.until { driver.findElements(By.id("listData")).isNotEmpty() }

            val listItems = driver.findElements(By.cssSelector("ul#listData > li.list-item"))

            for (item in listItems) {
                var href = item.findElement(By.cssSelector("a.a-block-all")).getDomAttribute("href") ?: continue
                if (href.startsWith("/")) {
                    href = "https://www.shinhancard.com$href"
                }

                val imgDiv = item.findElement(By.cssSelector("div.img-sec"))
                val style = imgDiv.getDomAttribute("style") ?: ""
                val imgUrlMatch = Regex("""background-image:\s?url\(['"]?(.*?)['"]?\)""").find(style)
                var imgUrl = imgUrlMatch?.groups?.get(1)?.value ?: ""
                if (imgUrl.startsWith("/")) {
                    imgUrl = "https://www.shinhancard.com$imgUrl"
                }

                val description = item.findElement(By.cssSelector("div.text-sec > div.text1")).text ?: ""

                val endDateText = item.findElement(By.cssSelector("div.text-sec > div.text2")).text ?: ""
                val endDate = try {
                    LocalDate.parse(endDateText, DateTimeFormatter.ofPattern("yyyy.MM.dd"))
                } catch (e: Exception) {
                    null
                }

                if (description.isNotEmpty() && endDate != null) {
                    promotions.add(
                        PromotionInfo(
                            cardCompany = cardCompany,
                            description = description,
                            imgUrl = imgUrl,
                            url = href,
                            startDate = null,
                            endDate = endDate
                        )
                    )
                }
            }
        } finally {
            driver.quit()
        }

        println(promotions)
        return promotions
    }
}
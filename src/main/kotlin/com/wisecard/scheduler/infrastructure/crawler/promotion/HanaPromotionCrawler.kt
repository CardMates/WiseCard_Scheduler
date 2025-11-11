package com.wisecard.scheduler.infrastructure.crawler.promotion

import com.wisecard.scheduler.domain.card.CardCompany
import com.wisecard.scheduler.domain.promotion.PromotionInfo
import com.wisecard.scheduler.util.DateUtils.parseDateRange
import com.wisecard.scheduler.util.LoggerUtils.logCrawlingError
import com.wisecard.scheduler.util.LoggerUtils.logCrawlingStart
import io.github.bonigarcia.wdm.WebDriverManager
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.springframework.stereotype.Component

@Component
class HanaPromotionCrawler : PromotionCrawler {
    override val cardCompany = CardCompany.HANA

    private val baseUrl = "https://m.hanacard.co.kr"

    override fun crawlPromotions(): List<PromotionInfo> {
        logCrawlingStart(cardCompany, "프로모션")
        WebDriverManager.chromedriver().setup()

        val options = ChromeOptions().apply {
            addArguments("--headless", "--no-sandbox", "--disable-dev-shm-usage")
            addArguments(
                "user-agent=Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) " +
                        "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1"
            )
        }

        val driver: WebDriver = ChromeDriver(options)

        return try {
            driver.get("$baseUrl/MKEVT1000M.web")
            scrollToEnd(driver)
            val items = driver.findElements(By.cssSelector(".usage-default-item.type-line"))
            val promotions = items.mapNotNull { parsePromotion(it) }
            println(promotions)
            promotions
        } finally {
            driver.quit()
        }
    }

    private fun scrollToEnd(driver: WebDriver) {
        var lastHeight = (driver as ChromeDriver).executeScript("return document.body.scrollHeight") as Long
        while (true) {
            driver.executeScript("window.scrollTo(0, document.body.scrollHeight);")
            Thread.sleep(2000)
            val newHeight = driver.executeScript("return document.body.scrollHeight") as Long
            if (newHeight == lastHeight) break
            lastHeight = newHeight
        }
    }

    private fun parsePromotion(item: WebElement): PromotionInfo? {
        return try {
            val linkElement = item.findElement(By.cssSelector("a.usage-default-left-group"))
            val title = linkElement.findElement(By.cssSelector(".usage-default-title")).text
            val brand = linkElement.findElement(By.cssSelector(".usage-default-brand")).text
            val imgUrl = item.findElement(By.tagName("img")).getDomAttribute("src")?.let { "$baseUrl$it" } ?: ""

            val onclickAttr = linkElement.getDomAttribute("onclick") ?: ""
            val regex = "detail\\('(.+?)','(\\d+)'\\)".toRegex()
            val match = regex.find(onclickAttr)
            val path = match?.groups?.get(1)?.value ?: ""
            val evnSeq = match?.groups?.get(2)?.value ?: ""
            val url = if (path.isNotBlank() && evnSeq.isNotBlank()) "$baseUrl$path?EVN_SEQ=$evnSeq" else ""

            val periodText = linkElement.findElement(By.cssSelector(".usage-default-etc-item")).text
            val (startDate, endDate) = parseDateRange(periodText)

            PromotionInfo(
                cardCompany = cardCompany,
                description = "$title $brand",
                imgUrl = imgUrl,
                url = url,
                startDate = startDate,
                endDate = endDate
            )
        } catch (e: Exception) {
            logCrawlingError(cardCompany, "프로모션", e)
            null
        }
    }
}
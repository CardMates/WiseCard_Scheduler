package com.wisecard.scheduler.infrastructure.crawler.promotion

import com.wisecard.scheduler.domain.card.CardCompany
import com.wisecard.scheduler.domain.promotion.PromotionInfo
import com.wisecard.scheduler.util.DateUtils.parseDateRange
import com.wisecard.scheduler.util.LoggerUtils.logCrawlingError
import com.wisecard.scheduler.util.LoggerUtils.logCrawlingStart
import io.github.bonigarcia.wdm.WebDriverManager
import org.jsoup.Jsoup
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class LottePromotionCrawler : PromotionCrawler {
    override val cardCompany = CardCompany.LOTTE

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
            val wait = WebDriverWait(driver, Duration.ofSeconds(10))

            val url = "https://www.lottecard.co.kr/app/LPBNFDA_V100.lc"
            driver.get(url)

            loadAllPromotions(driver, wait)

            val html = driver.pageSource
            val soup = Jsoup.parse(html!!)

            val promotionElements = soup.select("div.eventList ul#divList li")

            for (element in promotionElements) {
                try {
                    val descriptionElement = element.select("span.eventCont b").firstOrNull()
                    val description = descriptionElement?.text()?.trim() ?: ""
                    if (description.isEmpty()) continue

                    val imgSrc = element.select("img").attr("src").trim()
                    val imgUrl = if (imgSrc.startsWith("http")) imgSrc else "https:$imgSrc"

                    val href = element.select("a").attr("href").trim()
                    val eventId = extractEventId(href)
                    val fullUrl = if (eventId.isNotEmpty()) {
                        "https://www.lottecard.co.kr/app/LPBNFDA_V300.lc?evnBultSeq=$eventId&evnCtgSeq=9999&bigTabGubun=2"
                    } else {
                        ""
                    }
                    if (fullUrl.isEmpty()) continue

                    val dateText = element.select("span.date").text().trim()
                    val (startDate, endDate) = parseDateRange(dateText)

                    promotions.add(
                        PromotionInfo(
                            cardCompany = CardCompany.LOTTE,
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

    private fun loadAllPromotions(driver: WebDriver, wait: WebDriverWait) {
        var hasMore = true
        var currentPage = 1
        var clickCount = 0
        val maxClicks = 10

        while (hasMore && clickCount < maxClicks) {
            try {
                val moreButton = wait.until(
                    ExpectedConditions.elementToBeClickable(By.id("btnMore"))
                )
                val pageText = moreButton.findElement(By.id("pageText")).text

                if (!pageText.contains("더보기")) {
                    break
                }

                val jsExecutor = driver as JavascriptExecutor
                jsExecutor.executeScript("fnMoreSerch();")
                Thread.sleep(3000)
                currentPage++
                clickCount++
            } catch (e: Exception) {
                hasMore = false
            }
        }
    }

    private fun extractEventId(href: String): String {
        val matchResult = Regex("tlfLoad\\([^,]+,[^,]+,'([0-9]+)'[^)]+\\)").find(href)
        return matchResult?.groupValues?.get(1) ?: ""
    }
}
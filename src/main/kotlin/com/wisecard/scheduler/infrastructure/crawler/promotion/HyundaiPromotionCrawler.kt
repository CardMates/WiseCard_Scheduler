package com.wisecard.scheduler.infrastructure.crawler.promotion

import com.wisecard.scheduler.domain.card.CardCompany
import com.wisecard.scheduler.domain.promotion.PromotionInfo
import com.wisecard.scheduler.util.DateUtils.parseDateRange
import com.wisecard.scheduler.util.LoggerUtils.logCrawlingError
import com.wisecard.scheduler.util.LoggerUtils.logCrawlingStart
import io.github.bonigarcia.wdm.WebDriverManager
import org.jsoup.Jsoup
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.springframework.stereotype.Component

@Component
class HyundaiPromotionCrawler : PromotionCrawler {
    override val cardCompany = CardCompany.HYUNDAI

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

            val url = "https://www.hyundaicard.com/cpb/ev/CPBEV0101_01.hc"
            driver.get(url)

            loadAllPromotions(driver)

            val html = driver.pageSource
            val soup = Jsoup.parse(html!!)

            val promotionElements = soup.select("ul.list_event li")

            for (element in promotionElements) {
                try {
                    val descriptionElement = element.select("span.txt_title").firstOrNull()
                    val description = descriptionElement?.text()?.trim()?.replace("\n", " ")?.replace("\\s+".toRegex(), " ") ?: ""
                    if (description.isEmpty()) continue

                    val imgSrc = element.select("span.imgbox img").firstOrNull()?.attr("src") ?: ""
                    val imgUrl = if (imgSrc.startsWith("http")) imgSrc else "https://www.hyundaicard.com$imgSrc"

                    val href = element.select("a").firstOrNull()?.attr("href") ?: ""
                    val fullUrl = if (href.startsWith("http")) href else "https://www.hyundaicard.com$href"

                    val dateText = element.select("span.txt_date").firstOrNull()?.text()?.trim() ?: ""
                    val (startDate, endDate) = parseDateRange(dateText)

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

    private fun loadAllPromotions(driver: WebDriver) {
        while (true) {
            try {
                val buttons = driver.findElements(By.cssSelector("a.btn48_softbg_boldtxt"))
                if (buttons.isEmpty()) {
                    break
                }

                val moreButton = buttons.firstOrNull {
                    it.findElements(By.cssSelector("span.btn-more")).isNotEmpty()
                }

                if (moreButton == null) {
                    break
                }

                val buttonText = moreButton.findElement(By.cssSelector("span.btn-more")).text
                if (!buttonText.contains("더 보기")) {
                    break
                }

                (driver as org.openqa.selenium.JavascriptExecutor)
                    .executeScript("arguments[0].click();", moreButton)

                Thread.sleep(2000)
            } catch (e: Exception) {
                break
            }
        }
    }
}
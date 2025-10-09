package com.wisecard.scheduler.scheduler.crawler.promotion

import com.wisecard.scheduler.scheduler.dto.CardCompany
import com.wisecard.scheduler.scheduler.dto.PromotionInfo
import com.wisecard.scheduler.scheduler.util.DateUtils.parseDateRange
import com.wisecard.scheduler.scheduler.util.LoggerUtils.logCrawlingError
import com.wisecard.scheduler.scheduler.util.LoggerUtils.logCrawlingStart
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class SamsungPromotionCrawler : PromotionCrawler {
    override val cardCompany = CardCompany.SAMSUNG

    override fun crawlPromotions(): List<PromotionInfo> {
        logCrawlingStart(cardCompany, "프로모션")
        val url = "https://www.samsungcard.com/personal/event/ing/UHPPBE1401M0.jsp?click=benefitmain_logout_event"
        val promotions = mutableListOf<PromotionInfo>()

        val options = ChromeOptions()
        options.addArguments("--headless")
        options.addArguments("--disable-gpu")
        options.addArguments("--no-sandbox")
        options.addArguments("--disable-dev-shm-usage")

        val driver: WebDriver = ChromeDriver(options)
        val wait = WebDriverWait(driver, Duration.ofSeconds(10))

        try {
            driver.get(url)

            wait.until { driver.findElements(By.cssSelector("ul#board_append > li")).isNotEmpty() }

            val promotionElements = driver.findElements(By.cssSelector("ul#board_append > li"))

            for (element in promotionElements) {
                val descriptionElement = element.findElement(By.cssSelector("p.tit"))
                val description = descriptionElement.text.takeIf { it.isNotBlank() } ?: continue

                val imgUrlElement = element.findElement(By.cssSelector("img.p_display"))
                val imgUrl = imgUrlElement.getDomAttribute("src")?.let { src ->
                    val fixed = if (src.startsWith("//")) "https:$src" else src
                    fixed.takeIf { it.isNotBlank() }
                } ?: continue

                val linkElement = element.findElement(By.cssSelector("a.m_link"))
                val onclick = linkElement.getDomAttribute("onclick")?.takeIf { it.isNotBlank() } ?: continue
                val cmsId = Regex("'(\\d+)'").find(onclick)?.groupValues?.get(1) ?: continue
                val eventUrl = "https://www.samsungcard.com/personal/event/ing/UHPPBE1403M0.jsp?cms_id=$cmsId"

                val dateElement = element.findElement(By.cssSelector("span.date"))
                val dateText = dateElement.text.takeIf { it.isNotBlank() } ?: continue
                val (startDate, endDate) = parseDateRange(dateText)

                val promotion = PromotionInfo(
                    cardCompany = CardCompany.SAMSUNG,
                    description = description,
                    imgUrl = imgUrl,
                    url = eventUrl,
                    startDate = startDate,
                    endDate = endDate
                )
                promotions.add(promotion)
            }
        } catch (e: Exception) {
            logCrawlingError(cardCompany, "프로모션", e)
        } finally {
            driver.quit()
        }

        println(promotions)
        return promotions
    }
}
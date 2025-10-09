package com.wisecard.scheduler.scheduler.crawler.card

import com.wisecard.scheduler.scheduler.dto.CardCompany
import com.wisecard.scheduler.scheduler.dto.CardInfo
import com.wisecard.scheduler.scheduler.dto.CardType
import com.wisecard.scheduler.scheduler.util.LoggerUtils.logCrawlingError
import com.wisecard.scheduler.scheduler.util.LoggerUtils.logCrawlingStart
import org.jsoup.Jsoup
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class ShinhanCardCrawler : CardCrawler {
    override val cardCompany = CardCompany.SHINHAN

    private val creditCardUrl = "https://www.shinhancard.com/pconts/html/card/credit/MOBFM281/MOBFM281R11.html"
    private val checkCardUrl = "https://www.shinhancard.com/pconts/html/card/check/MOBFM282R11.html?crustMenuId=ms527"

    override fun crawlCreditCardBasicInfos(): List<CardInfo> {
        logCrawlingStart(cardCompany, "신용 카드 기본 정보")
        return crawlCardList(creditCardUrl, CardType.CREDIT)
    }

    override fun crawlCheckCardBasicInfos(): List<CardInfo> {
        logCrawlingStart(cardCompany, "체크 카드 기본 정보")
        return crawlCardList(checkCardUrl, CardType.CHECK)
    }

    override fun crawlCreditCardBenefits(cards: List<CardInfo>): List<CardInfo> {
        logCrawlingStart(cardCompany, "신용 카드 혜택 정보")
        return cards.map { card ->
            val benefits = crawlCardBenefits(card.cardUrl)
            card.copy(benefits = benefits)
        }
    }

    override fun crawlCheckCardBenefits(cards: List<CardInfo>): List<CardInfo> {
        logCrawlingStart(cardCompany, "체크 카드 혜택 정보")
        return cards.map { card ->
            val benefits = crawlCardBenefits(card.cardUrl)
            card.copy(benefits = benefits)
        }
    }

    private fun crawlCardList(url: String, cardType: CardType): List<CardInfo> {
        val cards = mutableListOf<CardInfo>()
        var driver: WebDriver? = null

        try {
            driver = createChromeDriver()
            driver.get(url)
            Thread.sleep(3000)

            val html = driver.pageSource
            val soup = Jsoup.parse(html!!)

            val divTag = soup.select("div[data-plugin-view='cmmCardList']").first()
            if (divTag != null) {
                val ulTag = divTag.select("ul.card_thumb_list_wrap").first()
                if (ulTag != null) {
                    val listElements = ulTag.select("li")

                    for (element in listElements) {
                        try {
                            val cardNameElement = element.select("a.card_name").first()
                            val cardName = cardNameElement?.text()?.trim() ?: ""

                            if (cardName.isNotEmpty()) {
                                val aTag = element.select("a").first()
                                val href = aTag?.attr("href") ?: ""
                                val cardUrl = href.split("/").lastOrNull() ?: ""
                                val type = if (cardType == CardType.CHECK) "check" else "credit"
                                val fullUrl = "https://www.shinhancard.com/pconts/html/card/apply/$type/$cardUrl"


                                val imgElement = element.select("img").first()
                                val imgSrc = imgElement?.attr("src") ?: ""
                                val imgUrl =
                                    if (imgSrc.startsWith("http")) imgSrc else "https://www.shinhancard.com$imgSrc"

                                cards.add(
                                    CardInfo(
                                        cardId = null,
                                        cardUrl = fullUrl,
                                        cardCompany = CardCompany.SHINHAN,
                                        cardName = cardName,
                                        imgUrl = imgUrl,
                                        cardType = cardType,
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            logCrawlingError(cardCompany, "카드 혜택", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logCrawlingError(cardCompany, "카드 혜택", e)
        } finally {
            driver?.quit()
        }

        return cards
    }

    private fun createChromeDriver(): WebDriver {
        val options = ChromeOptions()
        options.addArguments("--headless")
        options.addArguments("--no-sandbox")
        options.addArguments("--disable-dev-shm-usage")
        options.addArguments("--disable-gpu")
        options.addArguments("--window-size=1920,1080")

        val driver = ChromeDriver(options)
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(20))
        return driver
    }

    private fun crawlCardBenefits(cardUrl: String): String {
        var driver: WebDriver? = null
        try {
            driver = createChromeDriver()


            driver.get(cardUrl)
            Thread.sleep(3000)

            val html = driver.pageSource
            val soup = Jsoup.parse(html!!)
            val benefit = StringBuilder()

            val benefitContWraps = soup.select("div.benefit_cont_wrap")
            if (benefitContWraps.isNotEmpty()) {
                for (wrap in benefitContWraps) {
                    benefit.append("**${wrap.text().trim()}**\n")
                }
            } else {
                return ""
            }

            return benefit.toString().trim()
        } catch (e: Exception) {
            logCrawlingError(cardCompany, "카드 혜택", e)
            return "혜택 정보 없음"
        } finally {
            driver?.quit()
        }
    }
}
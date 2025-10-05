package com.wisecard.scheduler.scheduler.crawler.card

import com.wisecard.scheduler.scheduler.dto.CardInfo
import com.wisecard.scheduler.scheduler.dto.CardCompany
import com.wisecard.scheduler.scheduler.dto.CardType
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
        val cards = mutableListOf<CardInfo>()
        var driver: WebDriver? = null

        try {
            driver = createChromeDriver()
            driver.get(creditCardUrl)
            Thread.sleep(3000)

            println("======= [신한] 신용 카드 정보 크롤링 =======")

            val html = driver.pageSource
            val soup = Jsoup.parse(html)

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
                                val fullUrl = "https://www.shinhancard.com/pconts/html/card/apply/credit/$cardUrl"

                                val imgElement = element.select("img").first()
                                val imgSrc = imgElement?.attr("src") ?: ""
                                val imgUrl = if (imgSrc.startsWith("http")) imgSrc else "https://www.shinhancard.com$imgSrc"

                                cards.add(
                                    CardInfo(
                                        cardId = null,
                                        cardUrl = fullUrl,
                                        cardCompany = CardCompany.SHINHAN,
                                        cardName = cardName,
                                        imgUrl = imgUrl,
                                        cardType = CardType.CREDIT,
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            println("신한카드 크롤링 중 오류: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("신한카드 메인 페이지 크롤링 중 오류: ${e.message}")
        } finally {
            driver?.quit()
        }

        return cards
    }

    override fun crawlCheckCardBasicInfos(): List<CardInfo> {
        val cards = mutableListOf<CardInfo>()
        var driver: WebDriver? = null

        try {
            driver = createChromeDriver()
            driver.get(checkCardUrl)
            Thread.sleep(3000)

            println("======= [신한] 체크 카드 정보 크롤링 =======")

            val html = driver.pageSource
            val soup = Jsoup.parse(html)

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
                                val fullUrl = "https://www.shinhancard.com/pconts/html/card/apply/check/$cardUrl"


                                val imgElement = element.select("img").first()
                                val imgSrc = imgElement?.attr("src") ?: ""
                                val imgUrl = if (imgSrc.startsWith("http")) imgSrc else "https://www.shinhancard.com$imgSrc"

                                cards.add(
                                    CardInfo(
                                        cardId = null,
                                        cardUrl = fullUrl,
                                        cardCompany = CardCompany.SHINHAN,
                                        cardName = cardName,
                                        imgUrl = imgUrl,
                                        cardType = CardType.CHECK,
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            println("신한 체크카드 크롤링 중 오류: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("신한 체크카드 메인 페이지 크롤링 중 오류: ${e.message}")
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

    override fun crawlCreditCardBenefits(cards: List<CardInfo>): List<CardInfo> {
        return cards.map { card ->
            val benefits = crawlCardBenefits(card.cardUrl, CardType.CREDIT)
            println("${card.cardName}: $benefits")
            card.copy(benefits = benefits)
        }
    }

    override fun crawlCheckCardBenefits(cards: List<CardInfo>): List<CardInfo> {
        return cards.map { card ->
            val benefits = crawlCardBenefits(card.cardUrl, CardType.CHECK)
            println("${card.cardName}: $benefits")
            card.copy(benefits = benefits)
        }
    }

    private fun crawlCardBenefits(cardUrl: String, cardType: CardType): String {
        var driver: WebDriver? = null
        try {
            driver = createChromeDriver()


            driver.get(cardUrl)
            Thread.sleep(3000)

            val html = driver.pageSource
            val soup = Jsoup.parse(html)
            val benefit = StringBuilder()

            // benefit_cont_wrap 구조 처리
            val benefitContWraps = soup.select("div.benefit_cont_wrap")
            if (benefitContWraps.isNotEmpty()) {
                for (wrap in benefitContWraps) {
                    benefit.append("**${wrap.text().trim()}**\n")
                }
            } else {
                println("[$cardUrl] 혜택 정보 없음")
                return "혜택 정보 없음"
            }

            return benefit.toString().trim()
        } catch (e: Exception) {
            println("신한카드 혜택 크롤링 중 오류: ${e.message}")
            return "혜택 정보 없음"
        } finally {
            driver?.quit()
        }
    }
}
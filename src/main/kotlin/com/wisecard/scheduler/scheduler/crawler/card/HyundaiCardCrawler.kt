package com.wisecard.scheduler.scheduler.crawler.card

import com.wisecard.scheduler.scheduler.dto.CardCompany
import com.wisecard.scheduler.scheduler.dto.CardInfo
import com.wisecard.scheduler.scheduler.dto.CardType
import io.github.bonigarcia.wdm.WebDriverManager
import org.jsoup.Jsoup
import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class HyundaiCardCrawler : CardCrawler {
    override val cardCompany = CardCompany.HYUNDAI

    private val creditCardUrl = "https://www.hyundaicard.com/cpc/ma/CPCMA0101_01.hc?cardflag=ALL"
    private val checkCardUrl = "https://www.hyundaicard.com/cpc/cr/CPCCR0621_11.hc?cardflag=C#aTab_2"

    private val specialCardUrls = mapOf(
        "기아" to Pair(
            "https://www.hyundaicard.com/cpc/cr/CPCCR0621_11.hc?cardflag=K#aTab_1",
            "https://www.hyundaicard.com/cpc/cr/CPCCR0201_01.hc?cardflag=K&cardWcd="
        ),
        "현대자동차" to Pair(
            "https://www.hyundaicard.com/cpc/cr/CPCCR0621_11.hc?cardflag=H",
            "https://www.hyundaicard.com/cpc/cr/CPCCR0201_01.hc?cardflag=H&cardWcd="
        ),
        "대한항공" to Pair(
            "https://www.hyundaicard.com/cpc/cr/CPCCR0621_11.hc?cardflag=D",
            "https://www.hyundaicard.com/cpc/cr/CPCCR0201_01.hc?cardflag=D&cardWcd="
        ),
        "American Express" to Pair(
            "https://www.hyundaicard.com/cpc/ma/CPCMA0101_01.hc?cardflag=AX",
            "https://www.hyundaicard.com/cpc/cr/CPCCR0201_01.hc?cardWcd="
        ),
        "이마트" to Pair(
            "https://www.hyundaicard.com/cpc/cr/CPCCR0621_11.hc?cardflag=E",
            "https://www.hyundaicard.com/cpc/cr/CPCCR0201_01.hc?cardflag=E&cardWcd="
        ),
        "지마켓(스마일카드)" to Pair(
            "https://www.hyundaicard.com/cpc/cr/CPCCR0621_11.hc?cardflag=S",
            "https://www.hyundaicard.com/cpc/cr/CPCCR0201_01.hc?cardflag=S&cardWcd="
        ),
        "코스트코" to Pair(
            "https://www.hyundaicard.com/cpc/cr/CPCCR0621_11.hc?cardflag=T",
            "https://www.hyundaicard.com/cpc/cr/CPCCR0201_01.hc?cardflag=T&cardWcd="
        ),
        "미래에셋증권" to Pair(
            "https://www.hyundaicard.com/cpc/cr/CPCCR0621_11.hc?cardflag=MA",
            "https://www.hyundaicard.com/cpc/cr/CPCCR0201_01.hc?cardflag=MA&cardWcd="
        ),
        "넥슨" to Pair(
            "https://www.hyundaicard.com/cpc/cr/CPCCR0621_11.hc?cardflag=N",
            "https://www.hyundaicard.com/cpc/cr/CPCCR0201_01.hc?cardflag=MA&cardWcd="
        ),
        "KT" to Pair(
            "https://www.hyundaicard.com/cpc/cr/CPCCR0621_11.hc?cardflag=J&ctgrCd=050401",
            "https://www.hyundaicard.com/cpc/cr/CPCCR0201_01.hc?cardflag=MA&cardWcd="
        ),
        "SKT" to Pair(
            "https://www.hyundaicard.com/cpc/cr/CPCCR0621_11.hc?cardflag=J&ctgrCd=050403",
            "https://www.hyundaicard.com/cpc/cr/CPCCR0201_01.hc?cardflag=MA&cardWcd="
        ),
        "SC제일은행" to Pair(
            "https://www.hyundaicard.com/cpc/cr/CPCCR0621_11.hc?cardflag=J&ctgrCd=050703",
            "https://www.hyundaicard.com/cpc/cr/CPCCR0201_01.hc?cardflag=MA&cardWcd="
        ),
        "경차" to Pair(
            "https://www.hyundaicard.com/cpc/cr/CPCCR0621_11.hc?cardflag=J&ctgrCd=050802",
            "https://www.hyundaicard.com/cpc/cr/CPCCR0201_01.hc?cardflag=MA&cardWcd="
        ),
        "화물차" to Pair(
            "https://www.hyundaicard.com/cpc/cr/CPCCR0621_11.hc?cardflag=J&ctgrCd=050803",
            "https://www.hyundaicard.com/cpc/cr/CPCCR0201_01.hc?cardflag=MA&cardWcd="
        )
    )

    private val skipCardNames = setOf("체크카드", "Gift카드", "후불하이패스카드")
    private val specialCardNames = setOf(
        "쏘카", "제네시스", "야놀자ㆍ인터파크(NOL 카드)", "무신사", "SSG.COM", "네이버", "배달의민족",
        "스타벅스", "GS칼텍스", "LG U+", "기타", "현대홈쇼핑", "예스24", "하이마트", "The CJ",
        "coway-현대카드M Edition3", "LG전자-현대카드M Edition3", "햇살론", "인플카 현대카드"
    )

    private val myBusinessCards = setOf(
        "MY BUSINESS M Food&Drink", "MY BUSINESS M Retail&Service", "MY BUSINESS M Online Seller",
        "MY BUSINESS X Food&Drink", "MY BUSINESS X Retail&Service", "MY BUSINESS X Online Seller",
        "MY BUSINESS ZERO Food&Drink", "MY BUSINESS ZERO Retail&Service", "MY BUSINESS ZERO Online Seller"
    )

    private fun setupChromeDriver(): ChromeDriver {
        val options = ChromeOptions()
        options.addArguments("--headless=new")
        options.addArguments("--disable-gpu")
        options.addArguments("--no-sandbox")
        options.addArguments("--disable-dev-shm-usage")
        options.addArguments("--disable-web-security")
        WebDriverManager.chromedriver().setup()
        return ChromeDriver(options).apply {
            manage().timeouts().implicitlyWait(Duration.ofSeconds(20))
            manage().timeouts().pageLoadTimeout(Duration.ofSeconds(300))
        }
    }

    private fun removeBlank(text: String): String {
        return text.split("\\s+".toRegex()).joinToString(" ")
    }

    override fun crawlCreditCardBasicInfos(): List<CardInfo> {
        println("======= [현대] 신용 카드 기본 정보 크롤링 =======")
        return crawlCardBasics(CardType.CREDIT)
    }

    override fun crawlCheckCardBasicInfos(): List<CardInfo> {
        println("======= [현대] 체크 카드 기본 정보 크롤링 =======")
        return crawlCardBasics(CardType.CHECK)
    }

    override fun crawlCreditCardBenefits(cards: List<CardInfo>): List<CardInfo> {
        println("======= [현대] 신용 카드 혜택 정보 크롤링 =======")
        return crawlCardBenefits(cards)
    }

    override fun crawlCheckCardBenefits(cards: List<CardInfo>): List<CardInfo> {
        println("======= [현대] 체크 카드 혜택 정보 크롤링 =======")
        return crawlCardBenefits(cards)
    }

    private fun crawlCardBasics(cardType: CardType): List<CardInfo> {
        val driver = setupChromeDriver()
        val cardInfos = mutableListOf<CardInfo>()
        val url = if (cardType == CardType.CREDIT) creditCardUrl else checkCardUrl

        try {
            driver.get(url)
            Thread.sleep(3000)

            val soup = Jsoup.parse(driver.pageSource!!)
            val cardSections = soup.select("ul.list05")

            for (section in cardSections) {
                val listElements = section.select("div.card_plt")
                for (element in listElements) {
                    val cardNameElement = element.selectFirst("span.h4_b_lt") ?: continue
                    val cardName = cardNameElement.text().trim()

                    if (cardName in skipCardNames) continue

                    if (cardName in specialCardUrls) {
                        cardInfos.addAll(extractCardBasicsFromSpecialUrl(driver, cardName))
                        continue
                    }

                    val imgUrl = element.selectFirst("img")?.attr("src") ?: ""
                    val cardUrl = if (cardType == CardType.CREDIT) {
                        val cardCodeA = element.selectFirst("a") ?: continue
                        val onclick = cardCodeA.attr("onclick")
                        val cardUrlCode = if (onclick.isNotEmpty()) {
                            Regex("goCardDetail\\('([^']+)'\\)").find(onclick)?.groupValues?.get(1) ?: ""
                        } else {
                            ""
                        }
                        "https://www.hyundaicard.com/cpc/cr/CPCCR0201_01.hc?cardWcd=$cardUrlCode"
                    } else {
                        val cardUrlCode = imgUrl.takeLast(9).substring(0, 3)
                        "https://www.hyundaicard.com/cpc/cr/CPCCR0201_01.hc?cardflag=C&cardWcd=$cardUrlCode&eventCode=00000"
                    }

                    cardInfos.add(
                        CardInfo(
                            cardUrl = cardUrl,
                            cardCompany = CardCompany.HYUNDAI,
                            cardName = cardName,
                            imgUrl = imgUrl,
                            cardType = cardType
                        )
                    )
                }
            }
        } catch (e: Exception) {
            println("크롤링 실패 ${e.message}")
        } finally {
            driver.quit()
        }

        return cardInfos
    }

    private fun extractCardBasicsFromSpecialUrl(
        driver: ChromeDriver,
        cardName: String
    ): List<CardInfo> {
        val (listUrl, cardUrlPrefix) = specialCardUrls[cardName] ?: return emptyList()
        val cardInfos = mutableListOf<CardInfo>()

        try {
            driver.get(listUrl)
            Thread.sleep(3000)
            val soup = Jsoup.parse(driver.pageSource!!)
            val ulList = soup.select("ul.list05")

            for (ul in ulList) {
                val liList = ul.select("li")
                for (li in liList) {
                    val nameEl = li.selectFirst("span.h4_b_lt")?.text()?.trim() ?: continue
                    val cardDiv = li.selectFirst("div.card_plt") ?: continue
                    val aTag = cardDiv.selectFirst("a") ?: continue
                    val onclick = aTag.attr("onclick")
                    val match = Regex("goCardDetail\\('([^']+)'\\)").find(onclick)
                    val cardCode = match?.groupValues?.get(1) ?: continue

                    cardInfos.add(
                        CardInfo(
                            cardId = null,
                            cardUrl = "${cardUrlPrefix}${cardCode}&eventCode=00000",
                            cardCompany = CardCompany.HYUNDAI,
                            cardName = nameEl,
                            imgUrl = null,
                            cardType = CardType.CREDIT,
                            benefits = null
                        )
                    )
                }
            }
        } catch (e: Exception) {
            println("크롤링 실패 : ${e.message}")
        }

        return cardInfos
    }

    private fun crawlCardBenefits(cards: List<CardInfo>): List<CardInfo> {
        val driver = setupChromeDriver()
        val result = mutableListOf<CardInfo>()
        val excludeIds = setOf(
            "popup_call_reservation", "cardApplyPop", "popCardSelect", "popCardUse",
            "footerFamilySite", "popDisSS", "popQrCode", "popMemberFee"
        )

        try {
            cards.forEachIndexed { index, cardInfo ->
                try {
                    driver.get(cardInfo.cardUrl)
                    WebDriverWait(driver, Duration.ofSeconds(30)).until(
                        ExpectedConditions.presenceOfElementLocated(By.className("modal_pop"))
                    )
                    val soup = Jsoup.parse(driver.pageSource!!)
                    val popupContainers = soup.select("div.modal_pop")
                    val texts = mutableListOf<String>()
                    val textTags = listOf("h1", "h2", "h3", "h4", "h5", "h6", "p")
                    val allTags = textTags + "li"

                    for (container in popupContainers) {
                        val containerId = container.attr("id")
                        if (containerId in excludeIds) continue

                        val elements = container.select(allTags.joinToString(",")).filter { el ->
                            if (el.tagName() == "li") {
                                el.select(textTags.joinToString(",")).isEmpty()
                            } else {
                                true
                            }
                        }

                        texts.addAll(elements.map { it.text().trim() }.filter { it.isNotEmpty() })
                    }

                    result.add(
                        cardInfo.copy(
                            benefits = removeBlank(texts.joinToString(" | "))
                        )
                    )
                    println(cardInfo.cardName + texts)
                } catch (e: Exception) {
                    println("크롤링 실패 ${cardInfo.cardName}: ${e.message}")
                    result.add(cardInfo.copy(benefits = ""))
                }
            }
        } finally {
            driver.quit()
        }
        return result
    }
}
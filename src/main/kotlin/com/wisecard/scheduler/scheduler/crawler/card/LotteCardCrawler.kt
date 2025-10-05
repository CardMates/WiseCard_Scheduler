package com.wisecard.scheduler.scheduler.crawler.card

import com.wisecard.scheduler.scheduler.dto.CardCompany
import com.wisecard.scheduler.scheduler.dto.CardInfo
import com.wisecard.scheduler.scheduler.dto.CardType
import org.jsoup.Jsoup
import org.springframework.stereotype.Component
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class LotteCardCrawler : CardCrawler {
    override val cardCompany = CardCompany.LOTTE

    private val creditListUrl = "https://www.lottecard.co.kr/app/LPCDADA_V100.lc"
    private val checkListUrl  = "https://www.lottecard.co.kr/app/LPCDAEA_V100.lc"
    private val detailBaseUrl = "https://www.lottecard.co.kr/app/LPCDADB_V100.lc?vtCdKndC="

    private val nameSet = mutableSetOf<String>()

    override fun crawlCreditCardBasicInfos(): List<CardInfo> {
        return crawlBasicInfos(creditListUrl, CardType.CREDIT)
    }

    override fun crawlCheckCardBasicInfos(): List<CardInfo> {
        return crawlBasicInfos(checkListUrl, CardType.CHECK)
    }

    private fun crawlBasicInfos(listUrl: String, cardType: CardType): List<CardInfo> {
        val cards = mutableListOf<CardInfo>()
        val options = ChromeOptions()
        options.addArguments("--headless")
        val driver: WebDriver = ChromeDriver(options)
        driver.get(listUrl)

        // 일반 + 제휴 카드 반복 처리
        val tabs = listOf("일반", "제휴")
        for (tab in tabs) {
            if (tab == "제휴") {
                try {
                    val link = driver.findElement(By.linkText("제휴"))

                    (driver as org.openqa.selenium.JavascriptExecutor)
                        .executeScript("arguments[0].scrollIntoView(true);", link)
                    Thread.sleep(500)

                    (driver as org.openqa.selenium.JavascriptExecutor)
                        .executeScript("arguments[0].click();", link)
                    Thread.sleep(5000)
                } catch (e: Exception) {
                    println("제휴 탭 없음: ${e.message}")
                    continue
                }
            }

            // "더보기" 버튼 끝까지 클릭
            while (true) {
                val btnMore = driver.findElements(By.id("btnMore"))
                if (btnMore.isEmpty()) break
                btnMore[0].click()
                Thread.sleep(3000)
            }

            val soup = Jsoup.parse(driver.pageSource)
            val cardElements = soup.select("ul#ajaxCardList li")

            for (card in cardElements) {
                try {
                    val onclick = card.selectFirst("a")?.attr("onclick") ?: continue
                    val cardNo = Regex("'(.*?)'").find(onclick)?.groupValues?.get(1) ?: continue
                    val cardUrl = "$detailBaseUrl$cardNo"

                    val cardName = card.selectFirst("b")?.text()?.trim() ?: continue
                    if (nameSet.contains(cardName)) continue
                    nameSet.add(cardName)

                    val imgSrc = card.selectFirst("img")?.attr("src") ?: ""
                    val imgUrl = if (imgSrc.startsWith("http")) imgSrc else "https:$imgSrc"

                    cards.add(
                        CardInfo(
                            cardId = null,
                            cardUrl = cardUrl,
                            cardCompany = CardCompany.LOTTE,
                            cardName = cardName,
                            imgUrl = imgUrl,
                            cardType = cardType
                        )
                    )
                    println("${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))} [$cardName] --- 기본 정보 수집 완료 ($tab)")
                } catch (e: Exception) {
                    println("롯데카드 기본 정보 크롤링 오류: ${e.message}")
                }
            }
        }

        driver.quit()
        return cards
    }

    override fun crawlCreditCardBenefits(cards: List<CardInfo>): List<CardInfo> {
        return cards.map { card ->
            val benefits = crawlCardBenefits(card.cardUrl)
            println(card.cardName+benefits)
            card.copy(benefits = benefits)
        }
    }

    override fun crawlCheckCardBenefits(cards: List<CardInfo>): List<CardInfo> {
        return cards.map { card ->
            val benefits = crawlCardBenefits(card.cardUrl)
            println(card.cardName+benefits)
            card.copy(benefits = benefits)
        }
    }

    private fun crawlCardBenefits(cardUrl: String): String {
        return try {
            val driver: WebDriver = ChromeDriver(ChromeOptions().addArguments("--headless"))
            driver.get(cardUrl)
            Thread.sleep(3000)

            val doc = Jsoup.parse(driver.pageSource)
            driver.quit()

            val sb = StringBuilder()

            val benes = doc.select("div.bnfCont")
            if (benes.isNotEmpty()) {
                for (bene in benes) {
                    val title = bene.selectFirst("h3")?.text() ?: continue
                    if (title in listOf("L.POINT", "가족카드", "가족카드 안내", "연회비", "혜택 모아보기")) continue
                    sb.append("<$title> ")

                    val sections = bene.select("div.toggle")
                    for (section in sections) {
                        val subTitle = section.selectFirst("h4")?.text() ?: continue
                        sb.append("[$subTitle] ")

                        val details = section.select("div.toggleCont").first()?.children() ?: continue
                        for (detail in details) {
                            when {
                                detail.tagName() == "table" -> sb.append(detail.outerHtml())
                                detail.tagName() == "h3" -> sb.append("[${detail.text()}]")
                                detail.tagName() == "h4" -> sb.append("/${detail.text()}: ")
                                detail.tagName().startsWith("style") -> continue
                                else -> sb.append(detail.text().replace("\n","").replace("\r","").replace("\t",""))
                            }
                        }
                    }
                    sb.append("\n")
                }
            } else {
                val toggleList = doc.selectFirst("ul.toggleList")
                toggleList?.select("li")?.forEach { li ->
                    val title = li.selectFirst("a")?.text()?.replace("\n","")?.replace("\t","")?.replace(" ","") ?: ""
                    if (title in listOf("L.POINT", "가족카드", "가족카드안내", "연회비")) return@forEach
                    sb.append("<$title> ")

                    li.select("div.toggleCont").first()?.children()?.forEach { detail ->
                        when {
                            detail.tagName() == "table" -> sb.append(detail.outerHtml())
                            detail.tagName() == "h3" -> sb.append("[${detail.text()}]")
                            detail.tagName() == "h4" -> sb.append("/${detail.text()}: ")
                            detail.tagName().startsWith("style") -> return@forEach
                            else -> sb.append(detail.text().replace("\n","").replace("\t","").replace("\r",""))
                        }
                    }
                    sb.append("\n")
                }
            }
            sb.toString().trim()
        } catch (e: Exception) {
            println("롯데카드 혜택 크롤링 실패: ${e.message}")
            ""
        }
    }
}
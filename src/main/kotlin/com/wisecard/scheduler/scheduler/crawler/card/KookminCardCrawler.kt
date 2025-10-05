package com.wisecard.scheduler.scheduler.crawler.card

import com.wisecard.scheduler.scheduler.dto.CardCompany
import com.wisecard.scheduler.scheduler.dto.CardInfo
import com.wisecard.scheduler.scheduler.dto.CardType
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.springframework.stereotype.Component
import java.net.URL

@Component
class KookminCardCrawler : CardCrawler {
    override val cardCompany = CardCompany.KOOKMIN

    private val creditCardUrl = "https://card.kbcard.com/CRD/DVIEW/HCAMCXPRICAC0047?pageNo=1&cateIdx="
    private val checkCardUrl = "https://card.kbcard.com/CRD/DVIEW/HCAMCXPRICAC0056?pageNo=1&cateIdx="

    private val nameList = mutableSetOf<String>()

    override fun crawlCreditCardBasicInfos(): List<CardInfo> {
        println("======= [국민] 신용 카드 기본 정보 크롤링 =======")
        val cards = mutableListOf<CardInfo>()
        for (i in 1..11) {
            val url = "$creditCardUrl$i"
            cards += crawlCategoryPageBasic(url, CardType.CREDIT)
        }
        return cards
    }

    override fun crawlCheckCardBasicInfos(): List<CardInfo> {
        println("======= [국민] 체크 카드 기본 정보 크롤링 =======")
        val cards = mutableListOf<CardInfo>()
        for (i in 1..11) {
            val url = "$checkCardUrl$i"
            cards += crawlCategoryPageBasic(url, CardType.CHECK)
        }
        return cards
    }

    private fun crawlCategoryPageBasic(url: String, cardType: CardType): List<CardInfo> {
        val cards = mutableListOf<CardInfo>()
        try {
            val doc = Jsoup.parse(URL(url).readText())
            val links = doc.select("div.card-box__before a[onclick]")

            for (link in links) {
                try {
                    val onclick = link.attr("onclick")
                    val numbers = onclick.substring(21, 26)
                    val cardDetailUrl = "https://card.kbcard.com/CRD/DVIEW/HCAMCXPRICAC0076?mainCC=a&cooperationcode=$numbers"

                    val detailDoc = Jsoup.parse(URL(cardDetailUrl).readText())
                    val cardName = detailDoc.selectFirst("h1.tit")?.text()?.trim() ?: continue

                    if (nameList.contains(cardName)) continue
                    nameList.add(cardName)

                    val imgSrc = detailDoc.selectFirst("div.cardBoxInner img")?.attr("src") ?: ""
                    val imgUrl = if (imgSrc.startsWith("http")) imgSrc else "https://card.kbcard.com$imgSrc"

                    cards.add(
                        CardInfo(
                            cardId = null,
                            cardUrl = cardDetailUrl,
                            cardCompany = CardCompany.KOOKMIN,
                            cardName = cardName,
                            imgUrl = imgUrl,
                            cardType = cardType,
                        )
                    )
                } catch (e: Exception) {
                    println("카드 상세 크롤링 오류: ${e.message}")
                }
            }

        } catch (e: Exception) {
            println("카테고리 페이지 접속 오류: ${e.message}")
        }
        return cards
    }

    override fun crawlCreditCardBenefits(cards: List<CardInfo>): List<CardInfo> {
        return cards.map { card ->
            val benefit = parseCardBenefitsFromUrl(card.cardUrl)
            println(card.cardName+benefit)
            card.copy(benefits = benefit)
        }
    }

    override fun crawlCheckCardBenefits(cards: List<CardInfo>): List<CardInfo> {
        return cards.map { card ->
            val benefit = parseCardBenefitsFromUrl(card.cardUrl)
            println(card.cardName+benefit)
            card.copy(benefits = benefit)
        }
    }

    private fun parseCardBenefitsFromUrl(cardUrl: String): String {
        return try {
            val fullUrl = if (cardUrl.startsWith("http")) cardUrl else "https://card.kbcard.com$cardUrl"
            val doc = Jsoup.parse(URL(fullUrl).readText())
            parseCardBenefits(doc)
        } catch (e: Exception) {
            println("카드 혜택 크롤링 중 오류: ${e.message}")
            ""
        }
    }

    private fun parseCardBenefits(doc: org.jsoup.nodes.Document): String {
        val sb = StringBuilder()

        // 상세혜택 id 결정
        val topTab = doc.selectFirst("li#topTab1")?.text()?.replace("\n", "")?.replace(" ", "")?.replace("\t", "")
        val tabId = if (topTab == "상세혜택") "tabCon01" else "tabCon02"

        val summary = doc.selectFirst("div.tabMulti.marT20.multiLine2")
        if (summary != null) {
            val contents = doc.selectFirst("div#$tabId")
            contents?.select("div[id^=$tabId]")?.forEach { sb.append(parseBenefitDetail(it)) }
        } else {
            val contents = doc.selectFirst("div#$tabId")
            if (contents != null) sb.append(parseBenefitDetail(contents))
        }

        return sb.toString().replace("'", "").replace("\t", "").trim()
    }

    private fun parseBenefitDetail(element: Element): String {
        val sb = StringBuilder()

        for (child in element.children()) {
            val tagName = child.tagName()
            if (tagName == "h2" || tagName.startsWith("style")) continue

            if (child.id().startsWith("tabCon01")) {
                child.children().forEach { d ->
                    if (d.tagName() == "h2" || d.tagName().startsWith("style")) return@forEach
                    if (d.className().contains("benefitBox1 marT")) {
                        d.children().forEach { c ->
                            if (c.tagName().startsWith("style")) return@forEach
                            if (c.className().contains("titArea")) sb.append(parseBenefitMiniTitle(c))
                            else sb.append(findTableOrText(c))
                        }
                    } else {
                        if (d.className().contains("titArea")) sb.append(parseBenefitMiniTitle(d))
                        else sb.append(findTableOrText(d))
                    }
                }
            } else if (child.className().contains("benefitBox1 marT")) {
                child.children().forEach { c ->
                    if (c.tagName().startsWith("style")) return@forEach
                    if (c.className().contains("titArea")) sb.append(parseBenefitMiniTitle(c))
                    else sb.append(findTableOrText(c))
                }
            } else {
                if (child.className().contains("titArea")) sb.append(parseBenefitMiniTitle(child))
                else sb.append(findTableOrText(child))
            }
        }

        return sb.toString()
    }

    private fun parseBenefitMiniTitle(element: Element): String {
        val sb = StringBuilder()
        val title = element.selectFirst("div.tit")?.text()?.trim()
        var content = ""

        element.children().forEach { child ->
            val text = findTableOrText(child)
            if (text != title) content += text
        }

        if (title != null) sb.append("[$title] ")
        sb.append(content.replace("\n", ""))
        return sb.toString()
    }

    private fun findTableOrText(element: Element): String {
        return if (element.tagName() == "table") element.outerHtml().replace("\n", "")
        else element.text().replace("\n", "")
    }
}
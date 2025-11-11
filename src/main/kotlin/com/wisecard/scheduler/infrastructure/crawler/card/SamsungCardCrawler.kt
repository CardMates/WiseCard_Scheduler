package com.wisecard.scheduler.infrastructure.crawler.card

import com.wisecard.scheduler.domain.card.CardCompany
import com.wisecard.scheduler.domain.card.CardInfo
import com.wisecard.scheduler.domain.card.CardType
import com.wisecard.scheduler.util.LoggerUtils.logCrawlingError
import com.wisecard.scheduler.util.LoggerUtils.logCrawlingStart
import com.wisecard.scheduler.util.logger
import org.jsoup.Jsoup
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.regex.Pattern

@Component
class SamsungCardCrawler : CardCrawler {

    override val cardCompany = CardCompany.SAMSUNG

    private val creditCardUrl =
        "https://www.samsungcard.com/home/card/cardinfo/PGHPPDCCardCardinfoRecommendPC001"
    private val checkCardUrl =
        "https://www.samsungcard.com/home/card/cardinfo/PGHPPCCCardCardinfoCheckcard001"

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
        return cards.mapIndexed { _, card ->
            val benefits = crawlCardBenefits(card.cardUrl)
            card.copy(
                benefits = benefits
            )
        }
    }

    override fun crawlCheckCardBenefits(cards: List<CardInfo>): List<CardInfo> {
        logCrawlingStart(cardCompany, "체크 카드 혜택 정보")
        return cards.mapIndexed { _, card ->
            val benefits = crawlCardBenefits(card.cardUrl)
            card.copy(
                benefits = benefits,
            )
        }
    }

    private fun crawlCardList(url: String, type: CardType): List<CardInfo> {
        val driver = createChromeDriver()
        val cards = mutableListOf<CardInfo>()

        try {
            driver.get(url)
            val cssSelector = if (type == CardType.CREDIT) "div.tab-section" else "ul.lists"
            WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(cssSelector)))

            val soup = Jsoup.parse(driver.pageSource!!)

            if (type == CardType.CREDIT) {
                val tabSections = soup.select("div.tab-section")

                for (i in 2 until tabSections.size) {
                    val ulTag = tabSections[i].selectFirst("ul.lists") ?: continue
                    val listElements = ulTag.select("li")

                    for (element in listElements) {
                        try {
                            val cardName = element.selectFirst("div.tit-h4")?.text()?.trim() ?: "Unknown"
                            val cardImg = element.selectFirst("img")?.attr("src") ?: ""

                            val pattern = Pattern.compile("ABP(\\d{4})")
                            val matcher = if (cardImg.isNotEmpty()) pattern.matcher(cardImg) else null
                            val cardUrlCode = if (matcher?.find() == true) {
                                matcher.group(0)
                            } else {
                                if (cardImg.length >= 11) cardImg.substring(
                                    cardImg.length - 11,
                                    cardImg.length - 4
                                ) else ""
                            }

                            val cardUrl =
                                "https://www.samsungcard.com/home/card/cardinfo/PGHPPCCCardCardinfoDetails001?code=$cardUrlCode"

                            cards.add(
                                CardInfo(
                                    cardId = null,
                                    cardUrl = cardUrl,
                                    cardCompany = CardCompany.SAMSUNG,
                                    cardName = cardName,
                                    imgUrl = if (cardImg.startsWith("http")) cardImg else "https://www.samsungcard.com$cardImg",
                                    cardType = type
                                )
                            )
                        } catch (e: Exception) {
                            logger.error("삼성카드 혜택 크롤링 에러: ${e.message}")
                        }
                    }
                }
                val uniqueCards = cards.distinctBy { it.cardUrl }.toMutableList()
                cards.clear()
                cards.addAll(uniqueCards)
            } else {
                val ulTag = soup.selectFirst("ul.lists") ?: return emptyList()
                val listElements = ulTag.select("li")

                for (element in listElements) {
                    try {
                        val cardName = element.selectFirst("div.tit-h6")?.text()?.trim() ?: "Unknown"
                        val imgElements = element.select("img")
                        if (imgElements.size < 2) continue
                        val cardImg = imgElements[1].attr("src")

                        val cardUrlCode = if (cardImg.length >= 8) cardImg.takeLast(8).dropLast(4) else ""
                        val cardUrl =
                            "https://www.samsungcard.com/home/card/cardinfo/PGHPPCCCardCardinfoDetails001?code=ABP$cardUrlCode"

                        cards.add(
                            CardInfo(
                                cardId = null,
                                cardUrl = cardUrl,
                                cardCompany = CardCompany.SAMSUNG,
                                cardName = cardName,
                                imgUrl = if (cardImg.startsWith("http")) cardImg else "https://www.samsungcard.com$cardImg",
                                cardType = type
                            )
                        )
                    } catch (e: Exception) {
                        logCrawlingError(cardCompany, "카드 혜택", e)
                    }
                }
            }
        } catch (e: Exception) {
            logCrawlingError(cardCompany, "카드 혜택", e)
        } finally {
            driver.quit()
        }

        return cards
    }

    private fun crawlCardBenefits(cardUrl: String): String {
        val driver = createChromeDriver()
        val benefits = StringBuilder()

        try {
            driver.get(cardUrl)
            WebDriverWait(driver, Duration.ofSeconds(30))
                .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("body, article.swiper, button.benefit-content")))

            val js = driver as JavascriptExecutor
            val wait = WebDriverWait(driver, Duration.ofSeconds(30))

            val soup = Jsoup.parse(driver.pageSource!!)
            val swiperSlides = soup.select("article.swiper div.swiper-slide")

            swiperSlides.forEachIndexed { i, slide ->
                try {
                    val tabName = slide.selectFirst("p.wcms-tit-h3")?.text()?.trim() ?: "탭 ${i + 1}"

                    val contentArea = slide.selectFirst("div.swiper-inner-content") ?: return@forEachIndexed

                    val textBlocks = mutableSetOf<String>()
                    contentArea.select("p,h3,h4,h5,li,span,strong,td,th")
                        .mapNotNull {
                            it.text().trim()
                                .takeIf { txt -> txt.length > 2 && !txt.contains("컨택리스", ignoreCase = true) }
                        }
                        .forEach { textBlocks.add(it) }

                    val tables = mutableSetOf<String>()
                    contentArea.select("table").forEach { table ->
                        val caption = table.selectFirst("caption")?.text()?.trim() ?: ""
                        val rows = table.select("tr").joinToString(" | ") { row ->
                            row.select("td,th").joinToString(" | ") { cell -> cell.text().trim() }
                        }
                        val tableText = listOfNotNull(caption.takeIf { it.isNotEmpty() }, rows).joinToString(" | ")
                        if (tableText.isNotEmpty()) tables.add(tableText)
                    }

                    if (textBlocks.isEmpty() && tables.isEmpty()) {
                        return@forEachIndexed
                    }

                    val joinedText = (textBlocks + tables).joinToString("\n")
                    benefits.append("\n<$tabName>\n").append(joinedText).append("\n")
                } catch (e: Exception) {
                    logCrawlingError(cardCompany, "카드 혜택", e)
                }
            }

            if (benefits.isEmpty()) {
                val buttons = driver.findElements(By.cssSelector("button.benefit-content"))

                if (buttons.isNotEmpty()) {
                    val firstButton = buttons[0]
                    val tabName = firstButton.text.trim().ifEmpty { "첫 번째 탭" }
                    js.executeScript("arguments[0].scrollIntoView({block: 'center'});", firstButton)
                    js.executeScript("arguments[0].click();", firstButton)

                    try {
                        wait.until(
                            ExpectedConditions.presenceOfElementLocated(
                                By.cssSelector("div[class*='modal'], div[class*='benefit'], div.swiper-inner-content, div.card-benefit-cont, div.wcms-cont, div.modal-content, div.benefit-details, div.benefit-info, div.popup-content")
                            )
                        )
                    } catch (_: TimeoutException) {
                    }

                    val modalSoup = Jsoup.parse(driver.pageSource!!)
                    val modalContent =
                        modalSoup.selectFirst("div[class*='modal'], div[class*='benefit'], div.swiper-inner-content, div.card-benefit-cont, div.wcms-cont, div.modal-content, div.benefit-details, div.benefit-info, div.popup-content")
                    if (modalContent != null) {
                        val textBlocks = mutableSetOf<String>()
                        modalContent.select("p,h3,h4,h5,li,span,strong,td,th")
                            .mapNotNull {
                                it.text().trim()
                                    .takeIf { txt -> txt.length > 2 && !txt.contains("컨택리스", ignoreCase = true) }
                            }
                            .forEach { textBlocks.add(it) }

                        val tables = mutableSetOf<String>()
                        modalContent.select("table").forEach { table ->
                            val caption = table.selectFirst("caption")?.text()?.trim() ?: ""
                            val rows = table.select("tr").joinToString(" | ") { row ->
                                row.select("td,th").joinToString(" | ") { cell -> cell.text().trim() }
                            }
                            val tableText = listOfNotNull(caption.takeIf { it.isNotEmpty() }, rows).joinToString(" | ")
                            if (tableText.isNotEmpty()) tables.add(tableText)
                        }

                        if (textBlocks.isNotEmpty() || tables.isNotEmpty()) {
                            val joinedText = (textBlocks + tables).joinToString("\n")
                            benefits.append("\n<$tabName>\n").append(joinedText).append("\n")
                        }
                    }

                    try {
                        val closeButton =
                            driver.findElement(By.cssSelector("i.ico.common-close, button.close, i.close, button.modal-close"))
                        js.executeScript("arguments[0].click();", closeButton)
                        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("i.ico.common-close, button.close, i.close, button.modal-close")))
                    } catch (_: Exception) {
                    }
                }
            }
        } catch (e: Exception) {
            logCrawlingError(cardCompany, "카드 혜택", e)
        } finally {
            driver.quit()
        }

        val benefitText = benefits.toString().ifEmpty { "혜택 정보 없음" }
        return benefitText
    }

    private fun createChromeDriver(): WebDriver {
        val options = ChromeOptions()
        options.addArguments(
            "--headless=new",
            "--disable-gpu",
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--window-size=1920,1080"
        )
        val driver = ChromeDriver(options)
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10))
        return driver
    }
}
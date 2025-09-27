package com.wisecard.scheduler.scheduler.crawler.card

import com.wisecard.scheduler.scheduler.dto.CardCompany
import com.wisecard.scheduler.scheduler.dto.CardInfo
import com.wisecard.scheduler.scheduler.dto.CardType
import io.github.bonigarcia.wdm.WebDriverManager
import org.jsoup.Jsoup
import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.springframework.stereotype.Component

@Component
class HanaCrawler : CardCrawler {

    private val creditCardUrl =
        "https://www.hanacard.co.kr/OPI31000000D.web?schID=pcd&mID=OPI31000005P&CT_ID=241704030444153#none"

    private fun setupChromeDriver(): ChromeDriver {
        val chromeOptions = ChromeOptions()
        chromeOptions.addArguments("--headless=new")
        chromeOptions.addArguments("--disable-gpu")
        chromeOptions.addArguments("--no-sandbox")
        chromeOptions.addArguments("--disable-dev-shm-usage")
        WebDriverManager.chromedriver().setup()
        return ChromeDriver(chromeOptions).apply {
            manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(20))
        }
    }

    override fun crawlCreditCards(): List<CardInfo> {
        println("======= [하나] 신용 카드 정보 크롤링 =======")
        return crawlCards(CardType.CREDIT)
    }

    override fun crawlCheckCards(): List<CardInfo> {
        println("======= [하나] 체크 카드 정보 크롤링 =======")
        return crawlCards(CardType.CHECK)
    }

    private fun crawlCards(cardType: CardType): List<CardInfo> {
        val driver = setupChromeDriver()
        driver.get(creditCardUrl)
        Thread.sleep(3000)

        val cardInfos = mutableListOf<CardInfo>()
        val tabs = driver.findElements(By.cssSelector("#stc_list > li"))

        for (tab in tabs) {
            tab.click()
            Thread.sleep(3000)

            val soup = Jsoup.parse(driver.pageSource!!)
            val mainArea = soup.selectFirst("article.card_main_area")
            val cardUl = mainArea!!.selectFirst("ul.card_slide_area")
            val cardLi = cardUl!!.select("li.li")

            for (li in cardLi) {
                val name = li.selectFirst("dl.txt dt")!!.text()
                val urlBtn = li.selectFirst("ul.btn")!!.select("li")[1]
                val aTag = urlBtn.selectFirst("a.btn_ty04")
                val onclickValue = aTag!!.attr("onclick")
                val cardSeq = onclickValue.split("'")[1]

                val cardUrl = "https://www.hanacard.co.kr/OPI41000000D.web?schID=pcd&mID=PI410${
                    cardSeq.padStart(
                        5,
                        '0'
                    )
                }P&CD_PD_SEQ=$cardSeq&"
                val imgSrc = li.selectFirst("img")!!.attr("src")
                val imgUrl = "https://www.hanacard.co.kr$imgSrc"

                val benefits = crawlCardBenefits(cardUrl)

                cardInfos.add(
                    CardInfo(
                        cardCompany = CardCompany.HANA,
                        cardName = name,
                        imageUrl = imgUrl,
                        cardType = cardType,
                        benefits = benefits
                    )
                )

                println("$name $benefits")
            }
        }

        driver.quit()
        return cardInfos
    }

    private fun crawlCardBenefits(url: String): String {
        val driver = setupChromeDriver()
        driver.get(url)
        Thread.sleep(3000)

        val soup = Jsoup.parse(driver.pageSource!!)
        driver.quit()

        val tabList = soup.select("div.tab_cont")
        var benefit = ""

        if (tabList.isEmpty()) {
            val cardViewDetail = soup.selectFirst("div.card_view_detail")
            val infoList = cardViewDetail!!.select("ul.card_li li.list")
            for (info in infoList) {
                val tit = info.selectFirst("dt.tit")?.text() ?: ""
                benefit += "[$tit] "
                val infoTxtList = info.select("dd.txt li.blt1")
                for (txt in infoTxtList) {
                    benefit += "${txt.text()} "
                }
            }
        } else {
            for (tab in tabList) {
                val title = tab.selectFirst("h5.blind")?.text()
                if (title != null) benefit += "###$title"

                val contList = tab.select("div.cont")
                for (cont in contList) {
                    val contTit = cont.selectFirst("h6.t_tit")?.text()
                    if (contTit != null) benefit += "[$contTit]"

                    val tables = cont.select("table")
                    for (table in tables) {
                        benefit += table.text() + " "
                    }

                    val contUl = cont.select("> ul")
                    for (ul in contUl) {
                        val contLi = ul.select("> li")
                        for (li in contLi) {
                            if (li.select("table").isEmpty()) {
                                benefit += li.text() + " "
                            }
                        }
                    }
                }
            }
        }

        return benefit.trim()
    }
}
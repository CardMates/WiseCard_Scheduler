package com.wisecard.scheduler.scheduler.llm

import com.google.genai.Client
import org.springframework.stereotype.Component

@Component
class LlmClient {
    fun refine(benefitText: String): String {
        val apiKey = "AIzaSyB8ok76NbiLALkvON2tzrkaGZ0IS-Tq5cM"
        val model = "gemini-2.5-flash"

        val prompt = """
                $benefitText
다음 텍스트를 분석하여 카드 혜택 정보를 CrawledBenefit proto 메시지(JSON 표현)으로만 출력하라. 
다른 설명, 주석, 불필요한 텍스트는 출력하지 않는다. 

[출력 스키마]
{
  "benefits": [
      "discounts": [
        {
          "rate": double|null,           // 정률 할인
          "amount": int32|null,         // 정액 할인
          "minimum_amount": int32|null, // 최소 결제 금액
          "benefit_limit": int32|null,  // 최대 혜택 한도
          "minimum_spending": int32|null // 전월 최소 실적
          "channel": "ONLINE | OFFLINE | BOTH"
        },
        ...
      ],
      "points": [
        {
          "name": string|null,          //포인트 이름
          "amount": int32|null,         // 적립금
          "rate": double|null,           // 적립률
          "minimum_amount": int32|null, // 최소 결제 금액
          "benefit_limit": int32|null,  // 최대 혜택 한도
          "minimum_spending": int32|null // 전월 최소 실적
          "channel": "ONLINE | OFFLINE | BOTH"
        },
        ...
      ],
      "cashbacks": [
        {
          "rate": double|null,           // 캐시백률
          "amount": int32|null,         // 정액 캐시백
          "minimum_amount": int32|null, // 최소 결제 금액
          "benefit_limit": int32|null,  // 최대 혜택 한도
          "minimum_spending": int32|null // 전월 최소 실적
          "channel": "ONLINE | OFFLINE | BOTH"
        },
        ...
      ],
      "categories": ["MT1", ...],   // 카카오맵 업종 코드 필수, 알맞은 코드가 없으면 빈 배열
      "targets": ["스타벅스", "투썸플레이스", "음식점", ...] // 브랜드명 또는 한글 설명
      "summary": string // 혜택 정보 한 줄 요약 정리
    ]
}

[카테고리 규칙]
- categories는 반드시 카카오맵 업종 코드만 넣는다.
- 가능한 업종 코드: MT1, CS2, PS3, SC4, AC5, PK6, OL7, CT1, AG2, PO3, AT4, AD5, FD6, CE7, HP8, PM9
- 가능한 업종 코드 설명: 대형마트, 편의점, "어린이집, 유치원", 학교, 학원, 주차장, "주유소, 충전소", 문화시설, 중개업소, 공공기관, 관광명소, 숙박, 음식점, 카페, 병원, 약국
- 그 외 업종은 넣지 않는다.

[브랜드/대상 규칙]
- 일반적인 경우: targets에는 실제 브랜드명을 넣는다. (예: ["스타벅스", "이마트"])
- "모든 X", "전 X", "X 업종"과 같은 표현인 경우 → applicable_targets에는 **해당 업종의 한글 설명**을 넣는다.
  예) "모든 음식점" → ["음식점"], "전 주유소 (충전소 포함)" → ["주유소", "충전소"]

[기타 규칙]
- 숫자가 없는 경우 null로 둔다.
- 오직 JSON만 출력한다.
- discounts, points, cashbacks는 각각 리스트 형태로, 여러 혜택이 존재할 수 있다.
- channel은 각 혜택 단위로 ONLINE, OFFLINE, BOTH 중 하나를 지정한다.
- "benefits", "discounts", "points", "cashbacks", "categories", "targets", "summary"는 필수로 출력하고 관련 내용이 없으면 빈 배열을 출력한다.
            """.trimIndent()

        val client = Client.builder().apiKey(apiKey).build()
        val response = client.models.generateContent(model, prompt, null)
        val text = response.text()!!.replace("```json", "").replace("```", "")
        println(text)
        return text
    }
}
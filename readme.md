# Card Crawling & Refining System

카드사 혜택 정보를 **크롤링 → LLM 정제 → MongoDB 저장**하는 시스템입니다.  
아키텍처는 **Clean Architecture**를 기반으로 설계되었습니다.

---

## 📂 프로젝트 구조
```
src/main/kotlin/com/wisecard/scheduler
├─ core # 비즈니스 규칙 (도메인, 서비스, 인터페이스)
│ ├─ entity # CardBenefit, CrawlingResult 등 순수 엔티티
│ ├─ dto # API/서비스 계층 DTO
│ ├─ repository # Repository 인터페이스 (DB 기술과 무관)
│ └─ service # CardService, CrawlingService, RefiningService
│
├─ adapter # 외부 기술 의존성 (Infra, UI, Scheduler 등)
│ ├─ persistence # Mongo 구현체 (CardMongoRepository 등)
│ ├─ crawler # 카드사별 크롤러 (ShinhanCrawler, KbCrawler...)
│ ├─ scheduler # Spring Scheduler/Quartz Job
│ └─ api # REST Controller
│
└─ config # MongoDB, Scheduler, LLM 등 환경설정
```
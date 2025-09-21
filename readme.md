# Card Crawling & Refining System

카드사 혜택 정보를 **크롤링 → LLM 정제 → Proto 메시지 변환 → gRPC 전송**하는 시스템입니다.  
아키텍처는 **Clean Architecture**를 기반으로 설계되었습니다.

---

## 📂 프로젝트 구조
```
src/main/kotlin/com/wisecard/scheduler
├── SchedulerApplication.kt
├── grpc
│   └── CardServiceImpl.kt                # gRPC 서버
└── scheduler
    ├── CrawlingAndRefiningScheduler.kt   # 주기별로 실행 (Cron Job)
    ├── crawler                           # 카드사별 크롤러 (ShinhanCrawler, KbCrawler 등)
    ├── dto                               # dto
    ├── llm                               # LLM 호출
    └── service                           # JSON 정제
```
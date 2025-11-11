# Wisecard Scheduler - Architecture

카드사 혜택/프로모션을 주기적으로 수집하고(크롤링), LLM으로 정제한 뒤, Proto 메시지로 변환하여 gRPC로 전송하는 배치성 스케줄러입니다. 아키텍처는 테스트 용이성과 변경 내성에 초점을 맞춰, 계층화 + 포트/어댑터(헥사고날) 스타일로 재구성되었습니다.

## 패키지 구조
```
src/main/kotlin/com/wisecard/scheduler
├── SchedulerApplication.kt
├── application
│   ├── mapper
│   │   ├── CardBenefitMapper.kt          # 혜택 JSON → Proto 변환 포트
│   │   └── CardCompanyMapper.kt          # 도메인 CardCompany → Proto 매핑 단일화
│   ├── ports
│   │   └── out
│   │       ├── BenefitsRefiner.kt        # LLM 정제 포트
│   │       ├── CardBenefitsSender.kt     # gRPC 송신 포트(카드 혜택)
│   │       ├── CardCrawlingPort.kt       # 카드 크롤링 포트
│   │       ├── CardStoragePort.kt        # 저장소 포트
│   │       ├── PromotionCrawlingPort.kt  # 프로모션 크롤링 포트
│   │       └── PromotionSender.kt        # gRPC 송신 포트(프로모션)
│   ├── scheduler
│   │   └── CrawlingAndRefiningScheduler.kt  # 스케쥴러 실행 트리거
│   └── usecase
│       ├── CrawlRefineAndSendCardsUseCase.kt # 카드: 크롤링→정제→저장→전송
│       └── SendPromotionsUseCase.kt          # 프로모션: 크롤링→전송
├── domain
│   └── card / promotion ...                # 도메인 모델
└── infrastructure
    ├── crawler
    │   ├── card/*.kt                      # 카드사별 크롤러 구현(기존 인터페이스 유지)
    │   └── promotion/*.kt                 # 카드사별 프로모션 크롤러 구현
    │   └── adapter
    │       ├── CardCrawlingAdapter.kt     # 포트 out → 다중 크롤러 조합 어댑터
    │       └── PromotionCrawlingAdapter.kt
    ├── grpc
    │   ├── CardServiceImpl.kt             # gRPC 호출 구현(저수준)
    │   ├── PromotionServiceImpl.kt
    │   └── adapter
    │       ├── CardBenefitsSenderAdapter.kt  # 포트 out → gRPC 구현 어댑터
    │       └── PromotionSenderAdapter.kt
    ├── llm
    │   ├── LlmClient.kt
    │   └── adapter
    │       └── BenefitsRefinerAdapter.kt  # 포트 out → LLM 어댑터
    └── storage
        ├── CardDataStorageService.kt
        └── adapter
            └── CardStorageAdapter.kt      # 포트 out → 파일 저장 어댑터
```
## 아키텍처 요약
- 스케줄러는 오직 유스케이스만 호출
- 유스케이스는 포트(인터페이스)에만 의존
- 외부 통신/저장소/LLM은 인프라 어댑터로 격리

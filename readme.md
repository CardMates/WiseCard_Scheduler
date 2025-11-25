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

# Wisecard Scheduler - Test

## 검증 전략

### 1. LLM 정제 결과 검증
**문제**: LLM이 생성한 JSON의 신뢰성을 보장해야 함

**해결**: `BenefitsValidator`를 통해 다음을 검증
- **JSON 형식 검증**: 유효한 JSON 구조인지 확인
- **카테고리 코드 검증**: 카카오맵 업종 코드만 허용 (MT1, CS2, PS3, SC4, AC5, PK6, OL7, CT1, AG2, PO3, AT4, AD5, FD6, CE7, HP8, PM9)
- **채널 값 검증**: `ONLINE`, `OFFLINE`, `BOTH`만 허용
- **데이터 타입 검증**: 숫자 필드(`rate`, `amount` 등)는 숫자 또는 null만 허용
- **검증 실패 시 처리**: 검증 실패 시 빈 JSON(`{"benefits": []}`) 반환하여 시스템 안정성 유지

**구현 위치**: 
- `application/validator/BenefitsValidator.kt`: 검증 로직
- `infrastructure/llm/adapter/BenefitsRefinerAdapter.kt`: LLM 정제 후 자동 검증 수행

### 2. 매퍼 검증
**CardBenefitMapper 검증**:
- JSON → Proto 변환 정확성
- null 값 처리 (기본값으로 변환)
- 잘못된 JSON 형식 에러 처리
- 채널 값 대소문자 무시 변환
- 알 수 없는 채널 값은 `BOTH`로 기본 변환

**CardCompanyMapper 검증**:
- 모든 `CardCompany` enum 값이 Proto로 정확히 매핑되는지 확인
- 매핑 누락 방지

### 3. 저장소 로직 검증
**CardStoragePort 검증**:
- **ID 할당 로직**: 기존 카드의 최대 ID를 기준으로 순차적 할당
- **중복 필터링**: 카드사명 + 카드명 조합으로 중복 판별
- **병합 로직**: 기존 카드와 신규 카드 병합 정확성
- **파일 I/O**: 저장/로드 기능 정확성

### 4. 유스케이스 검증
**CrawlRefineAndSendCardsUseCase**:
- 신규 카드가 없을 때 기존 카드만 전송하는지 확인
- 신규 카드 처리 플로우: 크롤링 → ID 할당 → 정제 → 저장 → 전송
- 포트 호출 순서 및 횟수 검증

**SendPromotionsUseCase**:
- 프로모션 크롤링 후 전송 플로우 검증
- 빈 프로모션 리스트 처리

### 5. 어댑터 검증
**BenefitsRefinerAdapter**:
- LLM 정제 결과 검증 통합 확인
- 검증 실패 시 빈 JSON 반환 확인
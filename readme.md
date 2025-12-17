# Wisecard Scheduler
카드사 혜택/프로모션을 주기적으로 수집하고(크롤링), LLM으로 정제한 뒤, Proto 메시지로 변환하여 gRPC로 전송하는 배치성 스케줄러입니다. 아키텍처는 테스트 용이성과 변경 내성에 초점을 맞춰, 계층화 + 포트/어댑터(헥사고날) 스타일로 재구성되었습니다.

## 목차

- [프로젝트 개요](#프로젝트-개요)
- [기술 스택](#기술-스택)
- [시스템 요구사항](#시스템-요구사항)
- [빌드 방법](#빌드-방법)
- [설치 방법](#설치-방법)
- [테스트 방법](#테스트-방법)
- [데이터베이스](#데이터베이스)
- [사용된 오픈소스](#사용된-오픈소스)
- [프로젝트 구조](#프로젝트-구조)
- [패키지 구조](#패키지-구조)
- [아키텍처 요약](#아키텍처-요약)
- [검증](#검증)

## 프로젝트 개요

### 주요 기능
- **카드 크롤링**: 여러 카드사(삼성, 신한, 하나, 국민, 롯데, 현대)의 카드 정보를 자동으로 수집
- **프로모션 크롤링**: 각 카드사의 프로모션 정보를 수집
- **LLM 정제**: 크롤링한 혜택 텍스트를 LLM(Gemini)을 통해 구조화된 JSON으로 변환
- **데이터 검증**: LLM 정제 결과의 유효성을 검증 (카테고리 코드, 채널 값, 데이터 타입 등)
- **gRPC 전송**: 정제된 데이터를 Proto 메시지로 변환하여 gRPC 서버로 전송
- **로컬 저장**: 크롤링한 카드 데이터를 JSON 파일로 저장하여 중복 방지 및 ID 관리

## 기술 스택

- **언어**: Kotlin 1.9.25
- **프레임워크**: Spring Boot 3.5.4
- **빌드 도구**: Gradle
- **RPC**: gRPC 1.70.0
- **직렬화**: Protobuf 3.25.3
- **LLM**: Google Gemini API (google-genai 1.15.0)
- **웹 크롤링**: JSoup 1.21.2, Selenium 4.35.0
- **테스트**: JUnit 5, MockK, AssertJ

## 시스템 요구사항

- **Java 21** 이상
- **Gradle 8.0** 이상 (또는 Gradle Wrapper 사용)
- **Protobuf Compiler (protoc)** 3.25.3 이상
- **Chrome/Chromium** (Selenium 크롤링용)
- **Google Gemini API Key** (LLM 정제용)

## 빌드 방법

### 1. Repository Clone

```bash
git clone https://github.com/CardMates/WiseCard_Scheduler.git
cd wisecard
```

### 2. Protobuf 설치

#### macOS
```bash
brew install protobuf
```

#### Linux (Ubuntu/Debian)
```bash
apt-get update
apt-get install -y protobuf-compiler
```

#### Windows
1. [Protobuf Releases](https://github.com/protocolbuffers/protobuf/releases)에서 최신 버전 다운로드
2. 압축 해제 후 `bin` 디렉토리를 PATH에 추가
3. 또는 Chocolatey 사용: `choco install protobuf`

#### 설치 확인
```bash
protoc --version
# 출력: libprotoc 3.25.3 (또는 그 이상)
```

### 3. 환경 변수 설정

`src/main/resources/application.yml` 파일을 수정하거나 환경 변수로 설정:

```bash
export LMM_API_KEY=your-gemini-api-key
export GRPC_HOST=localhost
export GRPC_PORT=50052
```

또는 `application.yml` 파일 직접 수정:
```yaml
llm:
  api-key: ${LMM_API_KEY:your-api-key-here}
grpc:
  host: ${GRPC_HOST:localhost}
  port: ${GRPC_PORT:50052}
```

실제 변수는 `README.install` 파일 확인

### 4. 빌드

```bash
# Gradle Wrapper 사용 (권장)
./gradlew build

# 또는 직접 Gradle 사용
gradle build
```

빌드 결과물:
- `build/libs/scheduler-0.0.1-SNAPSHOT.jar`: 실행 가능한 JAR 파일
- `build/generated/source/proto/`: Protobuf로부터 생성된 코드

### Protobuf 컴파일

Gradle 빌드 시 자동으로 컴파일됩니다:

```bash
./gradlew generateProto
```

생성된 코드는 `build/generated/source/proto/main/` 디렉토리에 있습니다.

## 설치 방법

### 방법 1: JAR 파일 직접 실행

```bash
# 빌드 후
java -jar build/libs/scheduler-0.0.1-SNAPSHOT.jar
```

### 방법 2: Docker 사용

```bash
# Docker 이미지 빌드
docker build -t wisecard-scheduler .

# Docker 컨테이너 실행
docker run -e LMM_API_KEY=your-api-key \
           -e GRPC_HOST=your-grpc-host \
           -e GRPC_PORT=50052 \
           wisecard-scheduler
```

## 테스트 방법

### 단위 테스트 실행

```bash
./gradlew test
```

### 특정 테스트 클래스 실행

```bash
./gradlew test --tests "com.wisecard.scheduler.application.validator.BenefitsValidatorTest"
```

커버리지 리포트는 `build/reports/jacoco/test/html/index.html`에서 확인할 수 있습니다.

### 테스트 구조

```
src/test/kotlin/com/wisecard/scheduler
├── application
│   ├── mapper
│   │   ├── CardBenefitMapperTest.kt
│   │   └── CardCompanyMapperTest.kt
│   ├── usecase
│   │   ├── CrawlRefineAndSendCardsUseCaseTest.kt
│   │   └── SendPromotionsUseCaseTest.kt
│   └── validator
│       └── BenefitsValidatorTest.kt
└── infrastructure
    ├── llm/adapter
    │   └── BenefitsRefinerAdapterTest.kt
    └── storage/adapter
        └── CardStorageAdapterTest.kt
```

## 데이터베이스

이 프로젝트는 **파일 기반 저장소**를 사용합니다:
- **저장 위치**: `data/crawled_cards.json`
- **형식**: JSON 배열
- **용도**:
  - 크롤링한 카드 정보 저장
  - 중복 카드 필터링
  - 카드 ID 자동 할당

데이터베이스는 사용하지 않으며, 필요 시 `CardStoragePort` 구현을 변경하여 DB로 전환할 수 있습니다.

### 샘플 데이터 형식

```json
[
  {
    "cardId": 1,
    "cardUrl": "https://example.com/card",
    "cardCompany": "SAMSUNG",
    "cardName": "삼성카드 taptap",
    "imgUrl": "https://example.com/img.jpg",
    "cardType": "CREDIT",
    "benefits": "{\"benefits\": [{\"discounts\": [], \"points\": [], \"cashbacks\": [], \"categories\": [\"FD6\"], \"targets\": [\"스타벅스\"], \"summary\": \"카페 할인\"}]}"
  }
]
```

## 사용된 오픈소스

이 프로젝트에서 사용하는 오픈소스 라이브러리:

### Core Framework
- **Spring Boot 3.5.4**: 웹 애플리케이션 프레임워크
  - License: Apache 2.0
  - Repository: https://github.com/spring-projects/spring-boot

### Kotlin
- **Kotlin 1.9.25**: 프로그래밍 언어
  - License: Apache 2.0
  - Repository: https://github.com/JetBrains/kotlin

### gRPC / Protobuf
- **gRPC Java 1.70.0**: RPC 프레임워크
  - License: Apache 2.0
  - Repository: https://github.com/grpc/grpc-java
- **gRPC Kotlin Stub 1.4.3**: Kotlin용 gRPC 스텁
  - License: Apache 2.0
  - Repository: https://github.com/grpc/grpc-kotlin
- **Protobuf 3.25.3**: 직렬화 프레임워크
  - License: BSD 3-Clause
  - Repository: https://github.com/protocolbuffers/protobuf

### LLM
- **Google Genai 1.15.0**: Google Gemini API 클라이언트
  - License: Apache 2.0
  - Repository: https://github.com/google/generative-ai-java

### Web Scraping
- **JSoup 1.21.2**: HTML 파싱 라이브러리
  - License: MIT
  - Repository: https://github.com/jhy/jsoup
- **Selenium 4.35.0**: 웹 브라우저 자동화
  - License: Apache 2.0
  - Repository: https://github.com/SeleniumHQ/selenium
- **WebDriverManager 6.3.2**: WebDriver 자동 관리
  - License: Apache 2.0
  - Repository: https://github.com/bonigarcia/webdrivermanager

### JSON Processing
- **Jackson Kotlin Module**: Kotlin용 JSON 처리
  - License: Apache 2.0
  - Repository: https://github.com/FasterXML/jackson-module-kotlin

### Testing
- **JUnit 5**: 테스트 프레임워크
  - License: EPL 2.0
  - Repository: https://github.com/junit-team/junit5
- **MockK 1.13.13**: Kotlin용 Mocking 라이브러리
  - License: Apache 2.0
  - Repository: https://github.com/mockk/mockk
- **AssertJ 3.26.3**: Assertion 라이브러리
  - License: Apache 2.0
  - Repository: https://github.com/assertj/assertj-core

### CSV Processing
- **OpenCSV 5.12.0**: CSV 처리 라이브러리
  - License: Apache 2.0
  - Repository: https://github.com/opencsv/opencsv

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

## 검증

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
# HELP.md

이 문서는 `flag_speckit` 프로젝트의 개요, 아키텍처 및 로컬 개발 환경 설정 방법을 안내합니다.

## 1. 프로젝트 개요

이 프로젝트는 **피처 플래그(Feature Flag) 서비스**입니다. Kotlin과 Spring Boot를 기반으로 구현되었으며, 다음과 같은 주요 기능을 제공합니다.

- 단순 On/Off 토글
- 백분율 기반의 점진적 롤아웃
- 사용자 속성(Attribute) 기반의 타겟팅

이를 통해 애플리케이션의 새로운 기능을 안전하게 배포하고 A/B 테스트를 수행할 수 있습니다.

## 2. 프로젝트 구조

프로젝트는 주로 `flag_speckit` 디렉토리 내에 구성되어 있습니다.

- **`flag_speckit/`**: 메인 애플리케이션과 관련 파일이 위치합니다.
  - `backend/`: Kotlin/Spring Boot로 작성된 백엔드 소스 코드입니다.
  - `specs/`: 데이터 모델, API 명세 등 프로젝트의 주요 사양이 담긴 문서 폴더입니다.
  - `docker-compose.yml`: 로컬 개발에 필요한 PostgreSQL, Redis 등의 서비스를 정의합니다.
- **`feature_flag/`**: 별개의 Spring Boot 애플리케이션으로 보입니다. 메인 프로젝트와는 다른 구조와 의존성(MySQL)을 가지고 있어, 다른 마이크로서비스이거나 레거시 코드일 수 있습니다. **로컬 개발 시에는 이 디렉토리가 아닌 `flag_speckit`을 기준으로 작업합니다.**

## 3. 로컬 개발 환경 설정

로컬에서 `flag_speckit` 애플리케이션을 실행하는 절차는 다음과 같습니다.

### 사전 요구사항

- Docker & Docker Compose
- Java 17 또는 그 이상 버전의 JDK

### 설정 단계

1.  **의존성 서비스 실행**
    `flag_speckit` 디렉토리로 이동하여 아래 명령어를 실행해 PostgreSQL과 Redis 컨테이너를 백그라운드에서 시작합니다.

    ```bash
    cd flag_speckit
    docker-compose up -d
    ```

2.  **애플리케이션 빌드 및 실행**
    `flag_speckit/backend` 디렉토리로 이동하여 Gradle Wrapper를 사용해 애플리케이션을 빌드하고 실행합니다.

    ```bash
    cd backend
    ./gradlew build
    ./gradlew bootRun
    ```

    애플리케이션이 성공적으로 시작되면 `localhost:8080`에서 서비스가 실행됩니다.

## 4. 아키텍처

`flag_speckit` 백엔드 애플리케이션은 일반적인 **계층형 아키텍처(Layered Architecture)**를 따릅니다.

- **`controller`**: HTTP 요청을 수신하고 응답을 반환하는 API의 진입점입니다.
- **`service`**: 핵심 비즈니스 로직을 처리합니다.
- **`repository`**: PostgreSQL 데이터베이스와의 데이터 영속성을 담당합니다.
- **`domain`**: 애플리케이션의 핵심 데이터 모델(Entity)을 정의합니다.

## 5. 주요 기술 스택

- **언어**: Kotlin
- **프레임워크**: Spring Boot 3
- **데이터베이스**: PostgreSQL
- **캐시**: Redis
- **빌드 도구**: Gradle
- **컨테이너**: Docker

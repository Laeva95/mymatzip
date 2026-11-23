# MyMatzip (마이맛집) 🍽️

> **부산 지역 맛집을 탐색하고, 방문 경험을 리뷰로 공유하는 웹 서비스**

부산 지역 음식점 정보를 검색·조회하고 리뷰와 별점, 사진, 찜 목록을 관리할 수 있는 팀 프로젝트입니다. Spring Boot 기반의 서버 렌더링 화면과 REST API를 함께 사용하며, Google 로그인, AI 추천·리뷰 요약, 공공데이터 연동, AWS 배포를 다룹니다.

- **프로젝트 유형:** K-Digital Training 팀 프로젝트 (6인)
- **GitHub:** https://github.com/Laeva95/mymatzip
- **서비스 주소:** https://mymatzip.p-e.kr *(운영 상태는 별도 확인 필요)*

## 1. 주요 기능

| 영역 | 기능 | 소스 코드 근거 |
|---|---|---|
| 회원·인증 | 회원가입, 로그인, Google OAuth2 로그인, JWT 인증, 마이페이지 | `MemberApiController`, `CustomOAuth2UserService`, `JwtTokenProvider`, `MyPageApiController` |
| 맛집 | 음식점 목록·상세 조회, 등록·수정, 카테고리 및 공공데이터 연동 | `RestaurantApiController`, `RestaurantService`, `PublicRestaurantImportService` |
| 리뷰·소통 | 리뷰·별점, 댓글, 신고 관련 처리 | `ReviewApiController`, `ReplyApiController`, `ReviewService` |
| 사진 | 음식점·리뷰 이미지 업로드 및 다운로드, 로컬/S3 저장 처리 | `FileApiController`, `FileService` |
| 찜 | 관심 음식점 찜 목록 관리 | `WishlistApiController`, `WishlistService` |
| AI | 맛집 추천, 리뷰 요약, 리뷰 스팸 감지 관련 서비스 | `AiRecommendService`, `AiReviewSummaryService`, `AiReviewSpamDetectionService` |
| 운영·관리 | 관리자 대시보드, 회원·음식점·리뷰 관리, 점주 신청 처리 | `AdminApiController`, `AdminViewController`, `OwnerApplicationService` |

> 위 표의 기능은 업로드된 프로젝트 ZIP에서 관련 구현 클래스가 확인된 항목입니다. 모든 기능의 실제 운영 환경 정상 동작이나 개별 팀원의 코드 작성 범위를 증명하는 것은 아닙니다.

## 2. 팀 구성 및 역할

아래 역할은 **프로젝트 결과 보고서의 「02 팀 구성 및 역할」 슬라이드**를 기준으로 작성했습니다. 담당 업무는 팀 내 역할 분담을 나타내며, Git 커밋 작성자나 개인별 구현량을 단정하지 않습니다.

| 이름 | 역할 | 담당 업무 | 주요 기술 |
|---|---|---|---|
| **정운학** | 팀장 | 일정 관리, 저장소 및 PR 관리, 공통 레이아웃·메인 화면, 제출 문서 작성 | Git/GitHub, Thymeleaf |
| **유승민** | 부팀장 | 빌드·배포 관리(CI/CD, AWS EC2·S3, MySQL RDS), 테스트 코드 작성 | GitHub Actions, AWS |
| **권성애** | 팀원 | 맛집 게시판 CRUD, 맛집 사진 업로드, 공공데이터 API 연동 | Spring Data JPA, MultipartFile |
| **김지현** | 팀원 | 찜 기능, AI 맛집 추천 | REST API, OpenRouter |
| **서희정** | 팀원 | 회원가입·로그인, Google 로그인, JWT 인증, 마이페이지 | Spring Security, JWT |
| **정범석** | 팀원 | 리뷰·별점·댓글·신고, AI 리뷰 요약 | JPA, Spring AI |

### 역할별 작업 설명

**정운학 · 팀장 / 공통 화면 및 협업 관리**  
프로젝트 일정을 조율하고 GitHub 저장소·Pull Request 관리와 협업 흐름을 담당했습니다. 공통 레이아웃과 메인 화면을 구성하고 결과 보고를 위한 제출 문서를 정리했습니다.

**유승민 · 부팀장 / 인프라 및 CI/CD**  
Gradle 기반 Spring Boot 프로젝트의 빌드·배포 흐름을 관리하고, GitHub Actions를 이용한 CI/CD 구성을 담당했습니다. AWS EC2 애플리케이션 서버, S3 이미지 저장소, MySQL RDS 연동 및 배포 환경을 관리하고 테스트 코드를 작성하는 역할을 맡았습니다. 저장소에는 `.github/workflows/ci.yml`, `cd.yml`, `pr-code-review.yml`이 포함되어 있습니다.

**권성애 · 맛집 정보 및 이미지·공공데이터**  
맛집 게시판의 등록·조회·수정·삭제 기능을 담당하고 이미지 업로드 및 공공데이터 API 연동을 통해 음식점 정보 관리 기능을 구성했습니다.

**김지현 · 찜 및 AI 맛집 추천**  
사용자가 관심 있는 맛집을 저장하는 찜 기능과 AI 기반 맛집 추천 기능을 담당했습니다. 프로젝트에는 `WishlistService`, `AiRecommendService` 등 관련 서비스가 포함되어 있습니다.

**서희정 · 회원 및 인증**  
회원가입·로그인, Google OAuth2 소셜 로그인, JWT 기반 인증과 마이페이지 기능을 담당했습니다. Spring Security를 활용한 인증·인가 구성과 사용자 정보 처리 기능이 포함됩니다.

**정범석 · 리뷰 및 AI 리뷰 요약**  
맛집 리뷰, 별점, 댓글, 신고 기능과 AI 기반 리뷰 요약을 담당했습니다. JPA 기반 리뷰·댓글 처리와 `AiReviewSummaryService` 등 관련 코드가 포함됩니다.

## 3. 기술 스택

| 구분 | 기술 |
|---|---|
| Language | Java 21, HTML, CSS, JavaScript |
| Backend | Spring Boot **4.1.2** (`../../../Downloads/build.gradle` 기준), Spring MVC, Spring Data JPA, Spring Security |
| Frontend | Thymeleaf |
| Database | MySQL, H2(테스트용) |
| Authentication | Spring Security, Google OAuth2, JWT |
| AI | Spring AI, OpenRouter 호환 API |
| Storage | AWS S3, 로컬 파일 저장 방식 |
| Infrastructure | AWS EC2, RDS, S3, Nginx |
| Build / CI·CD | Gradle, GitHub Actions |
| Testing | JUnit Platform, Spring Boot Test, Mockito, AssertJ |
| Collaboration | Git, GitHub, Pull Request |

## 4. 시스템 구성

```text
사용자 브라우저
      │ HTTPS
      ▼
  Nginx (EC2)
      │ Reverse Proxy
      ▼
Spring Boot + Thymeleaf ───── MySQL (RDS)
      │
      ├── Spring Security / Google OAuth2 / JWT
      ├── REST API / JPA
      ├── 이미지 저장·조회 ─── AWS S3 (설정 시)
      ├── AI 기능 ────────── OpenRouter 호환 API
      └── 공공데이터 API 연동

GitHub → GitHub Actions (CI/CD) → 배포 환경
```

이 구조는 프로젝트 소스와 팀 역할 자료에 근거한 개요이며, 현재 AWS 리소스의 실시간 상태를 의미하지 않습니다.

## 5. 프로젝트 디렉터리

```text
src/
├── main/
│   ├── java/com/example/mymatzip/
│   │   ├── config/           # 보안·공통 설정
│   │   ├── controller/
│   │   │   ├── api/          # REST API
│   │   │   └── view/         # Thymeleaf 화면 컨트롤러
│   │   ├── dto/              # 요청·응답 DTO
│   │   ├── entity/           # JPA 엔티티
│   │   ├── repository/       # 데이터 접근
│   │   └── service/          # 비즈니스 로직·AI·공공데이터
│   └── resources/
│       ├── templates/        # Thymeleaf 템플릿
│       ├── static/           # 정적 리소스
│       └── application.properties
└── test/                     # 테스트 코드
.github/workflows/           # CI/CD 및 PR 리뷰 워크플로
build.gradle                 # Gradle 빌드 설정
```

## 6. 로컬 실행

**사전 준비:** JDK 21, MySQL 및 애플리케이션에 필요한 외부 서비스 설정.

1. 저장소를 내려받습니다.
   ```bash
   git clone https://github.com/Laeva95/mymatzip.git
   cd mymatzip
   ```
2. DB 접속 정보, JWT 서명 키, Google OAuth2 인증 정보, AI API 키, 메일 및 공공데이터 API 설정 등 필요한 환경값을 준비합니다. 실제 프로젝트 설정에서 참조하는 `developer.properties` 또는 환경변수를 이용하되, **실제 비밀값은 Git에 커밋하지 않습니다.**
3. 테스트 및 빌드:
   ```bash
   ./gradlew test
   ./gradlew bootJar
   ```
   Windows CMD에서는 `gradlew.bat test`, `gradlew.bat bootJar`를 사용합니다.
4. 생성된 실행형 JAR를 실행합니다.
   ```bash
   java -jar build/libs/mymatzip.jar
   ```
   *JAR 이름은 `settings.gradle`의 프로젝트명과 `../../../Downloads/build.gradle`의 `bootJar` 설정에 따라 달라질 수 있습니다.*

기본 포트는 업로드된 `../../../Downloads/application.properties` 기준 **8082**입니다. 실행 시 MySQL 연결 및 외부 API 인증 정보가 올바르지 않으면 일부 기능이 실패할 수 있습니다.

## 7. 배포 및 운영 시 참고 사항

- **HTTPS 프록시:** Nginx에서 `X-Forwarded-Proto`, `X-Forwarded-Host`를 전달하고 Spring Boot의 `server.forward-headers-strategy=framework`를 사용합니다.
- **Google OAuth2:** Google Cloud Console의 승인된 리디렉션 URI가 실제 배포 도메인의 HTTPS 콜백 주소와 정확히 일치해야 합니다.
- **AWS S3:** EC2에서는 가능한 IAM 역할 기반 인증을 사용하고, 로컬·공용 PC에 장기 액세스 키를 저장하지 않습니다.
- **DB / API 비밀값:** 소스 코드·README·로그에 비밀번호, 액세스 키, JWT 비밀값을 노출하지 않습니다.
- **CI/CD:** `.github/workflows/ci.yml`, `cd.yml` 및 PR 리뷰 워크플로가 저장소에 존재합니다. 실제 자동 배포 성공 여부는 GitHub Actions 실행 기록에서 확인해야 합니다.

## 8. 참고 및 작성 기준

- **팀원별 담당 업무:** 첨부된 「2조 프로젝트 결과 보고서」의 '02 팀 구성 및 역할' 슬라이드
- **구현 구조·기술 스택:** 첨부된 `mymatzip-develop(2).zip`의 `src/main`, `../../../Downloads/build.gradle`, `.github/workflows` 확인
- **검증 범위:** 파일과 클래스의 존재 및 설정을 확인한 것이며, 전체 기능의 통합 테스트나 서비스 운영 검증을 수행한 것은 아닙니다.

---

**MyMatzip — 부산의 맛집을 발견하고, 경험을 공유하는 공간**

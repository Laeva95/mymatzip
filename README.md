# C 담당 (방문 · 포인트 · 쿠폰) 합치기 안내

## 1. 파일 넣기
이 폴더의 `src` 안 파일을 팀 프로젝트 `src`에 그대로 복사 (기존 파일을 덮어쓰는 것은 없음).

- entity: Visit, PointHistory, PointType, Coupon
- repository: VisitRepository, PointHistoryRepository, CouponRepository
- dto: VisitForm
- service: VisitService, CouponService
- controller: VisitController
- templates/visit/list.html

## 2. 팀 프로젝트에 이미 있어야 하는 것
- `User` 엔티티: `getId()`, `getUsername()` 필요
- `UserRepository`: `Optional<User> findByUsername(String)` 필요
- 세션 로그인 (컨트롤러가 `Authentication.getName()` 으로 로그인한 username 사용)
- build.gradle 의존성: web, security, data-jpa, thymeleaf, validation, lombok

## 3. 패키지 이름이 다를 때
이 코드의 패키지는 `com.shin.springbootsecuritylogin.blog` 입니다.
팀 패키지가 다르면 복사한 파일 전체에서 아래 문자열을 일괄 바꾸기 (IntelliJ: Ctrl+Shift+R).

    com.shin.springbootsecuritylogin.blog  →  팀 패키지

(entity / repository / dto / service / controller 하위 패키지 구조가 팀과 다르면 import 도 함께 맞출 것)

## 4. SecurityConfig 에서 확인할 것
- `/visits/**` 는 로그인한 사람만 접근 → `.anyRequest().authenticated()` 가 이미 있으면 추가 설정 불필요
- 폼 로그인을 끈 구조라면 로그인 안 한 사람을 /login 으로 보내려고 아래를 추가 (선택)

      .exceptionHandling(ex -> ex
              .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
      )

  import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

- CSRF 를 켠 상태라 방문 등록 · 쿠폰 사용은 `th:action` 폼으로 보냄 (토큰 자동 포함)

## 5. Restaurant 연결 (팀 엔티티 확정 후)
지금 `Visit.restaurantName` 은 문자열입니다. 팀 `Restaurant` 가 확정되면
`Visit` 에 `@ManyToOne Restaurant restaurant` 를 두고, `VisitForm` / `VisitService.register()` /
`list.html` 의 식당 입력을 식당 선택(id)으로 바꾸면 됩니다.

## 6. 규칙 바꾸는 곳
- 적립률 5%: VisitService 의 EARN_RATE
- 쿠폰 조건(5회 방문, 3,000원, 30일): CouponService 의 MILESTONE / DISCOUNT / VALID_DAYS

## 7. 합친 뒤 확인
1. 앱 실행 후 로그인 → `/visits`
2. 결제 20000원 방문 등록 → "1000P 적립!"
3. 포인트 잔액보다 많이 쓰면 거부 메시지
4. 방문 5회 → 쿠폰 발급 → "사용" 버튼
5. H2 콘솔: visit, point_history, coupon 테이블에 데이터 확인

※ 예전 DB 파일(~/securitylogin.mv.db)에 옛 테이블이 남아 있으면 칼럼 충돌이 날 수 있으니 삭제 후 실행.

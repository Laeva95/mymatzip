// ============================================================
// [MemberRepository.java] 회원 데이터 접근 인터페이스 (Repository)
// ============================================================
// 역할: 회원(Member) 테이블에 대한 DB 접근(CRUD)을 담당합니다.
//       JpaRepository를 상속받으면 기본 CRUD 메서드가 자동으로 제공됩니다.
//
//
// ★ 핵심: interface로 선언만 하면 Spring Data JPA가 구현체를 자동 생성!
//   즉, SQL을 직접 작성하지 않아도 메서드 이름만으로 쿼리가 만들어집니다.
//   예: findByEmail(String email) → SELECT * FROM member WHERE email = ?
//
// [JpaRepository가 기본 제공하는 메서드들 — 아래에 안 보여도 사용 가능!]
//   - save(member)       : 회원 저장 (INSERT) 또는 수정 (UPDATE)
//   - findById(id)       : ID(번호)로 회원 1명 조회
//   - findAll()          : 모든 회원 목록 조회
//   - deleteById(id)     : ID로 회원 삭제
//   - count()            : 전체 회원 수
//
// [Query Method 규칙 — 메서드 이름이 곧 SQL]
//   findBy + 필드명   → WHERE 필드 = 값
//   existsBy + 필드명 → 해당 값이 존재하면 true, 없으면 false
//   예) findByEmail   → SELECT * FROM member WHERE email = ?
//
// [extends JpaRepository<Member, Long> 의미]
//   - Member : 이 창구가 다루는 엔티티(테이블)
//   - Long   : Member의 PK(기본키=id) 타입 (id가 Long이므로)
//
// 사용되는 곳: MemberService (회원가입, 로그인 처리)
// ============================================================

// --- 패키지 선언 ---
// 이 파일이 속한 폴더(패키지) 경로를 지정합니다.
// com.example.mymatzip.repository = "repository(저장소)" 계층에 해당
package com.example.mymatzip.repository;

// --- import 영역 (외부 클래스 가져오기) ---

// Member 엔티티 가져오기 — 이 Repository가 다룰 대상(테이블)
// entity 패키지의 Member 클래스 = DB의 member 테이블과 매핑된 객체
import com.example.mymatzip.entity.Member;

// JpaRepository 가져오기 — Spring Data JPA가 제공하는 "만능 창구" 인터페이스
// 이것을 상속(extends)하면 save, findById, findAll, delete 등이 자동 제공됨
import org.springframework.data.jpa.repository.JpaRepository;

// Page, Pageable 가져오기 — 페이징(한 페이지씩 나눠서 보기) 기능을 위한 클래스
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// Optional 가져오기 — "값이 있을 수도, 없을 수도 있는 상자" 클래스
// null 대신 사용하여 NullPointerException(널 에러)을 방지하는 안전장치
import java.util.Optional;

// ★ interface(인터페이스) 선언
// 클래스(class)가 아니라 인터페이스(interface)인 이유:
//   → Spring Data JPA가 이 인터페이스를 보고 "구현체(실제 동작하는 코드)"를
//     런타임(실행 시점)에 자동으로 만들어줍니다.
//   → 우리는 "무엇을 할 수 있는지(메서드 선언)"만 정의하면 됩니다.
//
// extends JpaRepository<Member, Long> 의미:
//   → JpaRepository의 모든 기능을 물려받겠다는 선언
//   → <Member, Long> = "Member 엔티티를 다루며, 기본키(PK) 타입은 Long"
public interface MemberRepository extends JpaRepository<Member, Long> {

    // ──────────────────────────────────────────────────────
    // 이메일로 회원 조회 메서드 (로그인 시 사용)
    // ──────────────────────────────────────────────────────
    // [사용 화면] 로그인 페이지 → 이메일+비밀번호로 로그인할 때
    //
    // [매개변수]
    //   - String email : 사용자가 로그인 폼에 입력한 이메일 주소
    //     예: "hong@mail.com"
    //
    // [반환값]
    //   - Optional<Member> : 이메일에 해당하는 회원이 담긴 "안전 상자"
    //     → 회원이 있으면: Optional 안에 Member 객체가 들어 있음
    //     → 회원이 없으면: 빈 Optional (null 대신 사용하여 안전)
    //
    // [Optional이란?]
    //
    // [Query Method 해석 — 메서드 이름이 SQL로 변환됨]
    //   find + By + Email
    //   → SELECT * FROM member WHERE email = ?
    //   → "member 테이블에서 email 컬럼이 일치하는 행을 찾아라"
    //
    // [호출 예시 — MemberService에서]
    //   Optional<Member> found = memberRepository.findByEmail("hong@mail.com");
    //   Member member = found.orElseThrow(() -> new RuntimeException("회원 없음"));
    // ──────────────────────────────────────────────────────
    Optional<Member> findByEmail(String email);

    // ──────────────────────────────────────────────────────
    // 이메일 중복 확인 메서드 (회원가입 시 사용)
    // ──────────────────────────────────────────────────────
    // [사용 화면] 회원가입 페이지 → "이미 사용 중인 이메일입니다" 검증
    //
    // [매개변수]
    //   - String email : 사용자가 회원가입 폼에 입력한 이메일 주소
    //
    // [반환값]
    //   - boolean : true(이미 존재) / false(사용 가능)
    //     → true이면 "이미 가입된 이메일"이므로 회원가입을 거부합니다.
    //
    // [Query Method 해석]
    //   exists + By + Email
    //   → 해당 이메일이 DB에 있으면 true, 없으면 false
    //   → 내부적으로: SELECT COUNT(*) > 0 FROM member WHERE email = ?
    //   → COUNT(*)가 1 이상이면 true, 0이면 false
    //
    // [호출 예시 — MemberService에서]
    //   if (memberRepository.existsByEmail("hong@mail.com")) {
    //       throw new RuntimeException("이미 사용 중인 이메일입니다.");
    //   }
    // ──────────────────────────────────────────────────────
    boolean existsByEmail(String email);

    // ──────────────────────────────────────────────────────
    // 닉네임 중복 확인 메서드 (회원가입 시 사용)
    // ──────────────────────────────────────────────────────
    // [사용 화면] 회원가입 페이지 → "이미 사용 중인 닉네임입니다" 검증
    //
    // [매개변수]
    //   - String nickname : 사용자가 회원가입 폼에 입력한 닉네임
    //     예: "맛집탐험가"
    //
    // [반환값]
    //   - boolean : true(이미 존재) / false(사용 가능)
    //     → true이면 "이미 사용 중인 닉네임"이므로 다른 닉네임을 입력하도록 안내
    //
    // [Query Method 해석]
    //   exists + By + Nickname
    //   → 해당 닉네임이 DB에 있으면 true, 없으면 false
    //   → 내부적으로: SELECT COUNT(*) > 0 FROM member WHERE nickname = ?
    //
    // [호출 예시 — MemberService에서]
    //   if (memberRepository.existsByNickname("맛집탐험가")) {
    //       throw new RuntimeException("이미 사용 중인 닉네임입니다.");
    //   }
    // ──────────────────────────────────────────────────────
    boolean existsByNickname(String nickname);

    // ──────────────────────────────────────────────────────
    // [Phase 7] OAuth 소셜 로그인 회원 조회 메서드
    // ──────────────────────────────────────────────────────
    // [사용 화면] 소셜 로그인(구글) 시 기존 회원인지 확인
    //
    // [매개변수]
    //   - String provider   : 소셜 로그인 제공자 이름 (예: "google")
    //   - String providerId : 소셜 로그인 제공자가 부여한 고유 ID (예: "123456789")
    //
    // [반환값]
    //   - Optional<Member> : 해당 소셜 계정으로 가입된 회원이 있으면 반환, 없으면 빈 Optional
    //
    // [Query Method 해석]
    //   find + By + Provider + And + ProviderId
    //   → SELECT * FROM member WHERE provider = ? AND provider_id = ?
    // ──────────────────────────────────────────────────────
    Optional<Member> findByProviderAndProviderId(String provider, String providerId);

    // ──────────────────────────────────────────────────────
    // [Phase 5] 전체 회원 목록 페이징 조회 메서드 (관리자용)
    // ──────────────────────────────────────────────────────
    // [사용 화면] 관리자 페이지 → "전체 회원 목록" (페이지별)
    //
    // [매개변수]
    //   - Pageable pageable : 페이지 번호와 크기 정보
    // [반환값]
    //   - Page<Member> : 페이지 정보가 포함된 회원 목록
    // ──────────────────────────────────────────────────────
    Page<Member> findAll(Pageable pageable);
}
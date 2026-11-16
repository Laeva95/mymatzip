// ============================================================
// [WishlistService.java] 찜(위시리스트) 비즈니스 로직 서비스
// ============================================================
// 역할: 찜(위시리스트)과 관련된 핵심 업무를 처리합니다.
//       찜 추가, 찜 해제, 찜 목록 조회, 찜 여부 확인 등
//       Controller로부터 요청을 받아 → Repository를 통해 DB를 조작합니다.
//
// MVC 아키텍처에서의 위치:
//   Controller (요청 접수) → ★Service (업무 처리)★ → Repository (DB 접근)
//
//
// 사용처:
//   - WishlistController에서 찜 추가/해제/조회 요청 시 호출
//   - 맛집 상세 페이지에서 "이 맛집을 찜했는지" 확인할 때 isWished() 호출
//   - 마이페이지에서 "내 찜 목록"을 보여줄 때 findByMemberId() 호출
//
// @Transactional 설명:
//   트랜잭션 = "하나의 작업 단위" (중간에 실패하면 전부 취소)
//   readOnly=true (기본): 조회 메서드에서 사용 → 성능 최적화
//   @Transactional (메서드 레벨): 찜 추가/해제처럼 DB에 변경이 필요한 경우 사용
// ============================================================

// 이 파일이 속한 패키지(폴더) 선언 — service 패키지에 위치함을 나타냄
package com.example.mymatzip.service;

// ─── import 영역 ─────────────────────────────────────────────
// Member: 회원 정보를 담는 엔티티 — 누가 찜했는지 참조하기 위해 사용
import com.example.mymatzip.entity.Member;

// Restaurant: 맛집 정보를 담는 엔티티 — 어떤 맛집을 찜했는지 참조하기 위해 사용
import com.example.mymatzip.entity.Restaurant;

// Wishlist: 찜(위시리스트) 정보를 담는 엔티티 — DB의 wishlist 테이블과 1:1 대응
//           회원(Member)과 맛집(Restaurant)의 관계를 나타내는 중간 테이블
import com.example.mymatzip.entity.Wishlist;

// RestaurantRepository: DB의 restaurant 테이블에 접근하는 인터페이스
//                       → 찜 추가 시 대상 맛집이 실제로 존재하는지 확인하기 위해 사용
import com.example.mymatzip.repository.RestaurantRepository;

// WishlistRepository: DB의 wishlist 테이블에 접근하는 인터페이스 (CRUD 기능 제공)
//                     → 찜 데이터의 조회, 저장, 삭제, 존재 여부 확인을 담당
import com.example.mymatzip.repository.WishlistRepository;

// @RequiredArgsConstructor: final 필드에 대한 생성자를 자동 생성해주는 Lombok 어노테이션
// → 직접 생성자를 작성하지 않아도 의존성 주입(DI)이 자동으로 이루어짐
import lombok.RequiredArgsConstructor;

// @Service: 이 클래스가 "서비스 계층"임을 Spring에게 알려주는 어노테이션
// → Spring이 자동으로 이 클래스의 객체(Bean)를 생성하고 관리함
import org.springframework.stereotype.Service;

// @Transactional: DB 작업을 하나의 트랜잭션(작업 단위)으로 묶어주는 어노테이션
// → 작업 도중 에러 발생 시 모든 변경사항을 자동으로 되돌림 (rollback)
import org.springframework.transaction.annotation.Transactional;

// List: 여러 개의 데이터를 순서대로 담을 수 있는 자바 컬렉션 (목록)
// → 찜 목록을 담아 반환할 때 사용
import java.util.List;

// ─── 클래스 선언부 ────────────────────────────────────────────

// @Service: Spring에게 "이 클래스는 비즈니스 로직을 담당하는 서비스입니다"라고 등록
//           → Spring 컨테이너가 이 클래스의 인스턴스를 자동 생성·관리함
@Service

// @RequiredArgsConstructor: 아래의 final 필드들(wishlistRepository, restaurantRepository)을
//                           매개변수로 받는 생성자를 Lombok이 자동 생성
//                           → Spring이 생성자를 통해 각 Repository를 자동 주입(DI)
@RequiredArgsConstructor

// @Transactional(readOnly = true): 이 클래스의 모든 메서드에 기본적으로 "읽기 전용" 트랜잭션 적용
//   - readOnly = true란? → DB에서 데이터를 "읽기만" 하겠다는 의미
//   - 장점: DB가 불필요한 잠금(Lock)을 걸지 않아 성능이 향상됨
//   - 쓰기가 필요한 메서드에는 별도로 @Transactional을 붙여 readOnly를 해제함
@Transactional(readOnly = true)
public class WishlistService {

    // ─── 필드(의존성) 영역 ────────────────────────────────────

    // wishlistRepository: DB의 wishlist 테이블에 접근하기 위한 저장소 객체
    // final 키워드: 한 번 주입된 후 변경 불가 → 안전성 보장
    // 역할: 찜 데이터의 조회, 저장, 삭제, 존재 여부 확인을 담당
    private final WishlistRepository wishlistRepository;

    // restaurantRepository: DB의 restaurant 테이블에 접근하기 위한 저장소 객체
    // 역할: 찜 추가 시 대상 맛집이 실제로 존재하는지 확인하기 위해 사용
    //       (존재하지 않는 맛집을 찜할 수 없으므로)
    private final RestaurantRepository restaurantRepository;

    // ─── 회원별 찜 목록 조회 메서드 ───────────────────────────

    // @Transactional이 없음 → 클래스 레벨의 readOnly = true 적용
    // ┌─────────────────────────────────────────────────────────────────────┐
    // │ 메서드명: findByMemberId (회원 ID로 찜 목록 조회)                     │
    // │ 매개변수: Long memberId — 찜 목록을 조회할 회원의 고유번호              │
    // │ 리턴값:  List<Wishlist> — 해당 회원이 찜한 목록 (Wishlist 엔티티 리스트)│
    // │ 동작 흐름:                                                          │
    // │   1단계: DB에서 해당 회원이 찜한 모든 데이터를 조회                     │
    // │   2단계: Wishlist 엔티티 목록을 그대로 반환                            │
    // │ 사용처: 마이페이지에서 "내가 찜한 맛집 목록"을 보여줄 때 사용            │
    // └─────────────────────────────────────────────────────────────────────┘
    public List<Wishlist> findByMemberId(Long memberId) {
        // wishlistRepository.findByMemberId(memberId):
        //   → DB에서 특정 회원(memberId)이 찜한 모든 Wishlist 데이터를 조회
        //   → Spring Data JPA의 메서드 이름 규칙으로 자동 생성된 쿼리
        //   → SQL: SELECT * FROM wishlist WHERE member_id = ?
        return wishlistRepository.findByMemberId(memberId);
    }

    // ─── 찜 추가 메서드 ──────────────────────────────────────

    // @Transactional: 이 메서드는 DB에 데이터를 "쓰는" 작업이므로
    //                 클래스 레벨의 readOnly=true를 덮어씀 (쓰기 가능 트랜잭션)
    //                 → 메서드 실행 중 오류 발생 시 모든 DB 변경이 자동 취소(rollback)됨
    @Transactional
    // ┌─────────────────────────────────────────────────────────────────────┐
    // │ 메서드명: add (맛집 찜 추가)                                         │
    // │ 매개변수: Long restaurantId — 찜할 맛집의 고유번호                     │
    // │          Member loginMember — 현재 로그인한 회원 (찜을 누르는 사람)     │
    // │ 리턴값:  Wishlist — 생성된 찜 엔티티 객체                             │
    // │ 동작 흐름:                                                          │
    // │   1단계: 이미 찜한 맛집인지 중복 확인 (중복이면 예외)                    │
    // │   2단계: 찜 대상 맛집이 존재하는지 확인 (없으면 예외)                    │
    // │   3단계: Wishlist 엔티티 객체 생성 (빌더 패턴)                         │
    // │   4단계: DB에 저장 후 반환                                           │
    // └─────────────────────────────────────────────────────────────────────┘
    public Wishlist add(Long restaurantId, Member loginMember) {

        // [1단계] 이미 찜한 맛집인지 중복 확인
        // wishlistRepository.existsByMemberIdAndRestaurantId(): DB에서 해당 회원이
        //   해당 맛집을 이미 찜했는지 확인 (true: 이미 찜함, false: 아직 찜하지 않음)
        // loginMember.getId(): 현재 로그인한 회원의 ID
        // restaurantId: 찜하려는 맛집의 ID
        if (wishlistRepository.existsByMemberIdAndRestaurantId(loginMember.getId(), restaurantId)) {
            // 이미 찜한 맛집이면 예외 발생 → 중복 찜 방지
            throw new IllegalArgumentException("이미 찜한 맛집입니다.");
        }

        // [2단계] 찜 대상 맛집이 존재하는지 확인
        // restaurantRepository.findById(restaurantId): DB에서 해당 ID의 맛집을 조회
        // .orElseThrow(): 맛집이 존재하지 않으면 예외 발생
        // → 존재하지 않는 맛집을 찜할 수 없으므로 사전 확인
        //[변경]2026.11.11권성애 (수정: findById → 삭제 안 된 맛집만)
        Restaurant restaurant = restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("맛집을 찾을 수 없습니다."));

        // [3단계] Wishlist 엔티티 객체 생성 (빌더 패턴 사용)
        // 빌더 패턴: .속성명(값) 형태로 가독성 좋게 객체를 생성하는 디자인 패턴
        Wishlist wishlist = Wishlist.builder()
                // .member(): 찜을 누른 회원 정보 설정 → 누가 찜했는지 기록
                .member(loginMember)
                // .restaurant(): 찜 대상 맛집 정보 설정 → 어떤 맛집을 찜했는지 기록
                .restaurant(restaurant)
                // .build(): 위에서 설정한 값들로 최종 Wishlist 객체를 생성
                .build();

        // [4단계] DB에 찜 정보 저장 후 반환
        // wishlistRepository.save(wishlist): DB의 wishlist 테이블에 INSERT(삽입)
        // 저장 후 DB가 자동 생성한 ID가 포함된 Wishlist 객체를 반환
        return wishlistRepository.save(wishlist);
    }

    // ─── 찜 해제(삭제) 메서드 ─────────────────────────────────

    // @Transactional: DB에서 데이터를 "삭제"하는 작업이므로 쓰기 가능 트랜잭션 적용
    @Transactional
    // ┌─────────────────────────────────────────────────────────────────────┐
    // │ 메서드명: remove (찜 해제)                                           │
    // │ 매개변수: Long restaurantId — 찜 해제할 맛집의 고유번호                 │
    // │          Member loginMember — 현재 로그인한 회원 (찜을 해제하는 사람)    │
    // │ 리턴값:  void (없음) — 삭제 작업이므로 반환할 데이터 없음               │
    // │ 동작 흐름:                                                          │
    // │   1단계: 해당 회원 + 해당 맛집의 찜 데이터 조회 (없으면 예외)            │
    // │   2단계: DB에서 찜 데이터 삭제                                        │
    // └─────────────────────────────────────────────────────────────────────┘
    public void remove(Long restaurantId, Member loginMember) {

        // [1단계] 찜 데이터 조회
        // wishlistRepository.findByMemberIdAndRestaurantId(): DB에서 특정 회원이
        //   특정 맛집에 대해 찜한 데이터를 조회
        // loginMember.getId(): 현재 로그인한 회원의 ID
        // restaurantId: 찜 해제하려는 맛집의 ID
        // .orElseThrow(): 찜 데이터가 없으면 예외 발생 (찜하지 않은 맛집은 해제 불가)
        Wishlist wishlist = wishlistRepository
                .findByMemberIdAndRestaurantId(loginMember.getId(), restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("찜 정보를 찾을 수 없습니다."));

        // [2단계] DB에서 찜 데이터 삭제
        // wishlistRepository.delete(wishlist): DB에서 해당 찜 데이터를 DELETE(삭제)
        // → 삭제 후 해당 맛집은 회원의 찜 목록에서 사라짐
        wishlistRepository.delete(wishlist);
    }

    // ─── 찜 여부 확인 메서드 ──────────────────────────────────

    // @Transactional이 없음 → 클래스 레벨의 readOnly = true 적용
    // ┌─────────────────────────────────────────────────────────────────────┐
    // │ 메서드명: isWished (특정 맛집을 찜했는지 확인)                         │
    // │ 매개변수: Long memberId — 확인할 회원의 고유번호                       │
    // │          Long restaurantId — 확인할 맛집의 고유번호                    │
    // │ 리턴값:  boolean — 찜했으면 true, 아니면 false                        │
    // │ 동작: DB에서 해당 회원이 해당 맛집을 찜했는지 존재 여부 확인              │
    // │ 사용처: 맛집 상세 페이지에서 하트(찜) 버튼의 상태를 결정할 때 사용        │
    // │         (찜했으면 ♥ 채워진 하트, 아니면 ♡ 빈 하트)                     │
    // └─────────────────────────────────────────────────────────────────────┘
    public boolean isWished(Long memberId, Long restaurantId) {
        // wishlistRepository.existsByMemberIdAndRestaurantId(): DB에서 해당 조합의
        //   찜 데이터가 존재하는지 확인
        // → SQL: SELECT COUNT(*) > 0 FROM wishlist WHERE member_id = ? AND restaurant_id = ?
        // → 존재하면 true (이미 찜한 상태), 존재하지 않으면 false (찜하지 않은 상태)
        return wishlistRepository.existsByMemberIdAndRestaurantId(memberId, restaurantId);
    }
}
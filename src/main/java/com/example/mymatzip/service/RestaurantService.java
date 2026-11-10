// ============================================================
// [RestaurantService.java] 맛집(음식점) 비즈니스 로직 서비스
// ============================================================
// 역할: 맛집과 관련된 핵심 업무를 처리합니다.
//       맛집 등록, 수정, 삭제, 조회, 검색, 카테고리별 필터링 등
//       Controller로부터 요청을 받아 → Repository를 통해 DB를 조작합니다.
//
// MVC 아키텍처에서의 위치:
//   Controller (요청 접수) → ★Service (업무 처리)★ → Repository (DB 접근)
//
//
// 사용처:
//   - RestaurantController에서 맛집 CRUD(생성/조회/수정/삭제) 요청 시 호출
//   - 메인 페이지에서 최신 맛집 6개를 보여줄 때 findLatest6() 호출
//   - 검색 기능에서 search() 메서드 호출
//
// @Transactional 설명:
//   트랜잭션 = "하나의 작업 단위" (중간에 실패하면 전부 취소)
//   readOnly=true (기본): 조회 메서드에서 사용 → 성능 최적화
//   @Transactional (메서드 레벨): 등록/수정/삭제처럼 DB에 변경이 필요한 경우 사용
// ============================================================

// 이 파일이 속한 패키지(폴더) 선언 — service 패키지에 위치함을 나타냄
package com.example.mymatzip.service;

// ─── import 영역 ─────────────────────────────────────────────

// [Phase 5] PageResponse: 페이징 응답 래퍼 DTO — 페이지 정보와 데이터 목록을 함께 전달
import com.example.mymatzip.dto.PageResponse;

// RestaurantForm: 맛집 등록/수정 시 사용자가 입력한 정보를 담는 DTO(데이터 전달 객체)
//                 (이름, 카테고리, 주소, 전화번호, 설명 등)
import com.example.mymatzip.dto.RestaurantForm;

// RestaurantResponse: 맛집 정보를 화면(View)에 전달하기 위한 응답용 DTO
//                     (엔티티를 직접 노출하지 않고, 필요한 정보만 담아 전달)
import com.example.mymatzip.dto.RestaurantResponse;

// Category: 맛집의 카테고리(분류)를 나타내는 열거형 (한식, 중식, 양식 등)
import com.example.mymatzip.entity.Category;

// Member: 회원 정보를 담는 엔티티 — 맛집을 등록한 회원 정보를 참조하기 위해 사용
import com.example.mymatzip.entity.Member;

// Restaurant: 맛집 정보를 담는 엔티티 — DB의 restaurant 테이블과 1:1 대응
import com.example.mymatzip.entity.Restaurant;

// RestaurantRepository: DB의 restaurant 테이블에 접근하는 인터페이스 (CRUD 기능 제공)
import com.example.mymatzip.repository.RestaurantRepository;

// ReviewRepository: DB의 review 테이블에 접근하는 인터페이스
//                   → 맛집의 평균 평점을 계산하기 위해 사용
import com.example.mymatzip.repository.ReviewRepository;

// @RequiredArgsConstructor: final 필드에 대한 생성자를 자동 생성해주는 Lombok 어노테이션
// → 직접 생성자를 작성하지 않아도 의존성 주입(DI)이 자동으로 이루어짐
import lombok.RequiredArgsConstructor;

// @Service: 이 클래스가 "서비스 계층"임을 Spring에게 알려주는 어노테이션
// → Spring이 자동으로 이 클래스의 객체(Bean)를 생성하고 관리함
import org.springframework.stereotype.Service;

// @Transactional: DB 작업을 하나의 트랜잭션(작업 단위)으로 묶어주는 어노테이션
// → 작업 도중 에러 발생 시 모든 변경사항을 자동으로 되돌림 (rollback)
import org.springframework.transaction.annotation.Transactional;

// [Phase 5] Page, Pageable 가져오기 — 페이징 기능을 위한 클래스
// Pageable = 페이지 요청 정보 (몇 번째 페이지, 한 페이지에 몇 개)
// Page = 페이지 응답 결과 (데이터 + 페이징 메타 정보)
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// [팀 프로젝트] 입력값은 원문 그대로 저장하고, 화면에 출력할 때 타임리프 th:text 와 common.js 의 esc() 가 HTML 이스케이프합니다.

// List: 여러 개의 데이터를 순서대로 담을 수 있는 자바 컬렉션 (목록)
// → 맛집 목록을 담아 반환할 때 사용
import java.util.List;

// ─── 클래스 선언부 ────────────────────────────────────────────

// @Service: Spring에게 "이 클래스는 비즈니스 로직을 담당하는 서비스입니다"라고 등록
//           → Spring 컨테이너가 이 클래스의 인스턴스를 자동 생성·관리함
@Service

// @RequiredArgsConstructor: 아래의 final 필드들(restaurantRepository, reviewRepository)을
//                           매개변수로 받는 생성자를 Lombok이 자동 생성
//                           → Spring이 생성자를 통해 각 Repository를 자동 주입(DI)
@RequiredArgsConstructor

// @Transactional(readOnly = true): 이 클래스의 모든 메서드에 기본적으로 "읽기 전용" 트랜잭션 적용
//   - readOnly = true란? → DB에서 데이터를 "읽기만" 하겠다는 의미
//   - 장점: DB가 불필요한 잠금(Lock)을 걸지 않아 성능이 향상됨
//   - 쓰기가 필요한 메서드에는 별도로 @Transactional을 붙여 readOnly를 해제함
@Transactional(readOnly = true)
public class RestaurantService {

    // ─── 필드(의존성) 영역 ────────────────────────────────────

    // restaurantRepository: DB의 restaurant 테이블에 접근하기 위한 저장소 객체
    // final 키워드: 한 번 주입된 후 변경 불가 → 안전성 보장
    // 역할: 맛집 데이터의 조회, 저장, 수정, 삭제를 담당
    private final RestaurantRepository restaurantRepository;

    // reviewRepository: DB의 review 테이블에 접근하기 위한 저장소 객체
    // 역할: 각 맛집의 평균 평점을 계산하기 위해 사용
    //       (맛집 응답 DTO에 평균 평점을 포함시키기 위함)
    private final ReviewRepository reviewRepository;

    // ─── 전체 맛집 목록 조회 메서드 ───────────────────────────

    // @Transactional이 없음 → 클래스 레벨의 readOnly = true 적용
    // ┌─────────────────────────────────────────────────────────────────────┐
    // │ 메서드명: findAll (전체 맛집 목록 조회)                               │
    // │ 매개변수: 없음                                                      │
    // │ 리턴값:  List<RestaurantResponse> — 맛집 응답 DTO 목록               │
    // │ 동작 흐름:                                                          │
    // │   1단계: DB에서 모든 맛집 엔티티 조회                                 │
    // │   2단계: 각 맛집 엔티티를 응답 DTO로 변환 (평균 평점 포함)              │
    // │   3단계: 변환된 DTO 목록을 반환                                      │
    // └─────────────────────────────────────────────────────────────────────┘
    public List<RestaurantResponse> findAll() {
        // restaurantRepository.findAll(): DB에서 모든 맛집 데이터를 List<Restaurant>로 가져옴
        // .stream(): 리스트를 스트림(흐름)으로 변환 → 데이터를 하나씩 처리할 수 있게 함
        // .map(this::toResponse): 각 Restaurant 엔티티를 RestaurantResponse DTO로 변환
        //   → this::toResponse는 아래에 정의된 toResponse() 메서드를 참조
        //   → 각 맛집에 대해 평균 평점을 계산하여 DTO에 포함시킴
        // .toList(): 스트림의 결과를 다시 List(목록)로 변환하여 반환
        return restaurantRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    // ─── [Phase 5] 전체 맛집 목록 페이징 조회 메서드 ────────────

    // ┌─────────────────────────────────────────────────────────────────────┐
    // │ 메서드명: findAll (전체 맛집 목록 페이징 조회)                        │
    // │ 매개변수: Pageable pageable — 페이지 번호와 크기 정보                 │
    // │ 리턴값:  PageResponse<RestaurantResponse> — 페이징 정보 + 맛집 DTO   │
    // │ 동작 흐름:                                                          │
    // │   1단계: DB에서 해당 페이지의 맛집 엔티티 조회                         │
    // │   2단계: 각 맛집 엔티티를 응답 DTO로 변환 (평균 평점 포함)              │
    // │   3단계: PageResponse로 감싸서 반환                                  │
    // └─────────────────────────────────────────────────────────────────────┘
    public PageResponse<RestaurantResponse> findAll(Pageable pageable) {
        // restaurantRepository.findAll(pageable): 해당 페이지의 맛집만 DB에서 조회
        Page<Restaurant> page = restaurantRepository.findAll(pageable);

        // 페이지 내 맛집 엔티티들을 응답 DTO로 변환
        List<RestaurantResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .toList();

        // PageResponse.from(): 페이징 메타 정보 + DTO 목록을 합쳐서 반환
        return PageResponse.from(page, content);
    }

    // ─── 카테고리별 맛집 목록 조회 메서드 ─────────────────────

    // ┌─────────────────────────────────────────────────────────────────────┐
    // │ 메서드명: findByCategory (카테고리별 맛집 조회)                       │
    // │ 매개변수: Category category — 조회할 카테고리 (한식, 중식, 양식 등)     │
    // │ 리턴값:  List<RestaurantResponse> — 해당 카테고리의 맛집 응답 DTO 목록  │
    // │ 동작 흐름:                                                          │
    // │   1단계: DB에서 해당 카테고리에 속한 맛집만 조회                        │
    // │   2단계: 각 맛집을 응답 DTO로 변환 (평균 평점 포함)                     │
    // │   3단계: 변환된 DTO 목록을 반환                                      │
    // └─────────────────────────────────────────────────────────────────────┘
    public List<RestaurantResponse> findByCategory(Category category) {
        // restaurantRepository.findByCategory(category): DB에서 특정 카테고리의 맛집만 조회
        // .stream().map(this::toResponse).toList(): 각 엔티티를 DTO로 변환 후 리스트로 반환
        return restaurantRepository.findByCategory(category).stream()
                .map(this::toResponse)
                .toList();
    }

    // ─── [Phase 5] 카테고리별 맛집 목록 페이징 조회 메서드 ──────

    // ┌─────────────────────────────────────────────────────────────────────┐
    // │ 메서드명: findByCategory (카테고리별 맛집 페이징 조회)                 │
    // │ 매개변수: Category category — 조회할 카테고리                        │
    // │          Pageable pageable — 페이지 정보                           │
    // │ 리턴값:  PageResponse<RestaurantResponse> — 페이징 정보 + 맛집 DTO   │
    // └─────────────────────────────────────────────────────────────────────┘
    public PageResponse<RestaurantResponse> findByCategory(Category category, Pageable pageable) {
        Page<Restaurant> page = restaurantRepository.findByCategory(category, pageable);
        List<RestaurantResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .toList();
        return PageResponse.from(page, content);
    }

    // ─── 맛집 단건 조회 메서드 (응답 DTO 반환) ────────────────

    // ┌─────────────────────────────────────────────────────────────────────┐
    // │ 메서드명: findById (ID로 맛집 조회 → 응답 DTO 반환)                   │
    // │ 매개변수: Long id — 조회할 맛집의 고유번호 (DB의 Primary Key)           │
    // │ 리턴값:  RestaurantResponse — 맛집 응답 DTO (평균 평점 포함)           │
    // │ 동작 흐름:                                                          │
    // │   1단계: ID로 맛집 엔티티 조회 (없으면 예외 발생)                       │
    // │   2단계: 엔티티를 응답 DTO로 변환 후 반환                              │
    // │ 사용처: 맛집 상세 페이지에서 맛집 정보를 보여줄 때 사용                   │
    // └─────────────────────────────────────────────────────────────────────┘
    public RestaurantResponse findById(Long id) {
        // getRestaurantOrThrow(id): 아래에 정의된 private 메서드 호출
        // → ID로 맛집을 조회하고, 없으면 예외를 던짐
        Restaurant restaurant = getRestaurantOrThrow(id);

        // toResponse(restaurant): 조회한 맛집 엔티티를 응답 DTO로 변환 (평균 평점 포함)
        return toResponse(restaurant);
    }

    // ─── 맛집 단건 조회 메서드 (엔티티 직접 반환) ──────────────

    // ┌─────────────────────────────────────────────────────────────────────┐
    // │ 메서드명: findEntityById (ID로 맛집 엔티티 직접 반환)                  │
    // │ 매개변수: Long id — 조회할 맛집의 고유번호                             │
    // │ 리턴값:  Restaurant — 맛집 엔티티 객체 (DB 테이블과 1:1 대응)           │
    // │ 사용처: 다른 Service에서 맛집 엔티티가 필요할 때 사용                    │
    // │         (예: 리뷰 작성 시 어떤 맛집에 대한 리뷰인지 참조할 때)           │
    // └─────────────────────────────────────────────────────────────────────┘
    public Restaurant findEntityById(Long id) {
        // getRestaurantOrThrow(id): ID로 맛집을 조회하고, 없으면 예외를 던짐
        return getRestaurantOrThrow(id);
    }

    // ─── 맛집 등록(저장) 메서드 ───────────────────────────────

    // @Transactional: 이 메서드는 DB에 데이터를 "쓰는" 작업이므로
    //                 클래스 레벨의 readOnly=true를 덮어씀 (쓰기 가능 트랜잭션)
    @Transactional
    // ┌─────────────────────────────────────────────────────────────────────┐
    // │ 메서드명: save (맛집 등록)                                           │
    // │ 매개변수: RestaurantForm form — 사용자가 입력한 맛집 정보              │
    // │          Member loginMember — 현재 로그인한 회원 (맛집 등록자)          │
    // │ 리턴값:  RestaurantResponse — 등록된 맛집의 응답 DTO                  │
    // │ 동작 흐름:                                                          │
    // │   1단계: Restaurant 엔티티 객체 생성 (빌더 패턴)                       │
    // │   2단계: DB에 저장                                                  │
    // │   3단계: 저장된 엔티티를 응답 DTO로 변환 후 반환                        │
    // └─────────────────────────────────────────────────────────────────────┘
    public RestaurantResponse save(RestaurantForm form, Member loginMember) {

        // [1단계] Restaurant 엔티티 객체 생성 (빌더 패턴 사용)
        // 빌더 패턴: .속성명(값) 형태로 가독성 좋게 객체를 생성하는 디자인 패턴
        Restaurant restaurant = Restaurant.builder()
                // .name(): 사용자가 입력한 맛집 이름 설정
                // 원문 그대로 저장 (XSS 방어는 출력할 때: 타임리프 th:text, common.js esc())
                .name(form.getName())
                // .category(): 사용자가 선택한 카테고리 설정 (한식, 중식 등)
                .category(form.getCategory())
                // .address(): 사용자가 입력한 맛집 주소 설정 
                .address(form.getAddress())
                // .phone(): 사용자가 입력한 맛집 전화번호 설정
                .phone(form.getPhone())
                // .description(): 사용자가 입력한 맛집 설명 설정 
                .description(form.getDescription())
                // .member(): 맛집을 등록한 회원 정보 설정 → 누가 등록했는지 기록
                .member(loginMember)
                // [Phase 7] .imageUrl(): 대표 이미지 URL 설정 (선택값)
                .imageUrl(validImageUrl(form.getImageUrl()))   // [수정] 업로드 주소 형식만 허용
                // .build(): 위에서 설정한 값들로 최종 Restaurant 객체를 생성
                .build();

        // [2단계] DB에 맛집 정보 저장
        // restaurantRepository.save(restaurant): DB의 restaurant 테이블에 INSERT(삽입)
        // saved: DB가 자동 생성한 ID가 포함된 Restaurant 객체
        Restaurant saved = restaurantRepository.save(restaurant);

        // [3단계] 저장된 엔티티를 응답 DTO로 변환 후 반환
        // toResponse(): 엔티티 → 응답 DTO 변환 (평균 평점도 함께 계산)
        return toResponse(saved);
    }

    // ─── 맛집 수정 메서드 ─────────────────────────────────────

    // @Transactional: DB에 데이터를 "수정"하는 작업이므로 쓰기 가능 트랜잭션 적용
    @Transactional
    // ┌─────────────────────────────────────────────────────────────────────┐
    // │ 메서드명: update (맛집 정보 수정)                                     │
    // │ 매개변수: Long id — 수정할 맛집의 고유번호                             │
    // │          RestaurantForm form — 새로 입력한 맛집 정보                  │
    // │          Member loginMember — 현재 로그인한 회원 (권한 확인용)          │
    // │ 리턴값:  RestaurantResponse — 수정된 맛집의 응답 DTO                  │
    // │ 동작 흐름:                                                          │
    // │   1단계: ID로 맛집 조회 (없으면 예외)                                 │
    // │   2단계: 수정 권한 확인 (등록자 본인만 수정 가능)                       │
    // │   3단계: 맛집 정보 수정 (JPA 변경 감지로 자동 UPDATE)                   │
    // │   4단계: 수정된 엔티티를 응답 DTO로 변환 후 반환                        │
    // └─────────────────────────────────────────────────────────────────────┘
    public RestaurantResponse update(Long id, RestaurantForm form, Member loginMember) {

        // [1단계] ID로 맛집 조회 → 없으면 예외 발생
        Restaurant restaurant = getRestaurantOrThrow(id);

        // [2단계] 수정 권한 확인
        // validateOwner(): 맛집 등록자와 로그인한 회원이 같은 사람인지 확인
        // → 다른 사람이면 "수정 권한이 없습니다" 예외 발생
        validateOwner(restaurant, loginMember);

        // [3단계] 맛집 정보 수정
        // restaurant.update(): 엔티티의 필드 값들을 새 값으로 변경
        // JPA의 "변경 감지(Dirty Checking)" 기능 덕분에
        // 엔티티의 값을 변경하면 트랜잭션 종료 시 자동으로 DB에 UPDATE 쿼리가 실행됨
        // → 별도로 save()를 호출하지 않아도 됨!
        restaurant.update(
                form.getName(),         // 새로운 맛집 이름 
                form.getCategory(),                             // 새로운 카테고리
                form.getAddress(),      // 새로운 주소 
                form.getPhone(),                                // 새로운 전화번호
                form.getDescription(),  // 새로운 설명 
                validImageUrl(form.getImageUrl())               // [Phase 7] 새로운 이미지 URL ([수정] 형식 검사)
        );

        // [4단계] 수정된 엔티티를 응답 DTO로 변환 후 반환
        return toResponse(restaurant);
    }

    // ─── 맛집 삭제 메서드 ─────────────────────────────────────

    // @Transactional: DB에서 데이터를 "삭제"하는 작업이므로 쓰기 가능 트랜잭션 적용
    @Transactional
    // ┌─────────────────────────────────────────────────────────────────────┐
    // │ 메서드명: delete (맛집 삭제)                                         │
    // │ 매개변수: Long id — 삭제할 맛집의 고유번호                             │
    // │          Member loginMember — 현재 로그인한 회원 (권한 확인용)          │
    // │ 리턴값:  void (없음) — 삭제 작업이므로 반환할 데이터 없음               │
    // │ 동작 흐름:                                                          │
    // │   1단계: ID로 맛집 조회 (없으면 예외)                                 │
    // │   2단계: 삭제 권한 확인 (등록자 본인 또는 관리자만 삭제 가능)            │
    // │   3단계: DB에서 맛집 삭제                                            │
    // └─────────────────────────────────────────────────────────────────────┘
    public void delete(Long id, Member loginMember) {

        // [1단계] ID로 맛집 조회 → 없으면 예외 발생
        Restaurant restaurant = getRestaurantOrThrow(id);

        // [2단계] 삭제 권한 확인
        // isOwner: 맛집 등록자의 ID와 로그인한 회원의 ID가 같은지 확인
        // restaurant.getMember().getId(): 맛집을 등록한 회원의 ID
        // loginMember.getId(): 현재 로그인한 회원의 ID
        // .equals(): 두 값이 같으면 true (본인이면 true)
        boolean isOwner = restaurant.getMember().getId().equals(loginMember.getId());

        // isAdmin: 로그인한 회원이 관리자(ADMIN) 권한인지 확인
        // loginMember.getRole().name(): 회원의 역할을 문자열로 변환 (예: "ADMIN", "USER")
        // .equals("ADMIN"): 관리자 권한이면 true
        boolean isAdmin = loginMember.getRole().name().equals("ADMIN");

        // 등록자 본인도 아니고(!isOwner) 관리자도 아니면(!isAdmin) → 삭제 불가
        // &&: AND 연산 → 둘 다 false일 때만 예외 발생
        if (!isOwner && !isAdmin) {
            // 삭제 권한이 없는 회원이 삭제를 시도하면 예외 발생
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        // [3단계] DB에서 맛집 삭제
        // restaurantRepository.delete(restaurant): DB에서 해당 맛집 데이터를 DELETE(삭제)
        restaurantRepository.delete(restaurant);
    }

    // ─── 맛집 검색 메서드 ─────────────────────────────────────

    // ┌─────────────────────────────────────────────────────────────────────┐
    // │ 메서드명: search (키워드로 맛집 검색)                                 │
    // │ 매개변수: String keyword — 사용자가 입력한 검색어                      │
    // │ 리턴값:  List<RestaurantResponse> — 검색 결과 맛집 응답 DTO 목록       │
    // │ 동작 흐름:                                                          │
    // │   1단계: DB에서 키워드로 맛집 검색 (이름, 주소 등에서 검색)              │
    // │   2단계: 검색된 맛집들을 응답 DTO로 변환 후 반환                        │
    // └─────────────────────────────────────────────────────────────────────┘
    public List<RestaurantResponse> search(String keyword) {
        // restaurantRepository.searchByKeyword(keyword): DB에서 키워드가 포함된 맛집을 검색
        // → Repository에서 정의한 커스텀 쿼리를 사용 (LIKE '%keyword%' 등)
        // .stream().map(this::toResponse).toList(): 각 엔티티를 DTO로 변환 후 리스트로 반환
        return restaurantRepository.searchByKeyword(keyword).stream()
                .map(this::toResponse)
                .toList();
    }

    // ─── [Phase 5] 키워드 검색 + 페이징 메서드 ──────────────────

    // ┌─────────────────────────────────────────────────────────────────────┐
    // │ 메서드명: search (키워드로 맛집 검색 — 페이징 버전)                    │
    // │ 매개변수: String keyword — 검색어                                    │
    // │          Pageable pageable — 페이지 정보                           │
    // │ 리턴값:  PageResponse<RestaurantResponse> — 페이징 정보 + 검색 결과   │
    // └─────────────────────────────────────────────────────────────────────┘
    public PageResponse<RestaurantResponse> search(String keyword, Pageable pageable) {
        Page<Restaurant> page = restaurantRepository.searchByKeyword(keyword, pageable);
        List<RestaurantResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .toList();
        return PageResponse.from(page, content);
    }

    // ─── 최신 맛집 6개 조회 메서드 ────────────────────────────

    // ┌─────────────────────────────────────────────────────────────────────┐
    // │ 메서드명: findLatest6 (최신 맛집 6개 조회)                            │
    // │ 매개변수: 없음                                                      │
    // │ 리턴값:  List<RestaurantResponse> — 최근 등록된 맛집 6개의 응답 DTO    │
    // │ 동작 흐름:                                                          │
    // │   1단계: DB에서 생성일(createdAt) 기준 내림차순으로 상위 6개 조회        │
    // │   2단계: 각 맛집을 응답 DTO로 변환 후 반환                             │
    // │ 사용처: 메인 페이지에서 "최근 등록된 맛집"을 보여줄 때 사용              │
    // └─────────────────────────────────────────────────────────────────────┘
    public List<RestaurantResponse> findLatest6() {
        // restaurantRepository.findTop6ByOrderByCreatedAtDesc():
        //   → Spring Data JPA의 메서드 이름 규칙으로 자동 생성된 쿼리
        //   → findTop6: 상위 6개만 조회
        //   → ByOrderByCreatedAtDesc: createdAt(생성일) 기준 내림차순(최신순) 정렬
        // .stream().map(this::toResponse).toList(): 각 엔티티를 DTO로 변환 후 리스트로 반환
        return restaurantRepository.findTop6ByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    // ─── private 유틸리티 메서드 영역 ─────────────────────────
    // private: 이 클래스 내부에서만 사용되는 메서드 (외부에서 호출 불가)
    // 코드의 중복을 줄이고, 가독성을 높이기 위해 공통 로직을 분리한 것

    // ┌─────────────────────────────────────────────────────────────────────┐
    // │ 메서드명: getRestaurantOrThrow (맛집 조회 또는 예외 발생)              │
    // │ 매개변수: Long id — 조회할 맛집의 고유번호                             │
    // │ 리턴값:  Restaurant — 조회된 맛집 엔티티                              │
    // │ 동작: ID로 맛집 조회 → 없으면 "맛집을 찾을 수 없습니다" 예외 발생       │
    // │ 사용처: findById, update, delete 등 여러 메서드에서 공통으로 사용       │
    // └─────────────────────────────────────────────────────────────────────┘
    // [추가] 이미지 주소 검사: 비어 있으면 null, 아니면 FileService 가 돌려주는
    //   "/uploads/UUID.확장자" 형식만 허용합니다. 화면이 이 값을 <img src="..."> 에 그대로 넣기 때문에
    //   따옴표나 다른 사이트 주소가 들어가지 않게 막습니다.
    private static final java.util.regex.Pattern IMAGE_URL =
            java.util.regex.Pattern.compile("^/uploads/[0-9a-fA-F-]{36}\\.(jpg|jpeg|png|gif|webp)$");

    private String validImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        if (!IMAGE_URL.matcher(imageUrl).matches()) {
            throw new IllegalArgumentException("사진 주소가 올바르지 않습니다. 사진은 업로드 버튼으로 올려 주세요.");
        }
        return imageUrl;
    }

    private Restaurant getRestaurantOrThrow(Long id) {
        // restaurantRepository.findById(id): DB에서 해당 ID의 맛집 조회
        // → 결과는 Optional<Restaurant> (값이 있을 수도, 없을 수도 있음)
        // .orElseThrow(): 값이 없으면(맛집이 없으면) 예외를 던짐
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("맛집을 찾을 수 없습니다."));
    }

    // ┌─────────────────────────────────────────────────────────────────────┐
    // │ 메서드명: validateOwner (소유자(등록자) 검증)                         │
    // │ 매개변수: Restaurant restaurant — 검증 대상 맛집                      │
    // │          Member loginMember — 현재 로그인한 회원                      │
    // │ 리턴값:  void (없음) — 검증 실패 시 예외를 던짐                        │
    // │ 동작: 등록자도 관리자도 아니면 "수정 권한이 없습니다" 예외 발생        │
    // └─────────────────────────────────────────────────────────────────────┘
    private void validateOwner(Restaurant restaurant, Member loginMember) {
        // restaurant.getMember().getId(): 맛집을 등록한 회원의 ID
        // loginMember.getId(): 현재 로그인한 회원의 ID
        // !...equals(...): 두 ID가 같지 않으면 → 본인이 아니므로 권한 없음
        // [수정] 관리자(ADMIN)도 수정할 수 있게 합니다. 상세 화면은 관리자에게도 수정 버튼을 보여 주고,
        //   삭제(delete)도 관리자를 허용하므로 같은 규칙으로 맞춥니다.
        boolean isOwner = restaurant.getMember().getId().equals(loginMember.getId());
        boolean isAdmin = loginMember.getRole().name().equals("ADMIN");
        if (!isOwner && !isAdmin) {
            // 등록자도 관리자도 아닌 사람이 수정하려고 하면 예외 발생
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }
    }

    // ┌─────────────────────────────────────────────────────────────────────┐
    // │ 메서드명: toResponse (엔티티 → 응답 DTO 변환)                        │
    // │ 매개변수: Restaurant restaurant — 변환할 맛집 엔티티                   │
    // │ 리턴값:  RestaurantResponse — 변환된 응답 DTO (평균 평점 포함)          │
    // │ 동작 흐름:                                                          │
    // │   1단계: 해당 맛집의 평균 평점 계산                                   │
    // │   2단계: 엔티티 + 평균 평점을 합쳐서 응답 DTO 생성                     │
    // │ 목적: 엔티티를 직접 외부에 노출하지 않고, 필요한 정보만 가공하여 전달     │
    // └─────────────────────────────────────────────────────────────────────┘
    private RestaurantResponse toResponse(Restaurant restaurant) {
        // reviewRepository.averageRatingByRestaurantId(): 해당 맛집에 달린 리뷰들의 평균 평점 계산
        // → DB에서 AVG(rating) 쿼리를 실행한 결과 (리뷰가 없으면 null)
        Double avgRating = reviewRepository.averageRatingByRestaurantId(restaurant.getId());

        // RestaurantResponse.from(): 정적 팩토리 메서드를 사용하여 응답 DTO 생성
        // → Restaurant 엔티티의 정보 + 평균 평점(avgRating)을 합쳐서 DTO를 만듦
        return RestaurantResponse.from(restaurant, avgRating);
    }
}

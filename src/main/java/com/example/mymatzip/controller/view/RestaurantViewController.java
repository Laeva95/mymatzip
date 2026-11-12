/*
 * ============================================================================
 *  파일명    : RestaurantViewController.java
 *  위치      : com.example.mymatzip.controller.view (뷰 컨트롤러 패키지)
 *  역할      : 맛집 관련 화면(목록, 상세, 등록폼, 수정폼)을 반환하는 "맛집 안내 데스크"
 * ============================================================================
 *
 *
 *  ■ MVC에서의 위치
 *    Controller 계층 중 "View Controller" (화면 반환 컨트롤러)에 속합니다.
 *    서버에서 데이터를 미리 담아서 타임리프 화면에 전달합니다. (서버 사이드 렌더링)
 *
 *    [브라우저] → [이 Controller]
 *                   ├→ [RestaurantService] → [DB에서 맛집 데이터 조회]
 *                   ├→ [ReviewService]     → [DB에서 리뷰 데이터 조회]
 *                   └→ [WishlistService]   → [DB에서 찜 여부 확인]
 *                        ↓
 *                  [Model에 데이터 담기]
 *                        ↓
 *                  [타임리프 화면 렌더링 → HTML 반환]
 *
 *  ■ URL 매핑 요약
 *    기본 경로: /restaurants
 *    ┌────────────────────────┬────────┬────────────────────────────────────┐
 *    │ URL                    │ Method │ 설명                               │
 *    ├────────────────────────┼────────┼────────────────────────────────────┤
 *    │ /restaurants           │ GET    │ 맛집 목록 (카테고리 필터 가능)      │
 *    │ /restaurants/{id}      │ GET    │ 맛집 상세 (리뷰, 평균별점, 찜 포함) │
 *    │ /restaurants/new       │ GET    │ 맛집 등록 폼                       │
 *    │ /restaurants/{id}/edit │ GET    │ 맛집 수정 폼                       │
 *    └────────────────────────┴────────┴────────────────────────────────────┘
 * ============================================================================
 */

// ── 패키지 선언 ──────────────────────────────────────────────
package com.example.mymatzip.controller.view;

// ── import 문 (외부 라이브러리 가져오기) ─────────────────────

// Category: 맛집 카테고리 열거형(enum)
// 예: KOREAN(한식), JAPANESE(일식), CHINESE(중식), WESTERN(양식) 등
// 목록 페이지에서 카테고리 필터, 등록/수정 폼에서 카테고리 선택에 사용
import com.example.mymatzip.dto.PageResponse;
import com.example.mymatzip.dto.RestaurantResponse;
import com.example.mymatzip.entity.Category;

// RestaurantService: 맛집 관련 비즈니스 로직 서비스
import com.example.mymatzip.service.RestaurantService;

// ReviewService: 리뷰 관련 비즈니스 로직 서비스
import com.example.mymatzip.service.ReviewService;

// WishlistService: 찜 관련 비즈니스 로직 서비스
import com.example.mymatzip.service.WishlistService;

// Member: 회원 엔티티 — 로그인 정보 확인 시 사용
import com.example.mymatzip.entity.Member;

// @LoginMember: 현재 로그인한 회원 정보를 컨트롤러 매개변수에 자동 주입하는 커스텀 어노테이션
import com.example.mymatzip.config.security.LoginMember;

// @RequiredArgsConstructor: final 필드의 생성자를 자동 생성하는 Lombok 어노테이션
import lombok.RequiredArgsConstructor;

// @Controller: 이 클래스가 뷰(View) 컨트롤러임을 선언 (타임리프 화면 이름 반환)
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;

// Model: Controller에서 타임리프 화면으로 데이터를 전달하는 "데이터 가방"
import org.springframework.ui.Model;

// @GetMapping: HTTP GET 요청을 처리하는 메서드에 붙이는 어노테이션
import org.springframework.web.bind.annotation.GetMapping;

// @PathVariable: URL 경로에서 변수 값을 추출하는 어노테이션
// 예: /restaurants/{id} → @PathVariable Long id
import org.springframework.web.bind.annotation.PathVariable;

// @RequestMapping: 기본 URL 경로를 설정하는 어노테이션
import org.springframework.web.bind.annotation.RequestMapping;

// @RequestParam: URL 쿼리 파라미터 값을 추출하는 어노테이션
// 예: /restaurants?category=KOREAN → @RequestParam Category category
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Value;   // [추가]2026.11.12권성애
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// ── @Controller 어노테이션 ──────────────────────────────────
// 이 클래스가 뷰(View) 컨트롤러임을 선언
// 메서드 반환값(String)이 타임리프 화면 파일 이름으로 해석됨
@Controller

// ── @RequestMapping("/restaurants") 어노테이션 ──────────────
// 기본 URL 경로: /restaurants
// 이 클래스의 모든 메서드 URL 앞에 "/restaurants"가 자동으로 붙음
@RequestMapping("/restaurants")

// ── @RequiredArgsConstructor ────────────────────────────────
// final 필드 3개(restaurantService, reviewService, wishlistService)에 대한
// 생성자를 자동 생성 → 의존성 주입(DI)
@RequiredArgsConstructor
// ── 클래스 선언 ─────────────────────────────────────────────
public class RestaurantViewController {

    // ── 필드 (멤버 변수) ────────────────────────────────────

    // RestaurantService: 맛집 관련 비즈니스 로직 담당
    // 맛집 목록 조회, 상세 조회, 카테고리별 조회 등에 사용
    private final RestaurantService restaurantService;

    // ReviewService: 리뷰 관련 비즈니스 로직 담당
    // 맛집 상세 페이지에서 리뷰 목록과 평균 별점을 가져올 때 사용
    private final ReviewService reviewService;

    // WishlistService: 찜 관련 비즈니스 로직 담당
    // 맛집 상세 페이지에서 현재 사용자가 이 맛집을 찜했는지 확인할 때 사용
    private final WishlistService wishlistService;


    // ── [추가]2026.11.12권성애 설정 값 (final 이 아니라 @RequiredArgsConstructor 생성자에 들어가지 않고 @Value 로 따로 채워짐) ──
    // 한 페이지에 보여 줄 맛집 수 — application.properties: app.restaurant.page-size=12 (없으면 12)
    @Value("${app.restaurant.page-size:12}")
    private int pageSize;

    // 페이지 번호를 몇 개씩 묶어 보여 줄지 — app.restaurant.page-block=10 (없으면 10) → « 1 2 … 10 »
    @Value("${app.restaurant.page-block:10}")
    private int pageBlock;


    // [추가]2026.11.12권성애 카카오 지도 JS 키 (application.properties kakao.map.js-key, 없으면 빈 값)
    //   form.html 이 지도 SDK 스크립트를 불러올 때 사용 → 등록 · 수정 화면에 model 로 넘김
    @Value("${kakao.map.js-key:}")
    private String kakaoJsKey;


    // ── GET /restaurants ────────────────────────────────────
    // 맛집 목록 페이지: 전체 맛집 또는 카테고리별 맛집 목록 화면
    // 요청 예시 1: GET http://localhost:8080/restaurants (전체 목록)
    // 요청 예시 2: GET http://localhost:8080/restaurants?category=KOREAN (한식만)
    // 응답: restaurant/list.html 페이지를 HTML로 렌더링하여 반환
    //
    // @GetMapping: GET 방식으로 "/restaurants" 요청이 오면 실행
    //   (경로를 지정하지 않으면 클래스의 @RequestMapping 경로 그대로 사용)
    // 매개변수:
    //   - @RequestParam(required = false) Category category:
    //     URL 쿼리 파라미터 "category"의 값을 Category enum으로 변환
    //     required = false: 선택적 (없으면 null)
    //     예: ?category=KOREAN → Category.KOREAN
    //   - Model model: 타임리프 화면에 데이터를 전달하기 위한 객체
    // 반환값: String "restaurant/list"
    //   → templates/restaurant/list.html 렌더링
    // [변경]2026.11.12권성애 (페이징: page · RedirectAttributes 매개변수 추가)
    //   - @RequestParam(required = false) String page: 보고 싶은 페이지 번호 (1부터, 없으면 1)
    //     숫자가 아닌 값(?page=abc)도 직접 검사하려고 int 가 아니라 String 으로 받음
    //     (int 로 받으면 스프링이 먼저 400 오류 화면을 냄)
    //   - RedirectAttributes redirectAttributes: 잘못된 page 일 때 1페이지로 보내면서 안내 문구(alertMsgNoPage)를 한 번만 넘김
    @GetMapping
    public String list(@RequestParam(required = false) Category category,
                       @RequestParam(required = false) String page,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        // ── [추가]2026.11.12권성애 0단계: page 값 검사 ─────────────────────
        // 없으면 1페이지, 숫자가 아니거나 1보다 작으면 → 안내 후 1페이지로 redirect
        int pageNumber = 1;
        if (page != null && !page.isBlank()) {
            try {
                pageNumber = Integer.parseInt(page.trim());
            } catch (NumberFormatException e) {
                pageNumber = 0;   // 숫자가 아님 → 아래에서 잘못된 페이지로 처리
            }
        }
        // 1페이지로 돌아갈 주소 (카테고리를 고른 상태면 카테고리는 유지)
        String firstPageUrl = (category != null) ? "redirect:/restaurants?category=" + category.name() : "redirect:/restaurants";
        if (pageNumber < 1) {
            redirectAttributes.addFlashAttribute("alertMsgNoPage", "없는 페이지입니다. 1페이지로 이동합니다.");
            return firstPageUrl;
        }

        // ── 1단계: 카테고리 필터링 여부에 따라 맛집 목록 조회 ─
        // [변경]2026.11.12권성애 (전체 조회 → 페이지 조회: 한 페이지 pageSize 개, 최신 등록순)
        //   PageRequest.of(페이지번호, 개수, 정렬): 스프링 페이지 번호는 0부터라 화면 번호(1부터)에서 1을 뺌
        //   정렬: 최신 등록순(createdAt 내림차순), 같은 시각이면 id 내림차순 (배치로 같은 시각에 많이 들어가도 순서가 매번 같게)
        //   서비스의 findByCategory(category, pageable) · findAll(pageable) 은 이미 있던 페이징 메서드 (삭제된 맛집 제외)
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
        PageResponse<RestaurantResponse> result;
        // category가 null이 아니면 (카테고리가 지정된 경우)
        if (category != null) {
            // 해당 카테고리의 맛집만 조회 ([변경]2026.11.12권성애 페이지 단위)
            result = restaurantService.findByCategory(category, pageable);

            // 현재 선택된 카테고리를 "selectedCategory"로 Model에 담음
            // 타임리프 화면에서 카테고리 버튼의 활성화 상태를 표시하는 데 사용
            model.addAttribute("selectedCategory", category);
        } else {
            // 카테고리가 지정되지 않은 경우 → 전체 맛집 목록 조회 ([변경]2026.11.12권성애 페이지 단위)
            result = restaurantService.findAll(pageable);
        }

        // [추가]2026.11.12권성애 전체 페이지보다 큰 번호(?page=999) → 안내 후 1페이지로 (맛집이 하나도 없으면 1페이지는 그대로 보여 줌)
        if (pageNumber > 1 && pageNumber > result.getTotalPages()) {
            redirectAttributes.addFlashAttribute("alertMsgNoPage", "없는 페이지입니다. 1페이지로 이동합니다.");
            return firstPageUrl;
        }

        // "restaurants"로 Model에 담음 — 타임리프 화면에서 ${restaurants}로 접근 (이름 그대로)
        // [변경]2026.11.12권성애 (전체 목록 → 이번 페이지 목록만: PageResponse.getContent())
        model.addAttribute("restaurants", result.getContent());

        // ── [추가]2026.11.12권성애 1-1단계: 페이지 번호 정보 (list.html 아래쪽 « 1 2 … 10 ») ─
        //   currentPage : 지금 페이지 (1부터)
        //   totalPages  : 전체 페이지 수 (맛집이 없으면 0)
        //   startPage · endPage : 지금 보여 줄 번호 묶음 (pageBlock 개씩)
        //     예) pageBlock=10, 지금 13페이지 → 11 ~ 20 (전체가 15페이지면 11 ~ 15)
        int startPage = ((pageNumber - 1) / pageBlock) * pageBlock + 1;
        int endPage = Math.min(startPage + pageBlock - 1, result.getTotalPages());
        model.addAttribute("currentPage", pageNumber);
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);

        // ── 2단계: 모든 카테고리 목록을 Model에 담기 ─────────
        // Category.values(): Category enum의 모든 값을 배열로 반환
        //   예: [KOREAN, JAPANESE, CHINESE, WESTERN, ...]
        // 타임리프 화면에서 카테고리 필터 버튼들을 생성하는 데 사용
        model.addAttribute("categories", Category.values());

        // ── 3단계: 타임리프 화면 이름 반환 ─────────────────────────
        // → templates/restaurant/list.html 렌더링
        return "restaurant/list";
    }

    // ── GET /restaurants/{id} ───────────────────────────────
    // 맛집 상세 페이지: 특정 맛집의 상세 정보, 리뷰 목록, 평균 별점, 찜 여부 표시
    // 요청 예시: GET http://localhost:8080/restaurants/1
    // 응답: restaurant/detail.html 페이지를 HTML로 렌더링하여 반환
    //
    // @GetMapping("/{id}"): GET 방식으로 "/restaurants/숫자" 요청이 오면 실행
    // 매개변수:
    //   - @PathVariable Long id: URL에서 맛집 ID를 추출
    //     예: /restaurants/3 → id = 3
    //   - Model model: 타임리프 화면에 데이터를 전달하기 위한 객체
    //   - @LoginMember Member loginMember: Spring Security에서 자동으로 로그인 회원 정보 주입
    //     (비로그인 시 null)
    // 반환값: String "restaurant/detail"
    //   → templates/restaurant/detail.html 렌더링
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model,
                         @LoginMember Member loginMember) {
        // ── 1단계: 맛집 상세 정보를 Model에 담기 ────────────
        // restaurantService.findById(id): 해당 ID의 맛집을 DB에서 조회
        // "restaurant"라는 이름으로 Model에 담음 → 타임리프 화면에서 ${restaurant}로 접근
        model.addAttribute("restaurant", restaurantService.findById(id));

        // ── 2단계: 해당 맛집의 리뷰 목록을 Model에 담기 ─────
        // reviewService.findByRestaurantId(id): 이 맛집에 달린 모든 리뷰 조회
        // "reviews"로 Model에 담음 → 타임리프 화면에서 ${reviews}로 접근
        model.addAttribute("reviews", reviewService.findByRestaurantId(id));

        // ── 3단계: 평균 별점을 Model에 담기 ─────────────────
        // reviewService.getAverageRating(id): 이 맛집의 리뷰 평균 별점 계산
        //   예: 리뷰 3개의 별점이 5, 4, 3이면 → 평균 4.0
        // "averageRating"으로 Model에 담음 → 타임리프 화면에서 ${averageRating}으로 접근
        model.addAttribute("averageRating", reviewService.getAverageRating(id));

        // ── 4단계: 찜 여부 확인 (로그인한 경우에만) ──────────
        // @LoginMember가 SecurityContext에서 자동으로 로그인 회원을 주입합니다.
        // 비로그인 시 loginMember는 null입니다.
        if (loginMember != null) {
            // wishlistService.isWished(loginMember.getId(), id):
            //   이 회원이 이 맛집을 찜했는지 확인 (true/false)
            // "isWished"로 Model에 담음 → 타임리프 화면에서 ${isWished}로 접근
            //   true → 빨간 하트 표시 (이미 찜한 상태)
            //   false → 빈 하트 표시 (찜하지 않은 상태)
            model.addAttribute("isWished", wishlistService.isWished(loginMember.getId(), id));
        }

        // ── 5단계: 타임리프 화면 이름 반환 ─────────────────────────
        // → templates/restaurant/detail.html 렌더링
        return "restaurant/detail";
    }

    // ── GET /restaurants/new ────────────────────────────────
    // 맛집 등록 폼 페이지: 새로운 맛집을 등록하는 입력 화면을 보여줌
    // 요청 예시: GET http://localhost:8080/restaurants/new
    //   (맛집 목록 페이지에서 "새 맛집 등록" 버튼 클릭 시)
    // 응답: restaurant/form.html 페이지를 HTML로 렌더링하여 반환
    //
    // @GetMapping("/new"): GET 방식으로 "/restaurants/new" 요청이 오면 실행
    // 매개변수:
    //   - Model model: 타임리프 화면에 카테고리 목록을 전달하기 위한 객체
    // 반환값: String "restaurant/form"
    //   → templates/restaurant/form.html 렌더링
    @GetMapping("/new")
    public String createForm(Model model) {
        // ── 1단계: 카테고리 목록을 Model에 담기 ─────────────
        // Category.values(): 모든 카테고리 값을 배열로 가져옴
        // 타임리프 화면의 카테고리 선택 드롭다운(select) 메뉴를 만드는 데 사용
        // 예: <select> 태그 안에 한식, 일식, 중식, 양식 등의 옵션을 생성
        model.addAttribute("categories", Category.values());

        // ── 2단계: 타임리프 화면 이름 반환 ─────────────────────────
        // → templates/restaurant/form.html 렌더링
        // 등록 폼과 수정 폼이 같은 타임리프 화면 파일(form.html)을 공유합니다.
        // 등록 시에는 "restaurant" 데이터가 Model에 없으므로 빈 폼이 표시됩니다.
        model.addAttribute("kakaoJsKey", kakaoJsKey);   // [추가]2026.11.12권성애 카카오 지도 JS 키
        return "restaurant/form";
    }

    // ── GET /restaurants/{id}/edit ──────────────────────────
    // 맛집 수정 폼 페이지: 기존 맛집 정보가 채워진 수정 화면을 보여줌
    // 요청 예시: GET http://localhost:8080/restaurants/1/edit
    //   (맛집 상세 페이지에서 "수정" 버튼 클릭 시)
    // 응답: restaurant/form.html 페이지를 HTML로 렌더링하여 반환
    //   (등록 폼과 같은 타임리프 화면를 사용하지만, 기존 데이터가 미리 채워져 있음)
    //
    // @GetMapping("/{id}/edit"): GET 방식으로 "/restaurants/숫자/edit" 요청이 오면 실행
    // 매개변수:
    //   - @PathVariable Long id: 수정할 맛집의 ID (URL에서 추출)
    //   - Model model: 타임리프 화면에 맛집 데이터와 카테고리 목록을 전달하기 위한 객체
    // 반환값: String "restaurant/form"
    //   → templates/restaurant/form.html 렌더링
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        // ── 1단계: 수정할 맛집의 기존 데이터를 Model에 담기 ──
        // restaurantService.findById(id): 해당 ID의 맛집을 DB에서 조회
        // "restaurant"로 Model에 담음 → 타임리프 화면에서 ${restaurant}로 접근
        // 타임리프 화면의 입력 폼에 기존 값이 미리 채워져서 표시됩니다.
        //   예: <input value="${restaurant.name}"> → 기존 맛집 이름이 표시됨
        model.addAttribute("restaurant", restaurantService.findById(id));

        // ── 2단계: 카테고리 목록을 Model에 담기 ─────────────
        // Category.values(): 모든 카테고리 값을 배열로 가져옴
        // 타임리프 화면의 카테고리 선택 드롭다운에서 기존 선택값을 표시하는 데 사용
        model.addAttribute("categories", Category.values());
        model.addAttribute("kakaoJsKey", kakaoJsKey);   // [추가]2026.11.12권성애 카카오 지도 JS 키

        // ── 3단계: 타임리프 화면 이름 반환 ─────────────────────────
        // → templates/restaurant/form.html 렌더링
        // 등록 폼과 같은 타임리프 화면를 사용하지만:
        //   - "restaurant" 데이터가 Model에 있으면 → 수정 모드 (기존 데이터 표시)
        //   - "restaurant" 데이터가 Model에 없으면 → 등록 모드 (빈 폼)
        return "restaurant/form";
    }
}

/*
 * ============================================================================
 *  파일명    : HomeController.java
 *  위치      : com.example.mymatzip.controller.view (뷰 컨트롤러 패키지)
 *  역할      : 메인(홈) 페이지와 찜 목록 페이지의 화면(HTML/타임리프 화면)을 반환하는 "안내 데스크"
 * ============================================================================
 *
 *
 *  ■ MVC에서의 위치
 *    Controller 계층 중 "View Controller" (화면 반환 컨트롤러)에 속합니다.
 *    API Controller(@RestController)는 JSON 데이터를 반환하지만,
 *    View Controller(@Controller)는 타임리프 화면 이름을 반환합니다.
 *
 *    [브라우저] → [이 Controller] → [타임리프 화면 렌더링] → [HTML을 브라우저에 전달]
 *
 *  ■ @Controller vs @RestController 차이
 *    @Controller: 메서드가 "뷰 이름"(문자열)을 반환 → 타임리프 화면 파일을 찾아서 HTML로 변환
 *    @RestController: 메서드가 "데이터"(객체)를 반환 → JSON으로 변환
 *
 *  ■ URL 매핑 요약
 *    ┌─────────────┬────────┬──────────────────────────────────────────┐
 *    │ URL         │ Method │ 설명                                     │
 *    ├─────────────┼────────┼──────────────────────────────────────────┤
 *    │ /           │ GET    │ 메인 페이지 (최신 맛집 6개 표시)          │
 *    │ /wishlists  │ GET    │ 찜 목록 페이지                           │
 *    └─────────────┴────────┴──────────────────────────────────────────┘
 * ============================================================================
 */

// ── 패키지 선언 ──────────────────────────────────────────────
// 이 파일이 controller.view 패키지에 속함을 선언합니다.
// view 패키지: HTML 화면(타임리프 화면)을 반환하는 뷰 컨트롤러들이 모여있는 곳
package com.example.mymatzip.controller.view;

// ── import 문 (외부 라이브러리 가져오기) ─────────────────────

// RestaurantService: 맛집 관련 비즈니스 로직을 처리하는 서비스 클래스
// 메인 페이지에서 최신 맛집 6개를 가져오기 위해 사용
import com.example.mymatzip.service.RestaurantService;

// @RequiredArgsConstructor: final 필드의 생성자를 자동 생성하는 Lombok 어노테이션
import lombok.RequiredArgsConstructor;

// @Controller: 이 클래스가 뷰(View) 컨트롤러임을 선언하는 어노테이션
// @RestController와 달리, 메서드의 반환값을 "뷰 이름"으로 해석합니다.
// 반환값 "home" → templates/home.html 파일을 찾아서 HTML로 렌더링
import org.springframework.stereotype.Controller;

// Model: Controller에서 View(타임리프 화면)로 데이터를 전달하는 "데이터 가방" 객체
// model.addAttribute("이름", 값)으로 데이터를 담으면,
// 타임리프 화면에서 ${이름}으로 꺼내서 사용할 수 있습니다.
import org.springframework.ui.Model;

// @GetMapping: HTTP GET 요청을 처리하는 메서드에 붙이는 어노테이션
import org.springframework.web.bind.annotation.GetMapping;

// ── @Controller 어노테이션 ──────────────────────────────────
// 이 클래스가 뷰(View) 컨트롤러임을 스프링에게 알려줍니다.
// 메서드의 반환값(String)이 타임리프 화면 파일의 이름으로 해석됩니다.
// 예: return "home" → templates/home.html
@Controller

// ── @RequiredArgsConstructor ────────────────────────────────
// final 필드(restaurantService)에 대한 생성자를 자동 생성 → 의존성 주입(DI)
@RequiredArgsConstructor
// ── 클래스 선언 ─────────────────────────────────────────────
public class HomeController {

    // ── 필드 (멤버 변수) ────────────────────────────────────
    // RestaurantService: 맛집 관련 비즈니스 로직 담당 서비스
    // 메인 페이지에서 최신 등록된 맛집 6개를 가져오기 위해 사용합니다.
    private final RestaurantService restaurantService;

    // ── GET / ───────────────────────────────────────────────
    // 메인(홈) 페이지: 최신 맛집 6개를 포함한 홈 화면을 보여줌
    // 요청 예시: GET http://localhost:8080/
    //   (브라우저 주소창에 http://localhost:8080 입력 시)
    // 응답: home.html 페이지를 HTML로 렌더링하여 반환
    //
    // @GetMapping("/"): GET 방식으로 "/" (루트 경로) 요청이 오면 실행
    //   "/" = 웹사이트의 첫 페이지 (메인 페이지)
    // 매개변수:
    //   - Model model: 타임리프 화면에 데이터를 전달하기 위한 객체
    //     스프링이 자동으로 빈 Model 객체를 만들어서 넣어줍니다.
    // 반환값: String "home"
    //   → 뷰 리졸버(ViewResolver)가 이 문자열을 타임리프 화면 파일 경로로 변환
    //   → templates/home.html 파일을 찾아서 HTML로 렌더링
    @GetMapping("/")
    public String home(Model model) {
        // ── 1단계: 최신 맛집 6개를 Model에 담기 ─────────────
        // restaurantService.findLatest6(): 가장 최근에 등록된 맛집 6개를 조회
        // model.addAttribute("restaurants", ...):
        //   "restaurants"라는 이름으로 맛집 목록을 Model에 담음
        //   타임리프 화면에서 ${restaurants}로 접근 가능
        model.addAttribute("restaurants", restaurantService.findLatest6());

        // ── 2단계: 타임리프 화면 이름 반환 ─────────────────────────
        // "home"을 반환하면:
        //   ThymeleafViewResolver 객체가 prefix(classpath:/templates/) + "home" + suffix(.html) 조합
        //   → templates/home.html 파일을 렌더링하여 브라우저에 전달
        return "home";
    }

    // ── GET /wishlists ──────────────────────────────────────
    // 찜 목록 페이지: 내가 찜한 맛집 목록을 보는 화면을 보여줌
    // 요청 예시: GET http://localhost:8080/wishlists
    // 응답: wishlist/list.html 페이지를 HTML로 렌더링하여 반환
    //
    // 이 페이지는 서버에서 데이터를 미리 담지 않고,
    // 페이지 로딩 후 JavaScript가 /api/wishlists API를 호출하여
    // 데이터를 가져와서 화면에 표시합니다. (클라이언트 사이드 렌더링)
    //
    // @GetMapping("/wishlists"): GET 방식으로 "/wishlists" 요청이 오면 실행
    // 매개변수: 없음 (데이터 전달 불필요 — JavaScript가 API로 가져옴)
    // 반환값: String "wishlist/list"
    //   → templates/wishlist/list.html 파일을 렌더링
    @GetMapping("/wishlists")
    public String wishlistPage() {
        // "wishlist/list"를 반환하면:
        //   뷰 리졸버가 templates/wishlist/list.html 파일을 찾아서 렌더링
        //   슬래시(/)는 하위 폴더를 의미
        return "wishlist/list";
    }
}
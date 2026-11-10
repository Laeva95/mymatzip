/*
 * ============================================================================
 *  파일명    : WebMvcConfig.java
 *  위치      : com.example.mymatzip.config (설정 패키지)
 *  역할      : 스프링 MVC의 동작 방식을 커스터마이징(맞춤 설정)하는 "설정 파일"
 * ============================================================================
 *
 *
 *  ■ MVC에서의 위치
 *    MVC 구조 자체를 설정하는 "인프라" 파일입니다.
 *    Controller, Service, Repository가 잘 동작하도록 환경을 세팅합니다.
 *
 *  ■ 이 파일이 하는 2가지 일
 *    1) ArgumentResolver 등록: @LoginMember 어노테이션을 처리하는 해석기 등록
 *    2) 정적 리소스 매핑: CSS, JS, 이미지, 업로드 파일의 위치를 브라우저에 알려줌
 *
 *  ■ 변경 이력
 *    - LoginInterceptor 제거: Spring Security의 필터 체인으로 대체되었습니다.
 *      (보안 검사는 SecurityConfig.java에서 설정합니다.)
 *    - LoginMemberArgumentResolver 추가: @LoginMember 어노테이션 지원
 *    - uploads 리소스 핸들러 추가: 파일 업로드 경로 매핑
 *
 *  ■ URL 매핑
 *    이 파일 자체는 URL을 처리하지 않습니다.
 *    다른 파일들(Controller, ArgumentResolver)이 URL을 처리할 수 있도록 설정만 합니다.
 * ============================================================================
 */

// ── 패키지 선언 ──────────────────────────────────────────────
// 이 파일이 config(설정) 패키지에 속함을 선언합니다.
package com.example.mymatzip.config;

// ── import 문 (외부 라이브러리 가져오기) ─────────────────────

// LoginMemberArgumentResolver: @LoginMember 어노테이션을 처리하는 해석기
import com.example.mymatzip.config.security.LoginMemberArgumentResolver;

// @RequiredArgsConstructor: final 필드에 대한 생성자 자동 생성 (Lombok)
import lombok.RequiredArgsConstructor;

// @Configuration: 이 클래스가 스프링의 "설정 파일"임을 표시하는 어노테이션
// 스프링이 시작할 때 이 어노테이션이 붙은 클래스를 찾아서 설정 내용을 적용합니다.
import org.springframework.context.annotation.Configuration;

// ResourceHandlerRegistry: 정적 리소스(CSS, JS, 이미지)의 위치를 등록하는 "등록부"
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

// WebMvcConfigurer: 스프링 MVC 설정을 커스터마이징하기 위한 인터페이스
// 이 인터페이스를 구현(implements)하면 스프링 MVC의 기본 동작을 변경할 수 있습니다.
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// HandlerMethodArgumentResolver: Controller 메서드의 매개변수를 해석하는 인터페이스
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

// List: 여러 개의 데이터를 순서대로 저장하는 자바 자료구조
import java.util.List;

// ── @Configuration 어노테이션 ───────────────────────────────
// 이 클래스가 스프링의 설정 파일임을 선언합니다.
// 스프링 부트가 시작될 때 이 클래스를 자동으로 읽어서 설정을 적용합니다.
@Configuration

// ── @RequiredArgsConstructor ─────────────────────────────────
// final 필드에 대한 생성자를 자동 생성 → 의존성 주입(DI)
@RequiredArgsConstructor

// ── 클래스 선언 ─────────────────────────────────────────────
// public: 다른 패키지에서도 접근 가능
// class WebMvcConfig: "웹 MVC 설정"이라는 이름의 클래스
// implements WebMvcConfigurer: WebMvcConfigurer 인터페이스를 구현
//   → addArgumentResolvers(), addResourceHandlers() 등의 메서드를 오버라이드 가능
public class WebMvcConfig implements WebMvcConfigurer {

    // ── 필드 (멤버 변수) ────────────────────────────────────
    // LoginMemberArgumentResolver: @LoginMember 어노테이션이 붙은 매개변수에
    // 현재 로그인한 회원(Member) 객체를 자동으로 주입해주는 해석기
    private final LoginMemberArgumentResolver loginMemberArgumentResolver;

    // ── addArgumentResolvers 메서드 ──────────────────────────
    // ■ 역할: Controller 메서드의 매개변수를 해석하는 "해석기"를 등록합니다.
    // ■ 실행 시점: 스프링이 시작될 때 1번만 실행 (설정 단계)
    // ■ @Override: 부모(WebMvcConfigurer)의 메서드를 재정의
    // ■ 매개변수: resolvers → 해석기들을 등록할 수 있는 "목록" 객체
    // ■ 반환값: void (없음) — 목록에 직접 등록하므로 반환할 필요 없음
    //
    // ■ 등록 후 효과:
    //   Controller 메서드에서 @LoginMember Member loginMember 매개변수를 사용하면,
    //   LoginMemberArgumentResolver가 자동으로 현재 로그인한 회원 정보를 주입해줍니다.
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // loginMemberArgumentResolver를 해석기 목록에 추가
        resolvers.add(loginMemberArgumentResolver);
    }

    // ── addResourceHandlers 메서드 ──────────────────────────
    // ■ 역할: 정적 리소스(CSS, JS, 이미지, 업로드 파일)의 URL과 실제 파일 위치를 매핑하는 메서드
    // ■ 실행 시점: 스프링이 시작될 때 1번만 실행 (설정 단계)
    // ■ @Override: 부모(WebMvcConfigurer)의 메서드를 재정의
    // ■ 매개변수: registry → 리소스 핸들러를 등록할 수 있는 "등록부" 객체
    // ■ 반환값: void (없음)
    //
    // ■ 왜 필요한가?
    //   브라우저가 /css/style.css 파일을 요청하면,
    //   스프링은 이 파일이 어디에 있는지 알아야 합니다.
    //   이 설정이 없으면 CSS, JS, 이미지 파일을 찾지 못합니다.
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // ── CSS 파일 매핑 ───────────────────────────────────
        // addResourceHandler("/css/**"):
        //   "/css/"로 시작하는 모든 URL 요청을 처리하겠다는 의미
        //   예: /css/style.css, /css/common/reset.css 등
        // addResourceLocations("classpath:/static/css/"):
        //   실제 파일이 있는 폴더 경로
        //   classpath: = src/main/resources/ 폴더를 가리킴
        //   즉, src/main/resources/static/css/ 폴더에서 파일을 찾음
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");

        // ── JS 파일 매핑 ────────────────────────────────────
        // "/js/"로 시작하는 요청 → src/main/resources/static/js/ 에서 찾기
        // 예: /js/app.js → src/main/resources/static/js/app.js
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");

        // ── 이미지 파일 매핑 ────────────────────────────────
        // "/images/"로 시작하는 요청 → src/main/resources/static/images/ 에서 찾기
        // 예: /images/logo.png → src/main/resources/static/images/logo.png
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");

        // ── 업로드 파일 매핑 ────────────────────────────────
        // "/uploads/"로 시작하는 요청 → 프로젝트 루트의 uploads/ 폴더에서 찾기
        // "file:uploads/": 파일 시스템의 상대 경로 (프로젝트 실행 위치 기준)
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
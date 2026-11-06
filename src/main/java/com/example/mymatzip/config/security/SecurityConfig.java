package com.example.mymatzip.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/*
 *  [임시] 개발용 시큐리티 설정 - 모든 요청을 로그인 없이 허용
 *  로그인 담당자가 작업을 시작하면 이 파일 내용을 실제 설정으로 바꿔 주세요.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 모든 주소를 로그인 없이 접근 가능하게 함
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                // POST/PUT/DELETE 요청이 403 으로 막히지 않도록 CSRF 검사 끔 (개발 중에만)
                .csrf(csrf -> csrf.disable());
        // formLogin() 을 설정하지 않았으므로 기본 로그인 화면도 나오지 않음
        return http.build();
    }
}
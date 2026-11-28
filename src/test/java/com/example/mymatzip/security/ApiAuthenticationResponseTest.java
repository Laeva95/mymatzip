package com.example.mymatzip.security;

import com.example.mymatzip.config.security.CustomUserDetails;
import com.example.mymatzip.entity.Member;
import com.example.mymatzip.entity.Role;
import com.example.mymatzip.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// SecurityConfig 의 인증 · 권한 실패 응답
//   /api/** : 로그인 안 함 → 401 JSON, 권한 없음 → 403 JSON
//   화면    : 로그인 안 함 → 로그인 페이지로 이동 (기존 동작 유지)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ApiAuthenticationResponseTest {

    @Autowired private WebApplicationContext context;
    @Autowired private MemberRepository memberRepository;

    private MockMvc mockMvc;
    private Member user;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        var constructor = Member.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        user = constructor.newInstance();
        ReflectionTestUtils.setField(user, "email", "auth-response@test.com");
        ReflectionTestUtils.setField(user, "password", "encodedPassword123!");
        ReflectionTestUtils.setField(user, "nickname", "인증응답");
        ReflectionTestUtils.setField(user, "role", Role.USER);
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.now());
        user = memberRepository.save(user);
    }

    @Test
    @DisplayName("토큰 없이 보호된 API 를 부르면 401 JSON")
    void 토큰_없이_API() throws Exception {
        mockMvc.perform(get("/api/mypage/stats"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.error").value("로그인이 필요합니다."));
    }

    @Test
    @DisplayName("잘못된 JWT 로 API 를 부르면 401 JSON")
    void 잘못된_토큰으로_API() throws Exception {
        mockMvc.perform(post("/api/wishlists/1").header("Authorization", "Bearer not-a-valid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("로그인이 필요합니다."));
    }

    @Test
    @DisplayName("일반 회원이 관리자 API 를 부르면 403 JSON")
    void 일반회원_관리자_API() throws Exception {
        mockMvc.perform(get("/api/admin/stats").with(loginAs(user)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("접근 권한이 없습니다."));
    }

    @Test
    @DisplayName("일반 회원이 맛집을 등록하면 403 과 서비스가 정한 안내 문구")
    void 일반회원_맛집_등록() throws Exception {
        String body = "{\"name\":\"권한테스트식당\",\"category\":\"KOREAN\",\"address\":\"부산광역시 해운대구 해운대해변로 264\",\"lat\":35.1587,\"lng\":129.1604}";
        mockMvc.perform(post("/api/restaurants").with(loginAs(user))
                        .contentType("application/json").content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("맛집 등록 권한이 없습니다."));
    }

    @Test
    @DisplayName("로그인 없이 보호된 화면을 열면 기존처럼 로그인 페이지로 이동")
    void 로그인_없이_화면() throws Exception {
        mockMvc.perform(get("/restaurants/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/members/login"));
    }

    @Test
    @DisplayName("로그인 없이 열 수 있는 API 는 그대로 200")
    void 공개_API() throws Exception {
        mockMvc.perform(get("/api/restaurants"))
                .andExpect(status().isOk());
    }

    private RequestPostProcessor loginAs(Member m) {
        CustomUserDetails userDetails = new CustomUserDetails(m);
        return authentication(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }
}

package com.example.mymatzip.wishlist;

import com.example.mymatzip.config.security.CustomUserDetails;
import com.example.mymatzip.entity.Category;
import com.example.mymatzip.entity.Member;
import com.example.mymatzip.entity.Restaurant;
import com.example.mymatzip.entity.Role;
import com.example.mymatzip.entity.Source;
import com.example.mymatzip.repository.MemberRepository;
import com.example.mymatzip.repository.RestaurantRepository;
import com.example.mymatzip.repository.WishlistRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// 검증일지 24번: 맛집 목록·검색 결과 썸네일 하트(찜) 버튼
//   - 목록 화면에 하트 버튼과 wishlist-heart.js 가 들어가는지
//   - 하트가 사용하는 기존 API(POST/DELETE/GET /api/wishlists)의 응답이 JS 가 기대하는 대로인지
//   - 상세 화면 찜 버튼이 그대로 동작하는지
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WishlistHeartTest {

    @Autowired private WebApplicationContext context;
    @Autowired private MemberRepository memberRepository;
    @Autowired private RestaurantRepository restaurantRepository;
    @Autowired private WishlistRepository wishlistRepository;

    private MockMvc mockMvc;
    private Member member;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        LocalDateTime now = LocalDateTime.now();

        member = newInstance(Member.class);
        ReflectionTestUtils.setField(member, "email", "heart@test.com");
        ReflectionTestUtils.setField(member, "password", "encodedPassword123!");
        ReflectionTestUtils.setField(member, "nickname", "하트");
        ReflectionTestUtils.setField(member, "role", Role.USER);
        ReflectionTestUtils.setField(member, "createdAt", now);
        member = memberRepository.save(member);

        restaurant = newInstance(Restaurant.class);
        ReflectionTestUtils.setField(restaurant, "member", member);
        ReflectionTestUtils.setField(restaurant, "name", "하트식당");
        ReflectionTestUtils.setField(restaurant, "address", "부산시 해운대구");
        ReflectionTestUtils.setField(restaurant, "category", Category.KOREAN);
        ReflectionTestUtils.setField(restaurant, "source", Source.MANUAL);
        ReflectionTestUtils.setField(restaurant, "createdAt", now);
        restaurant = restaurantRepository.save(restaurant);
    }

    @Test
    @DisplayName("목록 화면 카드에 하트 버튼과 스크립트가 렌더링된다")
    void 목록_카드에_하트_버튼() throws Exception {
        mockMvc.perform(get("/restaurants").with(loginAs(member)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("wish-heart")))
                .andExpect(content().string(containsString("data-restaurant-id=\"" + restaurant.getId() + "\"")))
                .andExpect(content().string(containsString("/js/wishlist-heart.js")))
                // 하트를 흰 원 가운데에 맞추는 스타일 (일반 카드 · 검색 결과 카드 공통 .wish-heart)
                .andExpect(content().string(containsString("/css/wishlist-heart.css")))
                .andExpect(content().string(containsString("WishHeart.init({ loggedIn: true")));
    }

    @Test
    @DisplayName("하트 가운데 정렬 스타일 파일이 제공된다")
    void 하트_스타일_파일() throws Exception {
        mockMvc.perform(get("/css/wishlist-heart.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(".wish-heart.btn")))
                .andExpect(content().string(containsString("justify-content: center")));
    }

    @Test
    @DisplayName("찜 등록 → 목록 조회 반영 → 중복 등록 409 → 취소 → 재취소 404")
    void 찜_등록_취소_흐름() throws Exception {
        Long id = restaurant.getId();

        mockMvc.perform(post("/api/wishlists/{id}", id).with(loginAs(member)))
                .andExpect(status().isCreated());
        assertThat(wishlistRepository.existsByMemberIdAndRestaurantId(member.getId(), id)).isTrue();

        // 새로고침 시 WishHeart.init() 이 읽는 목록에 들어 있어야 한다
        mockMvc.perform(get("/api/wishlists").with(loginAs(member)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].restaurant.id").value(id));

        // 이미 찜한 상태에서 POST → 409 (JS 는 화면을 '찜함'으로 맞춤)
        mockMvc.perform(post("/api/wishlists/{id}", id).with(loginAs(member)))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/wishlists/{id}", id).with(loginAs(member)))
                .andExpect(status().isOk());
        assertThat(wishlistRepository.existsByMemberIdAndRestaurantId(member.getId(), id)).isFalse();

        mockMvc.perform(get("/api/wishlists").with(loginAs(member)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        // 이미 취소된 상태에서 DELETE → 404 (JS 는 화면을 '찜 안 함'으로 맞춤)
        mockMvc.perform(delete("/api/wishlists/{id}", id).with(loginAs(member)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("비로그인 찜 요청은 처리되지 않고 로그인 화면으로 보내진다")
    void 비로그인_찜_요청() throws Exception {
        mockMvc.perform(post("/api/wishlists/{id}", restaurant.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/members/login"));
        assertThat(wishlistRepository.count()).isZero();
    }

    @Test
    @DisplayName("상세 화면 찜 버튼은 기존처럼 찜 상태를 표시한다")
    void 상세_화면_찜_버튼_유지() throws Exception {
        mockMvc.perform(get("/restaurants/{id}", restaurant.getId()).with(loginAs(member)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"wishBtn\"")))
                .andExpect(content().string(containsString("data-wished=\"false\"")))
                .andExpect(content().string(not(containsString("wishlist-heart.js"))));

        mockMvc.perform(post("/api/wishlists/{id}", restaurant.getId()).with(loginAs(member)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/restaurants/{id}", restaurant.getId()).with(loginAs(member)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-wished=\"true\"")));
    }

    private <T> T newInstance(Class<T> clazz) throws Exception {
        var constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private RequestPostProcessor loginAs(Member m) {
        CustomUserDetails userDetails = new CustomUserDetails(m);
        return authentication(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }
}

package com.example.mymatzip.review;

import com.example.mymatzip.config.security.CustomUserDetails;
import com.example.mymatzip.entity.Category;
import com.example.mymatzip.entity.Member;
import com.example.mymatzip.entity.Restaurant;
import com.example.mymatzip.entity.Review;
import com.example.mymatzip.entity.Role;
import com.example.mymatzip.repository.MemberRepository;
import com.example.mymatzip.repository.RestaurantRepository;
import com.example.mymatzip.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import com.example.mymatzip.entity.Source;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class IssueVerificationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    private Member authorMember;
    private Member otherMember;
    private Restaurant restaurant;
    private Review savedReview;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        LocalDateTime now = LocalDateTime.now();

        authorMember = createEntity(Member.class);
        ReflectionTestUtils.setField(authorMember, "email", "author@test.com");
        ReflectionTestUtils.setField(authorMember, "password", "encodedPassword123!");
        ReflectionTestUtils.setField(authorMember, "nickname", "작성자");
        ReflectionTestUtils.setField(authorMember, "role", Role.USER);
        ReflectionTestUtils.setField(authorMember, "createdAt", now);
        authorMember = memberRepository.save(authorMember);

        otherMember = createEntity(Member.class);
        ReflectionTestUtils.setField(otherMember, "email", "other@test.com");
        ReflectionTestUtils.setField(otherMember, "password", "encodedPassword123!");
        ReflectionTestUtils.setField(otherMember, "nickname", "신고자");
        ReflectionTestUtils.setField(otherMember, "role", Role.USER);
        ReflectionTestUtils.setField(otherMember, "createdAt", now);
        otherMember = memberRepository.save(otherMember);

        restaurant = createEntity(Restaurant.class);
        ReflectionTestUtils.setField(restaurant, "member", authorMember);
        ReflectionTestUtils.setField(restaurant, "name", "테스트식당");
        ReflectionTestUtils.setField(restaurant, "address", "부산시 부산진구");
        ReflectionTestUtils.setField(restaurant, "category", Category.KOREAN);
        restaurant = createEntity(Restaurant.class);
        ReflectionTestUtils.setField(restaurant, "member", authorMember);
        ReflectionTestUtils.setField(restaurant, "name", "테스트식당");
        ReflectionTestUtils.setField(restaurant, "address", "부산시 부산진구");
        ReflectionTestUtils.setField(restaurant, "category", Category.KOREAN);
        ReflectionTestUtils.setField(restaurant, "source", Source.MANUAL);   // ← 이 줄 추가
        ReflectionTestUtils.setField(restaurant, "createdAt", now);
        restaurant = restaurantRepository.save(restaurant);
        ReflectionTestUtils.setField(restaurant, "createdAt", now);
        restaurant = restaurantRepository.save(restaurant);

        savedReview = createEntity(Review.class);
        ReflectionTestUtils.setField(savedReview, "member", authorMember);
        ReflectionTestUtils.setField(savedReview, "restaurant", restaurant);
        ReflectionTestUtils.setField(savedReview, "rating", 5);
        ReflectionTestUtils.setField(savedReview, "content", "테스트 리뷰 내용입니다.");
        ReflectionTestUtils.setField(savedReview, "reported", false);
        ReflectionTestUtils.setField(savedReview, "createdAt", now);
        savedReview = reviewRepository.save(savedReview);
    }

    private <T> T createEntity(Class<T> clazz) throws Exception {
        var constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private RequestPostProcessor loginAs(Member member) {
        CustomUserDetails userDetails = new CustomUserDetails(member);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        return authentication(auth);
    }

    // =========================================================================
    // [4번 이슈] 리뷰·답글 삭제 - 없는 ID 삭제 시 403 대신 404 반환 검증
    // =========================================================================
    @Test
    @DisplayName("[이슈 4-1] 존재하지 않는 리뷰 ID 삭제 시 404 Not Found 반환 검증")
    void verifyIssue4_DeleteNonExistentReview() throws Exception {
        long nonExistentId = 999999L;

        mockMvc.perform(delete("/api/reviews/{id}", nonExistentId)
                        .with(loginAs(authorMember)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("[이슈 4-2] 존재하지 않는 답글 ID 삭제 시 404 Not Found 반환 검증")
    void verifyIssue4_DeleteNonExistentReply() throws Exception {
        long nonExistentId = 999999L;

        mockMvc.perform(delete("/api/replies/{id}", nonExistentId)
                        .with(loginAs(authorMember)))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // [5번 이슈] 리뷰 신고 - 본인 리뷰 신고 차단 및 중복 신고 방지 검증
    // =========================================================================
    @Test
    @DisplayName("[이슈 5-1] 본인이 작성한 리뷰 신고 차단 검증")
    void verifyIssue5_BlockSelfReport() throws Exception {
        Long reviewId = (Long) ReflectionTestUtils.getField(savedReview, "id");

        mockMvc.perform(post("/api/reviews/{id}/report", reviewId)
                        .with(loginAs(authorMember)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("[이슈 5-2] 동일 회원의 같은 리뷰 중복 신고 차단 검증")
    void verifyIssue5_BlockDuplicateReport() throws Exception {
        Long reviewId = (Long) ReflectionTestUtils.getField(savedReview, "id");

        mockMvc.perform(post("/api/reviews/{id}/report", reviewId)
                        .with(loginAs(otherMember)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/reviews/{id}/report", reviewId)
                        .with(loginAs(otherMember)))
                .andExpect(status().is4xxClientError());
    }

    // =========================================================================
    // [6번 이슈] 리뷰 작성 - imageUrl 길이(255자 초과) 및 URL 형식 검증
    // =========================================================================
    @Test
    @DisplayName("[이슈 6-1] imageUrl 256자 이상 입력 시 400 Bad Request 검증")
    void verifyIssue6_ValidateImageUrlLength() throws Exception {
        Long restaurantId = (Long) ReflectionTestUtils.getField(restaurant, "id");
        String overLengthUrl = "https://example.com" + "a".repeat(240) + ".png";
        assertThat(overLengthUrl.length()).isGreaterThan(255);

        String requestJson = """
                {
                    "restaurantId": %d,
                    "rating": 5,
                    "content": "이미지 URL 길이 초과 테스트",
                    "imageUrl": "%s"
                }
                """.formatted(restaurantId, overLengthUrl);

        mockMvc.perform(post("/api/reviews")
                        .with(loginAs(authorMember))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("[이슈 6-2] imageUrl 잘못된 형식 입력 시 400 Bad Request 검증")
    void verifyIssue6_ValidateImageUrlFormat() throws Exception {
        Long restaurantId = (Long) ReflectionTestUtils.getField(restaurant, "id");

        String requestJson = """
                {
                    "restaurantId": %d,
                    "rating": 5,
                    "content": "잘못된 URL 형식 테스트",
                    "imageUrl": "invalid-url-format"
                }
                """.formatted(restaurantId);

        mockMvc.perform(post("/api/reviews")
                        .with(loginAs(authorMember))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // [7번 이슈] 리뷰 목록 API - 페이징 적용 여부 검증
    // =========================================================================
    @Test
    @DisplayName("[이슈 7] 리뷰 목록 조회 시 페이징 파라미터(page, size) 적용 검증")
    void verifyIssue7_ReviewListPagination() throws Exception {
        Long restaurantId = (Long) ReflectionTestUtils.getField(restaurant, "id");
        LocalDateTime now = LocalDateTime.now();

        for (int i = 1; i <= 10; i++) {
            Review extraReview = createEntity(Review.class);
            ReflectionTestUtils.setField(extraReview, "member", authorMember);
        }
    }
}
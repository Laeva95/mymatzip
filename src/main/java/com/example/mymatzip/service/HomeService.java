package com.example.mymatzip.service;

import com.example.mymatzip.dto.RestaurantResponse;
import com.example.mymatzip.entity.Restaurant;
import com.example.mymatzip.repository.RestaurantRepository;
import com.example.mymatzip.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// ============================================================
// [HomeService.java] 메인 화면(home.html) 전용 조회 (팀장 담당)
// ============================================================
// 사용처: HomeController.home() (GET /)
//
// 최근 등록된 맛집 6개 + 카드에 보여줄 별점 · 리뷰 수 · 찜 수
//   맛집 목록(list.html) 카드와 같은 값을 보여주려고, RestaurantService.listWithReviewCountAndWishlist()
//   (권성애 담당, private)와 같은 방식으로 만든다. 그 메서드를 바꾸면 이 클래스도 같이 확인할 것.
//   쓰는 것은 모두 이미 public 으로 열려 있는 것만 (다른 팀원 파일은 수정하지 않음)
//     RestaurantRepository.findTop6ByDeletedAtIsNullOrderByCreatedAtDesc()       : 맛집 6개 (+ 작성자 @EntityGraph)
//     ReviewRepository.averageRatingsAndReviewCountsByRestaurantIdsAvoidNPlusOne() : 별점 · 리뷰 수
//     RestaurantRepository.wishlistCountsByRestaurantIdsAvoidNPlusOne()            : 찜 수
//     RestaurantResponse.withReviewCountAndWishlist()                              : 응답 DTO
//
// 쿼리 수: 맛집 1 + 별점 · 리뷰 수 1 + 찜 수 1 = 3번 (맛집 수와 상관없이 고정 → N+1 아님)
//   (기존 RestaurantService.findLatest6AvoidNPlusOne() 은 찜 수가 없어서 2번. 그 메서드와 테스트는 그대로 둠)
// ============================================================
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private final RestaurantRepository restaurantRepository;
    private final ReviewRepository reviewRepository;

    public List<RestaurantResponse> findLatest6WithReviewCountAndWishlist() {
        List<Restaurant> restaurants = restaurantRepository.findTop6ByDeletedAtIsNullOrderByCreatedAtDesc();
        if (restaurants.isEmpty()) {
            return List.of();                                       // IN () 빈 목록 방지
        }
        List<Long> ids = restaurants.stream().map(Restaurant::getId).toList();

        // 별점 · 리뷰 수 (리뷰 없는 맛집은 결과에 없음)
        Map<Long, ReviewRepository.RestaurantRatingReviewCount> ratingMap =
                reviewRepository.averageRatingsAndReviewCountsByRestaurantIdsAvoidNPlusOne(ids).stream()
                        .collect(Collectors.toMap(ReviewRepository.RestaurantRatingReviewCount::getRestaurantId, x -> x));

        // 찜 수 (찜 없는 맛집은 결과에 없음)
        Map<Long, Long> wishMap = restaurantRepository.wishlistCountsByRestaurantIdsAvoidNPlusOne(ids).stream()
                .collect(Collectors.toMap(RestaurantRepository.RestaurantWishlistCount::getRestaurantId,
                        RestaurantRepository.RestaurantWishlistCount::getWishlistCount));

        // 맛집 순서(최신순)는 그대로 두고 Map 에서 꺼내 채움 (추가 쿼리 없음)
        return restaurants.stream().map(r -> {
            ReviewRepository.RestaurantRatingReviewCount rr = ratingMap.get(r.getId());
            Double avg = (rr != null) ? rr.getAverageRating() : null;  // 리뷰 없음 → null → from() 이 0.0 으로 바꿈
            Long reviewCount = (rr != null) ? rr.getReviewCount() : 0L;
            Long wishlistCount = wishMap.getOrDefault(r.getId(), 0L);
            return RestaurantResponse.withReviewCountAndWishlist(r, avg, reviewCount, wishlistCount);
        }).toList();
    }
}

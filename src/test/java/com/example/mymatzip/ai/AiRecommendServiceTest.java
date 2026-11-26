
package com.example.mymatzip.ai;

import com.example.mymatzip.dto.RestaurantResponse;
import com.example.mymatzip.service.HomeService;
import com.example.mymatzip.service.RestaurantService;
import com.example.mymatzip.service.ai.AiRecommendService;
import com.example.mymatzip.service.ai.AiUsageLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AiRecommendServiceTest {

    @Test
    void 추천_결과가_0건이면_사용횟수를_복구한다() {
        // 1. AI 응답을 빈 문자열로 설정
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec =
                mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec =
                mock(ChatClient.CallResponseSpec.class);

        // 2. AI 호출 흐름을 모의 처리
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("");

        // 3. 맛집 후보 준비
        RestaurantService restaurantService = mock(RestaurantService.class);
        when(restaurantService.findAllAvoidNPlusOne()).thenReturn(List.of(
                mock(RestaurantResponse.class)
        ));

        // 4. 실제 사용 횟수 관리 객체 사용
        AiUsageLimiter limiter = new AiUsageLimiter(2);

        // 5. ChatClient.Builder를 모의 객체로 생성
                ChatClient.Builder builder = mock(ChatClient.Builder.class);

        // build()가 호출되면 위에서 만든 chatClient를 반환하도록 설정
                when(builder.build()).thenReturn(chatClient);

        // 6. 서비스 생성 (HomeService 는 mock — 결과가 0건이라 호출되지 않음)
                AiRecommendService service = new AiRecommendService(
                        builder,
                        restaurantService,
                        limiter,
                        mock(HomeService.class)
                );

        // 7. 추천 실행
        var result = service.recommend("조용한 맛집", 1L);

        // 8. 결과가 0건이고 사용 횟수가 복구됐는지 확인 (AI 가 빈 답을 주면 refund)
        assertThat(result.get("picks")).asList().isEmpty();
        assertThat(result.get("remaining")).isEqualTo(2);
    }
}

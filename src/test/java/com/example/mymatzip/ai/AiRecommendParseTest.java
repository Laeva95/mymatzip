package com.example.mymatzip.ai;

import com.example.mymatzip.service.ai.AiRecommendService;
import com.example.mymatzip.service.ai.AiRecommendService.Pick;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// AI 답에서 DB 에 있는 맛집 번호만 골라내는지 검사 (AI 를 실제로 부르지 않음)
class AiRecommendParseTest {

    @Test
    void 목록에_없는_번호와_중복은_버리고_최대_3개() {

        String answer = """
                추천 결과입니다.
                2|국물이 진해서 비 오는 날 잘 어울립니다
                99|이런 맛집은 없습니다
                2|중복
                5 | 조용한 분위기
                - 7: 별점이 높습니다
                8|네 번째는 버려집니다
                """;

        List<Pick> picks = AiRecommendService.parsePicks(answer, Set.of(2L, 5L, 7L, 8L));

        assertThat(picks).extracting(Pick::id).containsExactly(2L, 5L, 7L);
        assertThat(picks.get(0).reason()).contains("국물");
    }

    @Test
    void 형식이_맞지_않으면_빈_목록() {

        assertThat(AiRecommendService.parsePicks("잘 모르겠어요", Set.of(1L))).isEmpty();
        assertThat(AiRecommendService.parsePicks(null, Set.of(1L))).isEmpty();
    }
}

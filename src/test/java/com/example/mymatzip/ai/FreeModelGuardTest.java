package com.example.mymatzip.ai;

import com.example.mymatzip.config.ai.FreeModelGuard;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 유료 모델 차단 규칙 검사. GitHub Actions CI 에서 ./gradlew build 할 때 자동 실행된다.
class FreeModelGuardTest {

    @Test
    void 무료_모델은_통과한다() {

        assertThatCode(() -> FreeModelGuard.check("meta-llama/llama-3.3-70b-instruct:free"))
                .doesNotThrowAnyException();
    }

    @Test
    void 유료_모델은_서버_시작을_막는다() {

        assertThatThrownBy(() -> FreeModelGuard.check("openai/gpt-4o"))
                .isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> FreeModelGuard.check(null))
                .isInstanceOf(IllegalStateException.class);
    }
}
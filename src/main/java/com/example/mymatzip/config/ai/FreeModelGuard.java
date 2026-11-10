package com.example.mymatzip.config.ai;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

// ============================================================
// [FreeModelGuard] 유료 AI 모델 사용을 막는 검사 클래스 (C 리뷰 담당, 팀 공용)
// ============================================================
// 규칙: OpenRouter 모델 이름이 ":free" 로 끝나지 않으면 서버가 켜지지 않는다.
//       (OpenRouter 에서 이름 끝이 :free 인 모델만 요금이 0원이다)
// ============================================================

// @Configuration: 서버가 켜질 때 스프링 컨테이너 안의 DefaultListableBeanFactory 객체가
//   이 클래스의 객체를 하나 만들어 보관한다. 그래서 아래 검사가 서버 시작 시 반드시 한 번 실행된다.
@Configuration
public class FreeModelGuard {

    // @Value: application.properties 의 spring.ai.openai.chat.options.model 값을 이 필드에 넣는다.
    //   넣는 일은 AutowiredAnnotationBeanPostProcessor 객체가 이 객체를 만든 직후에 한다.
    //   환경 변수 OPENROUTER_MODEL 이 있으면 그 값, 없으면 properties 에 적힌 기본값이 들어온다.
    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    // @PostConstruct: @Value 값이 들어간 다음 CommonAnnotationBeanPostProcessor 객체가 이 메서드를 한 번 호출한다.
    //   여기서 예외를 던지면 MyMatzipApplication 의 SpringApplication.run() 이 실패하고 서버가 켜지지 않는다.
    @PostConstruct
    void checkFreeModel() {
        check(model);
    }

    // 테스트(FreeModelGuardTest)에서도 호출할 수 있도록 static 으로 분리한 검사 로직
    public static void check(String modelName) {
        if (modelName == null || !modelName.trim().endsWith(":free")) {
            throw new IllegalStateException(
                    "무료 모델(:free)만 사용할 수 있습니다. OPENROUTER_MODEL 값을 확인하세요. 현재 모델: " + modelName);
        }
    }
}
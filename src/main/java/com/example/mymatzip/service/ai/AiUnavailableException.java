package com.example.mymatzip.service.ai;

// AI 호출이 실패했을 때 던지는 예외. AiApiController 가 잡아서 503 응답으로 바꾼다.
public class AiUnavailableException extends RuntimeException {
    public AiUnavailableException(String message) {
        super(message);
    }
}
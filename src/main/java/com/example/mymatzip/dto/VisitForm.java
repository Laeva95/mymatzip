package com.shin.springbootsecuritylogin.blog.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

// 방문 등록 폼 (th:object 로 바인딩하려고 일반 클래스 + getter/setter 사용)
@Getter
@Setter
public class VisitForm {

    @NotBlank(message = "식당 이름을 입력해 주세요.")
    @Size(max = 50, message = "식당 이름은 50자 이하입니다.")
    private String restaurantName;

    @Min(value = 0, message = "금액은 0원 이상입니다.")
    @Max(value = 10000000, message = "금액이 너무 큽니다.")
    private int amount;

    @Min(value = 0, message = "포인트는 0 이상입니다.")
    private int usePoint;

    @Size(max = 200, message = "메모는 200자 이하입니다.")
    private String memo;
}

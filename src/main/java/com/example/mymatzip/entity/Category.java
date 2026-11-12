package com.example.mymatzip.entity; // 이 파일이 속한 패키지(폴더) 선언
// com.example.mymatzip.entity = entity 패키지 안에 있다는 뜻
// 같은 패키지 안의 파일들(Role, Member, Restaurant 등)은 import 없이 서로 사용 가능

// ============================================================
// [Category.java] 맛집 카테고리 열거형 (Enum)
// ============================================================
//
// 역할: 맛집을 음식 종류별로 분류하는 enum(열거형)입니다.
//       한식, 중식, 일식, 양식, 카페, 기타 -- 총 6가지 카테고리가 있습니다.
//       [추가]2026.11.11권성애 해산물·회, 고기·구이, 국밥·탕 3가지 추가 → 총 9가지
//         (부산 맛집 특성 + 공공 API 모범음식점 업태 "회집", "식육(숯불구이)", "탕류(보신용)" 매핑용)
//
//
// 왜 enum(열거형)을 사용하나요?
//   카테고리는 정해진 6가지만 존재합니다.
//   만약 String(문자열)으로 저장하면 "한식", "KOREAN", "korean", "Korean" 등
//   사람마다 다르게 입력하여 데이터가 엉망이 될 수 있습니다.
//   enum은 미리 정해진 값만 허용하므로 이런 문제를 원천 차단합니다.
//
// DB 저장 방식:
//   Restaurant 엔티티에서 @Enumerated(EnumType.STRING)을 사용하기 때문에
//   DB의 restaurant 테이블에는 "KOREAN", "CHINESE" 등 영어 대문자 문자열로 저장됩니다.
//   화면에 표시할 때는 displayName 필드의 한글("한식", "중식" 등)을 사용합니다.
//
//   [추가]2026.11.11권성애 주의: MySQL에서 이 컬럼은 enum('CAFE','CHINESE',...) 타입으로 만들어집니다.
//     ddl-auto=update 상태에서 상수를 추가하면 DB 컬럼 정의는 바뀌지 않으므로
//     ALTER TABLE restaurant MODIFY category ENUM(...) 을 직접 실행해야 합니다.
//     (ddl-auto=create로 테이블을 새로 만들 때는 자동 반영)
//
// 이 enum이 사용되는 곳들:
//   1. Restaurant 엔티티의 category 필드 -- 맛집이 어떤 카테고리인지 저장
//   2. RestaurantService -- 맛집 등록/수정 시 카테고리 설정
//   3. RestaurantRepository -- 카테고리별 맛집 목록 조회 (findByCategory)
//   4. RestaurantApiController -- 카테고리별 필터링 API
//   5. RestaurantViewController -- 카테고리 탭 화면 렌더링
//   6. RestaurantForm (DTO) -- 맛집 등록/수정 폼에서 카테고리 선택값 전달
//   7. 타임리프 화면 -- 맛집 등록 폼의 드롭다운(선택 목록), 목록 페이지의 카테고리 탭
//   8. [추가]2026.11.11권성애 PublicCategoryMapper -- 공공 API 데이터를 카테고리로 분류
//   9. [추가]2026.11.11권성애 기본 이미지 -- static/images/category/{상수 이름 소문자}.svg
// ============================================================

// public: 다른 파일(클래스)에서도 이 enum을 사용할 수 있도록 공개합니다.
// enum: "열거형"이라는 특별한 자바 타입. 미리 정해진 상수 값들만 가질 수 있습니다.
// Category: 이 enum의 이름. "분류/카테고리"라는 뜻입니다.
public enum Category {

    // ──────────────────────────────────────────────────────────
    // enum 상수 정의 영역
    // 여기에 나열된 6개의 값만 Category 타입으로 사용할 수 있습니다.
    // 각 상수 뒤의 괄호 안 문자열은 생성자로 전달되어 displayName에 저장됩니다.
    // [추가]2026.11.11권성애 상수 순서 = 등록 폼 드롭다운 · 카테고리 탭에 보이는 순서 (기타는 맨 뒤 유지)
    // ──────────────────────────────────────────────────────────

    KOREAN("한식"),     // 첫 번째 카테고리: 한식 (김치찌개, 비빔밥, 불고기 등)
    // DB에 "KOREAN" 문자열로 저장, 화면에는 "한식"으로 표시

    CHINESE("중식"),    // 두 번째 카테고리: 중식 (짜장면, 짬뽕, 탕수육 등)
    // DB에 "CHINESE" 문자열로 저장, 화면에는 "중식"으로 표시

    JAPANESE("일식"),   // 세 번째 카테고리: 일식 (초밥, 라멘, 돈카츠 등)
    // DB에 "JAPANESE" 문자열로 저장, 화면에는 "일식"으로 표시

    WESTERN("양식"),    // 네 번째 카테고리: 양식 (파스타, 스테이크, 피자 등)
    // DB에 "WESTERN" 문자열로 저장, 화면에는 "양식"으로 표시

    CAFE("카페"),       // 다섯 번째 카테고리: 카페 (커피, 디저트, 브런치 등)
    // DB에 "CAFE" 문자열로 저장, 화면에는 "카페"로 표시

    SEAFOOD("해산물·회"),   // [추가]2026.11.11권성애 해산물·회 (횟집, 장어, 조개, 복국 등)
    // DB에 "SEAFOOD" 문자열로 저장, 화면에는 "해산물·회"로 표시

    MEAT("고기·구이"),      // [추가]2026.11.11권성애 고기·구이 (갈비, 삼겹살, 곱창 등)
    // DB에 "MEAT" 문자열로 저장, 화면에는 "고기·구이"로 표시

    SOUP("국밥·탕"),        // [추가]2026.11.11권성애 국밥·탕 (돼지국밥, 해장국, 갈비탕 등)
    // DB에 "SOUP" 문자열로 저장, 화면에는 "국밥·탕"으로 표시

    ETC("기타");        // 여섯 번째 카테고리: 기타 (위 5가지에 속하지 않는 음식점)
    // DB에 "ETC" 문자열로 저장, 화면에는 "기타"로 표시
    // 세미콜론(;)으로 enum 상수 목록의 끝을 표시합니다.
    // 쉼표(,)는 다음 값이 있다는 뜻이고, 세미콜론(;)은 더 이상 값이 없다는 뜻

    // ──────────────────────────────────────────────────────────
    // 필드(변수) 영역
    // ──────────────────────────────────────────────────────────

    // 각 enum 상수가 가지고 있는 한글 이름을 저장하는 변수
    // private: 이 클래스 안에서만 직접 접근 가능
    //   외부에서는 getDisplayName() 메서드를 통해서만 읽을 수 있음
    // final: 한 번 값이 정해지면 절대 변경 불가
    // String: 문자열(텍스트) 타입
    // displayName: 변수 이름. "화면에 표시할 이름"이라는 뜻
    private final String displayName;

    // ──────────────────────────────────────────────────────────
    // 생성자(Constructor) 영역
    // ──────────────────────────────────────────────────────────

    // enum 생성자 -- 각 enum 상수가 만들어질 때 자동으로 호출됩니다.
    //
    // 호출 시점:
    //   프로그램이 시작될 때 KOREAN("한식"), CHINESE("중식"), ... 각각에 대해
    //   이 생성자가 한 번씩 호출되어 displayName 값을 설정합니다.
    //
    // 매개변수(Parameter):
    //   String displayName -- 화면에 표시할 한글 이름
    //   예: KOREAN("한식") → displayName = "한식"이 전달됨
    //   예: CHINESE("중식") → displayName = "중식"이 전달됨
    //
    // 왜 public이 아닌가?
    //   enum 생성자는 기본적으로 private입니다 (자바 규칙).
    //   외부에서 new Category("분식")처럼 새 카테고리를 만들 수 없게 막아놓은 것입니다.
    //   새 카테고리가 필요하면 위의 enum 상수 목록에 직접 추가해야 합니다.
    Category(String displayName) {
        this.displayName = displayName;
        // this.displayName: 이 enum 상수(예: KOREAN)의 displayName 필드
        // displayName: 매개변수로 전달받은 값(예: "한식")
        // this를 붙여 구분: "내 필드(this.displayName) = 전달받은 값(displayName)"
    }

    // ──────────────────────────────────────────────────────────
    // 메서드(Method) 영역
    // ──────────────────────────────────────────────────────────

    // 한글 이름을 가져오는 메서드 (Getter 메서드)
    //
    // public: 어디서든 이 메서드를 호출할 수 있음 (공개 메서드)
    // String: 이 메서드가 돌려주는(반환하는) 값의 타입 -- 문자열
    // getDisplayName: 메서드 이름. "표시 이름을 가져오다"라는 뜻
    //   자바 관례: getter 메서드는 "get + 필드명(첫 글자 대문자)"로 이름을 짓습니다.
    // (): 매개변수 없음 -- 아무것도 전달받지 않고, 저장된 값만 돌려줌
    //
    // 사용 예시 (자바 코드에서):
    //   Category.KOREAN.getDisplayName()   → "한식" 반환
    //   Category.JAPANESE.getDisplayName() → "일식" 반환
    //
    // 사용 예시 (타임리프 화면에서):
    //   ${restaurant.category.displayName} → "한식" 출력
    //   타임리프 표현식 ${...}에서는 get을 빼고 소문자로 시작하는 필드명으로 접근합니다.
    //
    // 사용 예시 (맛집 등록 폼에서):
    //   드롭다운 메뉴에 "한식", "중식", "일식" ... 을 표시할 때 이 메서드를 사용
    //
    // 반환값: displayName 필드에 저장된 한글 카테고리 이름 문자열
    public String getDisplayName() {
        return displayName;
        // return: 메서드를 호출한 곳으로 값을 돌려보내는 키워드
        // displayName: 이 enum 상수가 가지고 있는 한글 이름 (예: "한식")
    }

    // [추가]2026.11.11권성애 카테고리 기본 이미지 경로를 가져오는 메서드
    //
    // 이미지가 없는 맛집(imageUrl == null)이나, 외부 이미지가 깨졌을 때(img onerror)
    // 화면에서 대신 보여 줄 카테고리별 기본 이미지 주소를 돌려줍니다.
    // 이미지 파일 위치: src/main/resources/static/images/category/
    //
    // name(): enum 상수 이름 ("KOREAN") → toLowerCase()로 소문자 ("korean")
    //
    // 사용 예시 (자바 코드에서):
    //   Category.KOREAN.getDefaultImageUrl()  → "/images/category/korean.svg"
    //   Category.SEAFOOD.getDefaultImageUrl() → "/images/category/seafood.svg"
    //
    // 사용 예시 (타임리프 화면에서):
    //   ${restaurant.category.defaultImageUrl}
    public String getDefaultImageUrl() {
        return "/images/category/" + name().toLowerCase() + ".svg";
    }
}

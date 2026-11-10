// ============================================================
// [OAuthAttributes.java] OAuth2 소셜 로그인 속성 파싱 클래스
// ============================================================
// 역할: Google 소셜 로그인 제공자(Provider)마다
//       사용자 정보를 다른 형식(JSON 구조)으로 제공하는데,
//       이 클래스가 각 제공자의 형식을 "통일된 양식"으로 변환해줍니다.
//
//
// 각 소셜 로그인 제공자의 사용자 정보 JSON 구조:
//   ■ Google: { "sub": "123", "name": "홍길동", "email": "hong@gmail.com" }
//
// 사용처: CustomOAuth2UserService에서 소셜 로그인 사용자 정보를 파싱할 때
// ============================================================

// ── 패키지 선언 ──────────────────────────────────────────────
package com.example.mymatzip.config.security;

// ── import 문 ────────────────────────────────────────────────

// @Getter: 모든 필드의 getter 메서드를 Lombok이 자동 생성
import lombok.Getter;

// Map: 키-값 쌍으로 데이터를 저장하는 자료구조
// 소셜 로그인 응답 데이터(JSON)가 Map<String, Object> 형태로 전달됩니다.
import java.util.Map;

// ── 클래스 선언 ──────────────────────────────────────────────

// @Getter: getName(), getEmail() 등의 getter 메서드를 자동 생성
@Getter
public class OAuthAttributes {

    // ── 필드 ─────────────────────────────────────────────────

    // attributes: 소셜 로그인 제공자가 전달한 원본 사용자 정보 (JSON → Map)
    private Map<String, Object> attributes;

    // nameAttributeKey: 사용자를 식별하는 키 이름
    // Google: "sub"
    private String nameAttributeKey;

    // name: 사용자 이름 (닉네임으로 사용)
    private String name;

    // email: 사용자 이메일
    private String email;

    // [추가] 소셜 서비스 안에서의 회원 고유 번호 (구글 응답의 sub)
    //   이메일 제공에 동의하지 않은 사용자도 이 값으로 같은 회원을 다시 찾을 수 있습니다.
    private String providerId;

    // ── 생성자 (private) ──────────────────────────────────────
    // 외부에서 직접 new로 생성하지 못하게 하고,
    // 아래의 정적 팩토리 메서드(of, ofGoogle)를 통해서만 생성
    private OAuthAttributes(Map<String, Object> attributes,
                            String nameAttributeKey,
                            String name,
                            String email,
                            String providerId) {
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.name = name;
        this.email = email;
        this.providerId = providerId;
    }

    // ── 정적 팩토리 메서드 ──────────────────────────────────────
    // 호출: CustomOAuth2UserService.loadUser()
    // registrationId       : 소셜 로그인 이름. 이 프로젝트는 "google" 하나만 사용
    // userNameAttributeName: 구글 응답에서 사용자 번호가 들어 있는 키 이름 ("sub")
    // attributes           : 구글이 보내 준 사용자 정보 Map (DefaultOAuth2UserService 객체가 받아 옴)
    public static OAuthAttributes of(String registrationId,
                                     String userNameAttributeName,
                                     Map<String, Object> attributes) {
        return ofGoogle(userNameAttributeName, attributes);
    }

    // ── Google 사용자 정보 파싱 ──────────────────────────────────
    // Google 응답 형식: { "sub": "123", "name": "홍길동", "email": "hong@gmail.com" }
    // sub = 구글 고유 사용자 ID
    // name = 사용자 이름
    // email = 사용자 이메일
    private static OAuthAttributes ofGoogle(String userNameAttributeName,
                                            Map<String, Object> attributes) {
        return new OAuthAttributes(
                attributes,
                userNameAttributeName,
                (String) attributes.get("name"),    // Google은 "name" 키에 이름이 있음
                (String) attributes.get("email"),   // Google은 "email" 키에 이메일이 있음
                String.valueOf(attributes.get("sub"))  // [추가] Google 고유 번호
        );
    }
}
// ============================================================
// [CustomOAuth2UserService.java] OAuth2 소셜 로그인 사용자 서비스
// ============================================================
// 역할: Google 소셜 로그인 후 사용자 정보를 처리합니다.
//       소셜 로그인이 성공하면 Spring Security가 이 클래스를 자동으로 호출하여
//       사용자 정보를 가져오고, DB에 회원을 등록하거나 기존 회원을 조회합니다.
//
//
// 동작 흐름:
//   [사용자가 소셜 로그인 버튼 클릭]
//     → [소셜 서비스에서 인증 성공]
//     → [Spring Security가 이 클래스의 loadUser() 호출]
//     → [소셜 서비스에서 사용자 정보 가져오기]
//     → [OAuthAttributes로 파싱]
//     → [DB에서 기존 회원인지 확인]
//     → [새 회원이면 자동 등록]
//     → [CustomUserDetails로 감싸서 반환]
//
// 사용처: Spring Security OAuth2 내부에서 자동으로 호출됨
// ============================================================

// ── 패키지 선언 ──────────────────────────────────────────────
package com.example.mymatzip.config.security;

// ── import 문 ────────────────────────────────────────────────

// 엔티티
import com.example.mymatzip.entity.Member;
import com.example.mymatzip.entity.Role;

// Repository
import com.example.mymatzip.repository.MemberRepository;

// Lombok
import lombok.RequiredArgsConstructor;

// Spring Security OAuth2
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;

// Spring
import org.springframework.stereotype.Service;

// Java
import java.util.Optional;
import java.util.UUID;

// ── 클래스 선언 ──────────────────────────────────────────────

// @Service: Spring 빈으로 등록
@Service

// @RequiredArgsConstructor: 의존성 주입을 위한 생성자 자동 생성
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    // ── 의존성 필드 ─────────────────────────────────────────────

    // memberRepository: DB에서 회원 정보를 조회/저장하기 위한 저장소
    private final MemberRepository memberRepository;

    // passwordEncoder: 비밀번호를 암호화하기 위한 인코더
    // 소셜 로그인 회원은 비밀번호가 필요 없지만, DB 필드가 NOT NULL이므로 랜덤 값을 암호화하여 저장
    private final PasswordEncoder passwordEncoder;

    // ── loadUser() 메서드 ────────────────────────────────────────
    // Spring Security가 소셜 로그인 성공 후 자동으로 호출하는 메서드
    //
    // 매개변수: userRequest — 소셜 로그인 요청 정보 (어떤 소셜 서비스인지, 토큰 등)
    // 반환값: OAuth2User — Spring Security가 인증에 사용하는 사용자 객체
    //
    // 동작 흐름:
    //   1) 소셜 서비스에서 사용자 정보를 가져옴 (super.loadUser)
    //   2) OAuthAttributes로 파싱 (제공자별 형식 통일)
    //   3) DB에서 기존 회원인지 확인
    //   4) 새 회원이면 자동 등록
    //   5) CustomUserDetails로 감싸서 반환
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // [1단계] 소셜 서비스에서 사용자 정보 가져오기
        // DefaultOAuth2UserService의 loadUser()를 호출하여
        // Google에서 사용자 정보(JSON)를 가져옴
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // [2단계] 소셜 로그인 제공자 정보 추출
        // registrationId: "google"
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // userNameAttributeName: 소셜 서비스에서 사용자를 식별하는 키 이름
        // Google: "sub"
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        // [3단계] OAuthAttributes로 파싱 — 각 제공자별 형식을 통일된 양식으로 변환
        OAuthAttributes oAuthAttributes = OAuthAttributes.of(
                registrationId, userNameAttributeName, oAuth2User.getAttributes());

        // [4단계] DB에서 기존 회원 확인 또는 신규 등록
        Member member = saveOrUpdate(oAuthAttributes, registrationId);

        // [5단계] CustomUserDetails로 감싸서 반환
        // Spring Security가 이 객체를 통해 인증 정보를 관리함
        // [수정] 소셜 서비스가 준 원본 정보도 함께 담아 둡니다 (OAuth2User.getAttributes()).
        CustomUserDetails userDetails = new CustomUserDetails(member);
        userDetails.setAttributes(oAuth2User.getAttributes());
        return userDetails;
    }

    // ── saveOrUpdate() 메서드 ─────────────────────────────────────
    // 역할: 소셜 로그인 사용자를 DB에서 찾거나, 없으면 새로 등록
    //
    // 동작:
    //   1) 이메일로 기존 회원 조회
    //   2) 기존 회원이면 → 그대로 반환
    //   3) 새 회원이면 → 자동 회원가입 후 반환
    //
    private Member saveOrUpdate(OAuthAttributes attributes, String registrationId) {

        // 이메일로 기존 회원 조회
        // [수정] ① 같은 소셜 계정(서비스 + 고유 번호)으로 가입한 회원이 있으면 그 회원
        Optional<Member> socialMember =
                memberRepository.findByProviderAndProviderId(registrationId, attributes.getProviderId());
        if (socialMember.isPresent()) {
            return socialMember.get();
        }

        // [수정] ② 이메일을 받지 못한 경우(제공 동의 안 함) 회원 테이블의 email 이 비면 안 되므로
        //   "서비스_고유번호@social.local" 형태의 대체 이메일을 씁니다.
        String email = attributes.getEmail();
        if (email == null || email.isBlank()) {
            email = registrationId + "_" + attributes.getProviderId() + "@social.local";
        }

        // ③ 같은 이메일로 이미 가입한 회원이 있으면 그 회원에 소셜 계정을 연결
        Optional<Member> existingMember = memberRepository.findByEmail(email);

        if (existingMember.isPresent()) {
            // 기존 회원이 있으면 그대로 반환 (소셜 로그인으로 다시 접속한 것)
            Member member = existingMember.get();
            member.linkSocial(registrationId, attributes.getProviderId());   // [수정] 다음부터 ① 로 찾음
            return memberRepository.save(member);
        }

        // 새 회원이면 자동 회원가입
        // 비밀번호: 소셜 로그인은 비밀번호가 필요 없지만, DB 컬럼이 NOT NULL이므로
        //           UUID(랜덤 문자열)를 생성하여 암호화 후 저장
        //           이 비밀번호로는 일반 로그인 불가능 (알 수 없는 랜덤값이므로)
        String randomPassword = passwordEncoder.encode(UUID.randomUUID().toString());

        // 닉네임: 소셜 서비스에서 가져온 이름 사용
        // 만약 닉네임이 이미 DB에 있으면 뒤에 랜덤 숫자를 붙임
        String nickname = attributes.getName();
        if (nickname == null || nickname.isEmpty()) {
            nickname = registrationId + "_user";
        }
        if (memberRepository.existsByNickname(nickname)) {
            nickname = nickname + "_" + (int)(Math.random() * 10000);
        }

        // Member 엔티티 생성 (빌더 패턴 사용)
        Member newMember = Member.builder()
                .email(email)                     // 소셜 서비스에서 가져온 이메일 (없으면 대체 이메일)
                .password(randomPassword)         // 랜덤 암호화 비밀번호
                .nickname(nickname)               // 소셜 서비스에서 가져온 이름
                .role(Role.USER)                  // 기본 역할: 일반 사용자
                .build();

        // DB에 저장 후 반환
        newMember.linkSocial(registrationId, attributes.getProviderId());   // [추가] 소셜 계정 정보 저장
        return memberRepository.save(newMember);
    }
}
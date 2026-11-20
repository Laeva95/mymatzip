package com.example.mymatzip.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// ============================================================
// [OwnerApplicationViewController.java] 식당 사장 신청 화면
// ============================================================
//
// GET /owner/apply → owner/apply.html (로그인한 회원이 사장 권한을 신청하고 진행 상태를 보는 화면)
// GET /admin/owner-applications → admin/owner-applications.html (관리자 심사 화면)
//
// 보안: /owner/apply 는 anyRequest().authenticated(), /admin/** 는 ADMIN 전용 (SecurityConfig 수정 불필요)
// 화면 데이터는 모두 fetch 로 API 에서 가져오므로 여기서 Model 에 넣는 값은 없음
// (loginMember 는 GlobalModelAdvice 가 모든 화면에 자동으로 넣어줌)
// AdminViewController(다른 팀원 파일)를 수정하지 않으려고 별도 컨트롤러로 분리
@Controller
public class OwnerApplicationViewController {

    @GetMapping("/owner/apply")
    public String applyPage() {
        return "owner/apply";
    }

    @GetMapping("/admin/owner-applications")
    public String adminListPage() {
        return "admin/owner-applications";
    }
}

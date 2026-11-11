package com.example.mymatzip.controller.api;

import com.example.mymatzip.config.security.LoginMember;
import com.example.mymatzip.dto.ReplyForm;
import com.example.mymatzip.dto.ReplyResponse;
import com.example.mymatzip.entity.Member;
import com.example.mymatzip.service.ReplyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/replies")
@RequiredArgsConstructor
public class ReplyApiController {

    private final ReplyService replyService;

    @GetMapping("/review/{reviewId}")
    public ResponseEntity<List<ReplyResponse>> listByReview(@PathVariable Long reviewId) {
        return ResponseEntity.ok(replyService.findByReviewId(reviewId));
    }

    @PostMapping
    public ResponseEntity<ReplyResponse> create(@Valid @RequestBody ReplyForm form,
                                                @LoginMember Member loginMember) {
        ReplyResponse response = replyService.save(form, loginMember);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id,
                                                      @LoginMember Member loginMember) {
        replyService.delete(id, loginMember);
        return ResponseEntity.ok(Map.of("message", "대댓글이 삭제되었습니다."));
    }
}

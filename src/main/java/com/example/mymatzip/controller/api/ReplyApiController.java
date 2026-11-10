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
    public ResponseEntity<?> create(@Valid @RequestBody ReplyForm form,
                                    @LoginMember Member loginMember) {
        try {
            ReplyResponse response = replyService.save(form, loginMember);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                    @LoginMember Member loginMember) {
        try {
            replyService.delete(id, loginMember);
            return ResponseEntity.ok(Map.of("message", "대댓글이 삭제되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }
}
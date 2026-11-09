package com.example.mymatzip.service;

import com.example.mymatzip.dto.ReplyForm;
import com.example.mymatzip.dto.ReplyResponse;
import com.example.mymatzip.entity.Member;
import com.example.mymatzip.entity.Reply;
import com.example.mymatzip.entity.Review;
import com.example.mymatzip.repository.ReplyRepository;
import com.example.mymatzip.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReplyService {

    private final ReplyRepository replyRepository;
    private final ReviewRepository reviewRepository;

    public List<ReplyResponse> findByReviewId(Long reviewId) {
        return replyRepository.findByReviewIdOrderByCreatedAtAsc(reviewId).stream()
                .map(ReplyResponse::from)
                .toList();
    }

    @Transactional
    public ReplyResponse save(ReplyForm form, Member loginMember) {
        Review review = reviewRepository.findById(form.getReviewId())
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        Reply reply = Reply.builder()
                .content(form.getContent())
                .review(review)
                .member(loginMember)
                .build();

        return ReplyResponse.from(replyRepository.save(reply));
    }

    @Transactional
    public void delete(Long id, Member loginMember) {
        Reply reply = replyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("대댓글을 찾을 수 없습니다."));

        if (!reply.getMember().getId().equals(loginMember.getId())) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        replyRepository.delete(reply);
    }
}
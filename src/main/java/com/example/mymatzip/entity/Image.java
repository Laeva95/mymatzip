// ============================================================
// [Image.java] 이미지 엔티티 — DB의 image 테이블과 1:1 매핑되는 클래스
// ============================================================
// 역할: 사용자가 업로드한 이미지 파일의 정보를 저장하는 DB 테이블을
//       자바 클래스로 표현한 것입니다.
//       이 클래스의 객체 하나 = DB image 테이블의 행(row) 하나 = 이미지 1장
//
//
// [왜 파일 이름을 UUID로 바꿔서 저장하나요?]
//   여러 사용자가 "맛집사진.jpg"라는 같은 이름의 파일을 업로드하면 충돌이 발생합니다.
//   UUID(고유한 랜덤 문자열)로 이름을 바꾸면 절대로 이름이 겹치지 않습니다.
//
// 사용되는 곳:
//   - FileService (이미지 업로드 시 DB에 이미지 정보 저장)
//   - Restaurant, Review 엔티티의 imageUrl 필드에 filePath 값이 저장됨
// ============================================================

package com.example.mymatzip.entity;

import jakarta.persistence.*;
// JPA 관련 어노테이션:
//   @Entity, @Id, @GeneratedValue, @Column, GenerationType

import lombok.AccessLevel;
// 접근 수준 열거형. @NoArgsConstructor에서 PROTECTED 수준 지정에 사용

import lombok.Builder;
// 빌더 패턴 자동 생성 어노테이션

import lombok.Getter;
// 모든 필드의 getter 메서드를 자동 생성

import lombok.NoArgsConstructor;
// JPA 전용 기본 생성자 자동 생성

import java.time.LocalDateTime;
// 날짜+시간 저장용 클래스

// @Entity: 이 클래스 = DB의 image 테이블
@Entity

// @Getter: 모든 필드의 getXxx() 메서드를 자동 생성
@Getter

// @NoArgsConstructor(access = AccessLevel.PROTECTED)
//   JPA 전용 기본 생성자. 외부에서 빈 이미지 객체 생성 방지
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Image {

    // ── 기본키(PK) 필드 ──
    // DB가 자동으로 번호를 매겨줍니다 (1, 2, 3, ...)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── 원본 파일명 ──
    // 사용자가 업로드한 원래 파일 이름 (예: "맛집사진.jpg")
    // 다운로드하거나 화면에 표시할 때 원본 이름을 보여주기 위해 저장
    @Column(nullable = false)
    private String originalFileName;

    // ── 저장된 파일명 (UUID) ──
    // 서버에 실제로 저장되는 파일 이름 (예: "a1b2c3d4-5678-9012-abcd-e5f6g7h8i9j0.jpg")
    // UUID로 생성하여 파일 이름 충돌을 방지합니다.
    @Column(nullable = false)
    private String storedFileName;

    // ── 파일 저장 경로 ──
    // 파일이 저장된 서버 경로 (예: "/uploads/a1b2c3d4-...-e5f6.jpg")
    // 웹 브라우저에서 이미지를 불러올 때 이 경로를 사용합니다.
    @Column(nullable = false)
    private String filePath;

    // ── 파일 크기 (bytes) ──
    // 파일의 크기를 바이트 단위로 저장합니다.
    // 예: 1048576 bytes = 1MB
    private long fileSize;

    // ── MIME 타입 ──
    // 파일의 종류를 나타내는 문자열 (예: "image/jpeg", "image/png", "image/gif")
    // 브라우저가 이 값을 보고 파일을 어떻게 표시할지 결정합니다.
    private String contentType;

    // ── 업로드 일시 ──
    // 이미지가 업로드된 날짜+시간. 한 번 저장되면 수정 불가(updatable = false)
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── [추가]2026.11.12권성애 소프트 삭제 시각 ──
    // null = 사용 중, 값이 있음 = 맛집 삭제 · 사진 교체로 더 이상 쓰지 않는 사진 (행은 남김)
    // S3 파일과 이 행은 코드에서 지우지 않고 개발자가 직접 삭제 → 이 칸이 "지울 사진 목록"이 됨
    //   SELECT file_path, deleted_at FROM image WHERE deleted_at IS NOT NULL;
    // nullable 이라 기존 행은 자동으로 null (팀장이 테이블 drop 후 create 모드 재시작하면 칸이 생김)
    private LocalDateTime deletedAt;

    // ── 생성자 (Builder 패턴) ──
    // @Builder: Image.builder().originalFileName("맛집.jpg").storedFileName("uuid.jpg")...build()
    // createdAt은 자동으로 현재 시간이 설정됩니다.
    @Builder
    public Image(String originalFileName, String storedFileName,
                 String filePath, long fileSize, String contentType) {
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.createdAt = LocalDateTime.now();
    }

    // ── [추가]2026.11.12권성애 소프트 삭제 (Restaurant.softDelete() 와 같은 이름) ──
    // 호출: FileService.deleteImageRecord() ← RestaurantService.delete() · update() (같은 트랜잭션)
    // @Transactional 안에서 값만 바꾸면 변경 감지로 UPDATE image SET deleted_at = ? WHERE id = ? 자동 실행
    // 이미 삭제 표시가 있으면 그대로 둠 → 처음 지운 시각 유지 (두 번 불려도 안전)
    public void softDelete() {
        if (this.deletedAt == null) {
            this.deletedAt = LocalDateTime.now();
        }
    }
}

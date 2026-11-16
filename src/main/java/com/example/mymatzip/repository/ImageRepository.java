package com.example.mymatzip.repository;

import com.example.mymatzip.entity.Image;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// ============================================================
// [ImageRepository] 업로드한 이미지 정보(Image 엔티티)를 저장·조회하는 Repository
// ============================================================
// JpaRepository<Image, Long> 을 상속하면 save, findById, findAll, deleteById 등을
// 구현 없이 사용할 수 있습니다. FileService 가 파일을 저장한 뒤 그 정보를 기록할 때 씁니다.
public interface ImageRepository extends JpaRepository<Image, Long> {

    // [변경]2026.11.12권성애 deleteByStoredFileName(행 DELETE) 없앰 → 찾아서 소프트 삭제
    // [추가]2026.11.12권성애 파일 이름(UUID.확장자)으로 image 행 찾기 — FileService.deleteImageRecord() 가 찾아서 softDelete()
    //   메서드 이름으로 만드는 쿼리: SELECT * FROM image WHERE stored_file_name = ?
    //   Optional: 없을 수도 있음 (공공 API 외부 URL · 기록 없이 올라간 예전 사진)
    Optional<Image> findByStoredFileName(String storedFileName);

}

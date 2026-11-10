package com.example.mymatzip.repository;

import com.example.mymatzip.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;

// ============================================================
// [ImageRepository] 업로드한 이미지 정보(Image 엔티티)를 저장·조회하는 Repository
// ============================================================
// JpaRepository<Image, Long> 을 상속하면 save, findById, findAll, deleteById 등을
// 구현 없이 사용할 수 있습니다. FileService 가 파일을 저장한 뒤 그 정보를 기록할 때 씁니다.
public interface ImageRepository extends JpaRepository<Image, Long> {
}

package com.example.mymatzip.controller.api;

import com.example.mymatzip.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

// ============================================================
// [FileApiController] 사진 업로드·조회 API (B 맛집 담당)
// ============================================================
// POST /api/files/upload      : 사진 올리기 (로그인 필요) → {"url": "..."}
// GET  /api/files/{filename}  : 사진 보기 (누구나, SecurityConfig 에서 GET 만 허용)
// [추가]2026.11.12권성애 S3 전용 + 폴더 구분 (기존 두 주소는 공용이라 그대로 두고 추가)
// POST /api/files/uploadfors3only           : 사진 올리기 (file, folder=restaurant|review) → {"url": "/api/files/restaurant/UUID.jpg"}
// GET  /api/files/{folder}/{filename}       : 사진 보기 (SecurityConfig 의 GET /api/files/** permitAll 에 포함)
// ============================================================
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileApiController {

    private final FileService fileService;

    // 호출: form.html 의 imageFile change 이벤트 → fetch POST (FormData 에 file 담음)
    // @RequestParam("file"): multipart 요청에서 이름이 file 인 부분을 StandardMultipartHttpServletRequest 객체가 꺼내
    //   MultipartFile 객체로 넣어 준다. 크기 제한(5MB)은 application.properties 의 multipart 설정.
    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        try {
            String url = fileService.upload(file);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "파일 업로드에 실패했습니다."));
        } catch (RuntimeException e) {
            // S3 권한 오류 등 AWS SDK 예외
            return ResponseEntity.internalServerError().body(Map.of("error", "사진 저장소에 연결할 수 없습니다."));
        }
    }

    // 호출: 화면의 <img src="/api/files/uuid.jpg"> 를 브라우저가 요청할 때
    // 반환: 사진 바이트 + Content-Type. 브라우저가 1일 동안 캐시하도록 Cache-Control 을 붙인다.
    @GetMapping("/{filename:.+}")
    public ResponseEntity<byte[]> download(@PathVariable String filename) {
        try {
            FileService.StoredFile file = fileService.download(filename);
            if (file == null) {
                return ResponseEntity.notFound().build();
            }
            MediaType type = file.contentType() != null
                    ? MediaType.parseMediaType(file.contentType())
                    : MediaType.APPLICATION_OCTET_STREAM;
            return ResponseEntity.ok()
                    .contentType(type)
                    .cacheControl(CacheControl.maxAge(Duration.ofDays(1)))
                    .body(file.bytes());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // [추가]2026.11.12권성애 사진 올리기 — S3 전용 + 폴더 구분
    // 호출: form.html uploadImageOnSave() → fetch POST /api/files/uploadfors3only (FormData: file, folder=restaurant)
    //   (JS · 컨트롤러 · 서비스 메서드 이름을 uploadForS3Only 로 같게)
    // @RequestParam("folder"): "restaurant" 또는 "review" — 그 밖은 FileService 가 400 안내
    @PostMapping("/uploadfors3only")
    public ResponseEntity<?> uploadForS3Only(@RequestParam("file") MultipartFile file,
                                             @RequestParam("folder") String folder) {
        try {
            String url = fileService.uploadForS3Only(file, folder);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            // S3 설정이 없음 → 503 + "사진 저장소(S3) 설정이 없습니다."
            return ResponseEntity.status(503).body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "파일 업로드에 실패했습니다."));
        } catch (RuntimeException e) {
            // S3 권한 오류 등 AWS SDK 예외
            return ResponseEntity.internalServerError().body(Map.of("error", "사진 저장소에 연결할 수 없습니다."));
        }
    }

    // [추가]2026.11.12권성애 사진 보기 — S3 전용 + 폴더 구분
    // 호출: 화면의 <img src="/api/files/restaurant/UUID.jpg"> 를 브라우저가 요청할 때
    //   기존 "/{filename:.+}" 는 한 단계 주소라 두 단계인 이 주소와 겹치지 않음
    @GetMapping("/{folder}/{filename:.+}")
    public ResponseEntity<byte[]> downloadForS3Only(@PathVariable String folder, @PathVariable String filename) {
        try {
            FileService.StoredFile file = fileService.downloadForS3Only(folder, filename);
            if (file == null) {
                return ResponseEntity.notFound().build();
            }
            MediaType type = file.contentType() != null
                    ? MediaType.parseMediaType(file.contentType())
                    : MediaType.APPLICATION_OCTET_STREAM;
            return ResponseEntity.ok()
                    .contentType(type)
                    .cacheControl(CacheControl.maxAge(Duration.ofDays(1)))
                    .body(file.bytes());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).build();
        }
    }
}

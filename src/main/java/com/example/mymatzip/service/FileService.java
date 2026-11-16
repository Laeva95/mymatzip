package com.example.mymatzip.service;

import com.example.mymatzip.entity.Image;
import com.example.mymatzip.repository.ImageRepository;
import lombok.extern.slf4j.Slf4j;                                    // [추가]2026.11.11권성애 로그 (log.info / log.warn)
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

// ============================================================
// [FileService] 맛집 사진 업로드 (B 맛집 담당)
// ============================================================
// 저장 위치는 application.properties 의 app.storage 값으로 정한다.
//   local : 프로젝트 폴더의 uploads/ 에 저장 → 주소 /uploads/파일이름   (내 PC 개발용)
//   s3    : AWS S3 버킷의 images/ 에 저장   → 주소 /api/files/파일이름 (EC2 배포용)
// S3 버킷은 비공개로 두고, 사진은 FileApiController 의 GET /api/files/{filename} 이 S3 에서 읽어 응답한다.
// [추가]2026.11.11권성애 삭제: 맛집 소프트 삭제 · 사진 교체 때 RestaurantService 가 부름
//   deleteImageRecord() : image 테이블 기록 [변경]2026.11.12권성애 삭제(DELETE) → 소프트 삭제(deleted_at 기록), 맛집 삭제 · 수정과 같은 트랜잭션 안에서
//   [변경]2026.11.12권성애 deleteQuietly()(S3 · uploads 파일 삭제) 없앰 — 사진 파일은 코드에서 지우지 않고 개발자가 AWS 콘솔에서 직접 삭제
//     (SdkException · DeleteObjectRequest import 도 함께 없앰)
// [추가]2026.11.12권성애 S3 전용 + 폴더 구분 (기존 upload() · download() 는 공용이라 그대로 두고 추가)
//   uploadForS3Only(file, folder)       : images/{folder}/UUID.확장자 에 저장 → /api/files/{folder}/UUID.확장자
//   downloadForS3Only(folder, filename) : S3 의 images/{folder}/파일이름 읽기
//   folder 는 "restaurant" · "review" 두 가지만 (경로 조작 방지). S3 설정이 없으면 "사진 저장소(S3) 설정이 없습니다"
// ============================================================
@Slf4j      // [추가]2026.11.11권성애
@Service
public class FileService {

    private static final String UPLOAD_DIR = "uploads";
    private static final String S3_PREFIX = "images/";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    // 저장 파일 이름 규칙: UUID 36자 + 확장자. 이 모양이 아니면 다운로드를 거부한다(경로 조작 방지).
    private static final Pattern STORED_NAME = Pattern.compile("^[0-9a-f\\-]{36}\\.(jpg|jpeg|png|gif|webp)$");

    private final ImageRepository imageRepository;
    private final String storage;
    private final String bucket;
    private final S3Client s3Client;   // app.storage=s3 일 때만 만든다. local 이면 null.

    // @Value: application.properties 의 값을 생성자 매개변수에 넣는다.
    //   이 생성자는 DefaultListableBeanFactory 객체가 서버 시작 시 한 번 호출한다.
    public FileService(ImageRepository imageRepository,
                       @Value("${app.storage:local}") String storage,
                       @Value("${app.s3.bucket:}") String bucket,
                       @Value("${app.s3.region:ap-northeast-2}") String region) {
        this.imageRepository = imageRepository;
        this.storage = storage;
        this.bucket = bucket;
        if (isS3()) {
            if (bucket == null || bucket.isBlank()) {
                throw new IllegalStateException("app.storage=s3 이면 S3_BUCKET 환경 변수가 필요합니다.");
            }
            // S3Client.builder().build():
            //   접근 권한은 SDK 의 DefaultCredentialsProvider 객체가 순서대로 찾는다.
            //   (환경 변수 → ~/.aws/credentials → EC2 에 연결된 IAM 역할)
            //   EC2 에서는 IAM 역할(matzip-ec2-role)의 임시 키를 자동으로 쓰므로 코드에 키를 넣지 않는다.
            this.s3Client = S3Client.builder().region(Region.of(region)).build();
        } else {
            this.s3Client = null;
        }
    }

    public boolean isS3() {
        return "s3".equalsIgnoreCase(storage);
    }

    // 호출하는 곳: FileApiController.upload() ← form.html 의 사진 선택(fetch POST /api/files/upload)
    // 반환: 화면에서 <img src> 로 쓸 주소 → FileApiController 가 {"url": ...} JSON 으로 응답
    //       → form.html 이 hidden 입력 imageUrl 에 넣고, 맛집 저장 시 RestaurantForm.imageUrl 로 함께 보낸다.
    public String upload(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }
        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다. (jpg, png, gif, webp만 가능)");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 올릴 수 있습니다.");
        }

        // 원래 이름 대신 UUID 이름으로 저장 (같은 이름 덮어쓰기, 한글·특수문자 문제 방지)
        String savedFilename = UUID.randomUUID() + "." + extension;
        String url;
        String storedPath;

        if (isS3()) {
            // PutObjectRequest: 어느 버킷의 어떤 키(경로)에 어떤 형식으로 올릴지 정하는 요청 객체
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(S3_PREFIX + savedFilename)
                    .contentType(contentType)
                    .build();
            // RequestBody.fromInputStream: 업로드 파일 내용을 메모리에 다 올리지 않고 흘려보내며 전송
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            storedPath = "s3://" + bucket + "/" + S3_PREFIX + savedFilename;
            url = "/api/files/" + savedFilename;
        } else {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Path filePath = uploadPath.resolve(savedFilename);
            file.transferTo(filePath.toAbsolutePath().toFile());
            storedPath = filePath.toString();
            url = "/uploads/" + savedFilename;   // WebMvcConfig 가 /uploads/** → file:uploads/ 로 연결
        }

        // JPA: INSERT INTO image (...) VALUES (...) — 업로드 기록 저장
        imageRepository.save(Image.builder()
                .originalFileName(originalFilename)
                .storedFileName(savedFilename)
                .filePath(storedPath)
                .fileSize(file.getSize())
                .contentType(contentType)
                .build());
        return url;
    }

    // 호출하는 곳: FileApiController.download() ← 화면의 <img src="/api/files/파일이름">
    // 반환: S3 에서 읽은 파일 내용과 형식. 없으면 null → 컨트롤러가 404 응답
    public StoredFile download(String filename) {
        if (!STORED_NAME.matcher(filename).matches()) {
            throw new IllegalArgumentException("잘못된 파일 이름입니다.");
        }
        if (isS3()) {
            try {
                // getObjectAsBytes: S3 의 images/파일이름 객체를 byte 배열로 읽는다
                ResponseBytes<GetObjectResponse> bytes = s3Client.getObjectAsBytes(
                        GetObjectRequest.builder().bucket(bucket).key(S3_PREFIX + filename).build());
                return new StoredFile(bytes.asByteArray(), bytes.response().contentType());
            } catch (NoSuchKeyException e) {
                return null;
            }
        }
        try {
            Path path = Paths.get(UPLOAD_DIR).resolve(filename);
            if (!Files.exists(path)) return null;
            return new StoredFile(Files.readAllBytes(path), Files.probeContentType(path));
        } catch (IOException e) {
            return null;
        }
    }

    // 파일 내용 + 형식(image/png 등)
    public record StoredFile(byte[] bytes, String contentType) {}

    // [변경]2026.11.12권성애 deleteQuietly() 없앰 (S3 · uploads 파일 삭제) — 사진 파일은 개발자가 AWS 콘솔에서 직접 삭제
    //   지울 목록: SELECT file_path, deleted_at FROM image WHERE deleted_at IS NOT NULL;  (file_path = s3://버킷/images/restaurant/UUID.jpg)

    /**
     * [추가]2026.11.11권성애 image 테이블의 업로드 기록 정리
     *  [변경]2026.11.12권성애 기록 삭제(DELETE) → <b>소프트 삭제</b>(deleted_at 기록). S3 파일은 지우지 않음(개발자가 직접)
     *  맛집 삭제 · 수정과 <b>같은 트랜잭션 안에서</b> 부름 → 맛집 저장이 실패하면 삭제 표시도 함께 되돌아감
     *  찾은 Image 의 값만 바꾸면 트랜잭션이 끝날 때 변경 감지로 UPDATE 자동 실행 (save() 불필요)
     *  기록이 없으면(공공 API 외부 URL · 기록 없이 올라간 예전 사진) 그냥 넘어감
     * 호출하는 곳: RestaurantService.delete() · update()
     */
    public void deleteImageRecord(String imageUrl) {
        String filename = storedNameOf(imageUrl);
        if (filename == null) {
            return;                                           // 우리 파일이 아님 → 기록도 없음
        }
        // [변경]2026.11.12권성애 폴더가 붙어 있으면("restaurant/UUID.jpg") 마지막 / 뒤의 파일 이름만 — image 테이블에는 파일 이름만 저장됨
        String storedFileName = filename.substring(filename.lastIndexOf('/') + 1);
        // [변경]2026.11.12권성애 DELETE → 찾아서 소프트 삭제
        //   findByStoredFileName: SELECT * FROM image WHERE stored_file_name = ? (메서드 이름으로 만드는 쿼리)
        //   ifPresent(Image::softDelete): 찾았을 때만 softDelete() 호출 → UPDATE image SET deleted_at = ? WHERE id = ?
        imageRepository.findByStoredFileName(storedFileName)
                .ifPresent(Image::softDelete);
    }

    // [추가]2026.11.11권성애 "/api/files/UUID.jpg" · "/uploads/UUID.jpg" → "UUID.jpg" (우리 파일 이름 규칙이 아니면 null)
    //   STORED_NAME 검사는 download() 와 같음 → "../" 같은 경로 조작 방지
    private String storedNameOf(String imageUrl) {
        if (imageUrl == null) {
            return null;
        }
        String filename;
        if (imageUrl.startsWith("/api/files/")) {
            filename = imageUrl.substring("/api/files/".length());
        } else if (imageUrl.startsWith("/uploads/")) {
            filename = imageUrl.substring("/uploads/".length());
        } else {
            return null;
        }
        // [변경]2026.11.12권성애 폴더가 붙은 새 주소(/api/files/restaurant/UUID.jpg)도 알아봄 → "restaurant/UUID.jpg" 를 돌려줌
        //   폴더는 restaurant · review 만, 파일 이름은 STORED_NAME 규칙만 (그 밖은 null → 지우지 않음)
        int slash = filename.indexOf('/');
        if (slash >= 0) {
            String folder = filename.substring(0, slash);
            String name = filename.substring(slash + 1);
            boolean okFolder = "restaurant".equals(folder) || "review".equals(folder);
            return (okFolder && STORED_NAME.matcher(name).matches()) ? folder + "/" + name : null;
        }
        return STORED_NAME.matcher(filename).matches() ? filename : null;
    }

    /**
     * [추가]2026.11.12권성애 사진 올리기 — S3 전용 + 폴더 구분 (기존 upload() 는 공용이라 그대로 두고 추가)
     *  저장 위치: S3 images/{folder}/UUID.확장자,  돌려주는 주소: /api/files/{folder}/UUID.확장자
     *  folder: "restaurant"(맛집) · "review"(리뷰)만
     *  S3 설정이 없으면(app.storage 가 s3 가 아니면) IllegalStateException "사진 저장소(S3) 설정이 없습니다"
     * 호출: FileApiController.uploadForS3Only() ← form.html uploadImageOnSave() (folder=restaurant)
     */
    public String uploadForS3Only(MultipartFile file, String folder) throws IOException {
        if (!isS3()) {
            throw new IllegalStateException("사진 저장소(S3) 설정이 없습니다.");
        }
        if (!"restaurant".equals(folder) && !"review".equals(folder)) {
            throw new IllegalArgumentException("사진 폴더는 restaurant 또는 review 만 쓸 수 있습니다.");
        }
        // 파일 검사는 기존 upload() 와 같음 (빈 파일 · 확장자 · 이미지 형식)
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }
        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다. (jpg, png, gif, webp만 가능)");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 올릴 수 있습니다.");
        }

        String savedFilename = UUID.randomUUID() + "." + extension;
        String key = S3_PREFIX + folder + "/" + savedFilename;                   // images/restaurant/UUID.jpg
        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        // 업로드 기록 (기존 upload() 와 같은 항목). storedFileName 에는 파일 이름만 → 삭제할 때 deleteImageRecord 가 찾음
        imageRepository.save(Image.builder()
                .originalFileName(originalFilename)
                .storedFileName(savedFilename)
                .filePath("s3://" + bucket + "/" + key)
                .fileSize(file.getSize())
                .contentType(contentType)
                .build());
        return "/api/files/" + folder + "/" + savedFilename;
    }

    /**
     * [추가]2026.11.12권성애 사진 읽기 — S3 전용 + 폴더 구분 (기존 download() 는 공용이라 그대로 두고 추가)
     *  S3 images/{folder}/{filename} 을 읽음. 없으면 null → 컨트롤러가 404
     * 호출: FileApiController.downloadForS3Only() ← 화면의 &lt;img src="/api/files/restaurant/UUID.jpg"&gt;
     */
    public StoredFile downloadForS3Only(String folder, String filename) {
        if (!"restaurant".equals(folder) && !"review".equals(folder)) {
            throw new IllegalArgumentException("잘못된 사진 폴더입니다.");
        }
        if (!STORED_NAME.matcher(filename).matches()) {
            throw new IllegalArgumentException("잘못된 파일 이름입니다.");
        }
        if (!isS3()) {
            throw new IllegalStateException("사진 저장소(S3) 설정이 없습니다.");
        }
        try {
            ResponseBytes<GetObjectResponse> bytes = s3Client.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket).key(S3_PREFIX + folder + "/" + filename).build());
            return new StoredFile(bytes.asByteArray(), bytes.response().contentType());
        } catch (NoSuchKeyException e) {
            return null;
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("파일 확장자를 확인할 수 없습니다.");
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}

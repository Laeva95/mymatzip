package com.example.mymatzip.service;

import com.example.mymatzip.entity.Image;
import com.example.mymatzip.repository.ImageRepository;
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
// ============================================================
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

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("파일 확장자를 확인할 수 없습니다.");
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}

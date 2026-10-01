package com.example.securefileupload.file;

import com.example.securefileupload.common.ApiException;
import com.example.securefileupload.common.ExtensionFormat;
import com.example.securefileupload.policy.FileExtensionPolicyRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FileUploadService {
    static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Logger log = LoggerFactory.getLogger(FileUploadService.class);
    private final FileExtensionPolicyRepository policyRepository;
    private final Path uploadDirectory;

    public FileUploadService(FileExtensionPolicyRepository policyRepository,
                             @Value("${app.upload.directory:./uploads}") String uploadDirectory) {
        this.policyRepository = policyRepository;
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    public UploadResponse upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("File upload validation failed: reason=empty_file");
            throw new ApiException(HttpStatus.BAD_REQUEST, "빈 파일은 업로드할 수 없습니다.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("File upload validation failed: reason=file_too_large, size={}", file.getSize());
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "파일 크기는 최대 10MB까지 허용됩니다.");
        }

        // 업로드 허용 여부는 화면 상태가 아닌 이 요청 시점의 DB 정책으로 결정한다.
        String extension = extractExtension(file.getOriginalFilename());
        if (policyRepository.existsByExtensionAndBlockedTrue(extension)) {
            log.warn("File upload blocked: extension={}, size={}", extension, file.getSize());
            throw new ApiException(HttpStatus.BAD_REQUEST, "." + extension + " 확장자는 업로드가 차단되어 있습니다.");
        }

        // 원본 파일명은 저장 경로에 사용하지 않고, 검증된 확장자만 UUID에 붙인다.
        String storedFileName = UUID.randomUUID() + "." + extension;
        Path destination = uploadDirectory.resolve(storedFileName);
        try {
            Files.createDirectories(uploadDirectory);
            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, destination);
            }
        } catch (IOException | SecurityException e) {
            // 복사 도중 실패하면 생성됐을 수 있는 불완전한 파일을 지운다.
            try {
                Files.deleteIfExists(destination);
            } catch (IOException | SecurityException ignored) {
            }
            log.error("File storage failed: extension={}, size={}", extension, file.getSize(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "파일을 저장하지 못했습니다.");
        }
        log.info("File upload succeeded: extension={}, size={}", extension, file.getSize());
        return new UploadResponse(storedFileName, extension, file.getSize());
    }

    static String extractExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            log.warn("File upload validation failed: reason=missing_filename");
            throw new ApiException(HttpStatus.BAD_REQUEST, "파일명이 없는 파일은 업로드할 수 없습니다.");
        }
        // 경로 형태의 원본 이름도 마지막 파일명만 보고 판단하며 실제 저장에는 사용하지 않는다.
        String filename = originalFilename.replace('\\', '/');
        filename = filename.substring(filename.lastIndexOf('/') + 1);
        // 이중 확장자는 마지막 점 뒤를 기준으로 삼고 .env 같은 dotfile은 확장자 없음으로 본다.
        int lastDot = filename.lastIndexOf('.');
        if (lastDot <= 0 || lastDot == filename.length() - 1) {
            log.warn("File upload validation failed: reason=missing_extension_or_dotfile");
            throw new ApiException(HttpStatus.BAD_REQUEST, "확장자가 없는 파일 또는 dotfile은 업로드할 수 없습니다.");
        }
        String extension = filename.substring(lastDot + 1).trim().toLowerCase(Locale.ROOT);
        if (!ExtensionFormat.isValid(extension)) {
            log.warn("File upload validation failed: reason=invalid_extension");
            throw new ApiException(HttpStatus.BAD_REQUEST, "파일 확장자는 영문 소문자와 숫자로 된 1~20자여야 합니다.");
        }
        return extension;
    }

    public record UploadResponse(String storedFileName, String extension, long size) {
    }
}

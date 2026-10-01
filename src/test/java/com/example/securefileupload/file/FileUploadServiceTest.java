package com.example.securefileupload.file;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.securefileupload.common.ApiException;
import com.example.securefileupload.policy.FileExtensionPolicyRepository;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class FileUploadServiceTest {
    @Mock
    FileExtensionPolicyRepository policyRepository;
    @TempDir
    Path tempDirectory;

    @Test
    @DisplayName("허용된 파일 업로드_UUID 이름으로 지정한 디렉터리에 저장한다")
    void 허용된_파일_업로드_UUID_이름으로_지정한_디렉터리에_저장한다() throws Exception {
        FileUploadService service = new FileUploadService(policyRepository, tempDirectory.toString());
        MockMultipartFile file = new MockMultipartFile("file", "report.PDF", "application/pdf", "content".getBytes());

        FileUploadService.UploadResponse response = service.upload(file);

        assertThat(response.extension()).isEqualTo("pdf");
        assertThat(response.storedFileName()).matches("[0-9a-f-]{36}\\.pdf");
        assertThat(Files.readString(tempDirectory.resolve(response.storedFileName()))).isEqualTo("content");
        verify(policyRepository).existsByExtensionAndBlockedTrue("pdf");
    }

    @Test
    @DisplayName("빈 파일 업로드_거부한다")
    void 빈_파일_업로드_거부한다() {
        FileUploadService service = new FileUploadService(policyRepository, tempDirectory.toString());
        assertThatThrownBy(() -> service.upload(new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0])))
                .isInstanceOf(ApiException.class).extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("파일 크기가 10MB 경계인 경우_정확히 10MB는 허용하고 초과는 거부한다")
    void 파일_크기가_10MB_경계인_경우_정확히_10MB는_허용하고_초과는_거부한다() {
        FileUploadService service = new FileUploadService(policyRepository, tempDirectory.toString());
        byte[] maximumContents = new byte[(int) FileUploadService.MAX_FILE_SIZE];
        FileUploadService.UploadResponse response = service.upload(
                new MockMultipartFile("file", "maximum.txt", "text/plain", maximumContents));
        assertThat(response.size()).isEqualTo(FileUploadService.MAX_FILE_SIZE);

        byte[] contents = new byte[(int) FileUploadService.MAX_FILE_SIZE + 1];
        assertThatThrownBy(() -> service.upload(new MockMultipartFile("file", "large.txt", "text/plain", contents)))
                .isInstanceOf(ApiException.class).extracting("status").isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @Test
    @DisplayName("확장자가 없거나 dotfile인 경우_거부하고 추가 점이 있으면 마지막 확장자를 사용한다")
    void 확장자가_없거나_dotfile인_경우_거부하고_추가_점이_있으면_마지막_확장자를_사용한다() {
        assertThatThrownBy(() -> FileUploadService.extractExtension("README")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> FileUploadService.extractExtension(".env")).isInstanceOf(ApiException.class);
        assertThat(FileUploadService.extractExtension(".config.JSON ")).isEqualTo("json");
    }

    @Test
    @DisplayName("확장자에 내부 공백이나 특수문자가 있는 경우_거부한다")
    void 확장자에_내부_공백이나_특수문자가_있는_경우_거부한다() {
        assertThatThrownBy(() -> FileUploadService.extractExtension("report.p df"))
                .isInstanceOf(ApiException.class).extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> FileUploadService.extractExtension("report.pdf!"))
                .isInstanceOf(ApiException.class).extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("차단된 확장자 파일 업로드_차단 사유와 함께 거부한다")
    void 차단된_확장자_파일_업로드_차단_사유와_함께_거부한다() {
        when(policyRepository.existsByExtensionAndBlockedTrue("exe")).thenReturn(true);
        FileUploadService service = new FileUploadService(policyRepository, tempDirectory.toString());
        assertThatThrownBy(() -> service.upload(new MockMultipartFile("file", "malware.EXE", "application/octet-stream", "x".getBytes())))
                .isInstanceOf(ApiException.class).hasMessageContaining(".exe");
    }

    @Test
    @DisplayName("파일 저장 경로가 디렉터리가 아닌 경우_서버 오류를 반환한다")
    void 파일_저장_경로가_디렉터리가_아닌_경우_서버_오류를_반환한다() throws Exception {
        Path fileInsteadOfDirectory = Files.createFile(tempDirectory.resolve("not-a-directory"));
        FileUploadService service = new FileUploadService(policyRepository, fileInsteadOfDirectory.toString());

        assertThatThrownBy(() -> service.upload(new MockMultipartFile("file", "report.txt", "text/plain", "x".getBytes())))
                .isInstanceOf(ApiException.class).extracting("status").isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

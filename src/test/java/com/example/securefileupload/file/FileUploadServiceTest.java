package com.example.securefileupload.file;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.securefileupload.common.ApiException;
import com.example.securefileupload.policy.FileExtensionPolicyRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class FileUploadServiceTest {
    @Mock FileExtensionPolicyRepository policyRepository;
    @TempDir Path tempDirectory;

    @Test void savesAllowedFileWithUuidNameOutsideStaticResources() throws Exception {
        FileUploadService service = new FileUploadService(policyRepository, tempDirectory.toString());
        MockMultipartFile file = new MockMultipartFile("file", "report.PDF", "application/pdf", "content".getBytes());

        FileUploadService.UploadResponse response = service.upload(file);

        assertThat(response.extension()).isEqualTo("pdf");
        assertThat(response.storedFileName()).matches("[0-9a-f-]{36}\\.pdf");
        assertThat(Files.readString(tempDirectory.resolve(response.storedFileName()))).isEqualTo("content");
        verify(policyRepository).existsByExtensionAndBlockedTrue("pdf");
    }

    @Test void rejectsEmptyFile() {
        FileUploadService service = new FileUploadService(policyRepository, tempDirectory.toString());
        assertThatThrownBy(() -> service.upload(new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0])))
            .isInstanceOf(ApiException.class).extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test void rejectsFilesLargerThanTenMegabytes() {
        FileUploadService service = new FileUploadService(policyRepository, tempDirectory.toString());
        byte[] contents = new byte[(int) FileUploadService.MAX_FILE_SIZE + 1];
        assertThatThrownBy(() -> service.upload(new MockMultipartFile("file", "large.txt", "text/plain", contents)))
            .isInstanceOf(ApiException.class).extracting("status").isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @Test void rejectsFilesWithoutExtensionAndDotfiles() {
        assertThatThrownBy(() -> FileUploadService.extractExtension("README")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> FileUploadService.extractExtension(".env")).isInstanceOf(ApiException.class);
        assertThat(FileUploadService.extractExtension(".config.JSON")).isEqualTo("json");
    }

    @Test void rejectsBlockedExtension() {
        when(policyRepository.existsByExtensionAndBlockedTrue("exe")).thenReturn(true);
        FileUploadService service = new FileUploadService(policyRepository, tempDirectory.toString());
        assertThatThrownBy(() -> service.upload(new MockMultipartFile("file", "malware.EXE", "application/octet-stream", "x".getBytes())))
            .isInstanceOf(ApiException.class).hasMessageContaining(".exe");
    }

    @Test void returnsServerErrorWhenStorageFails() throws Exception {
        Path fileInsteadOfDirectory = Files.createFile(tempDirectory.resolve("not-a-directory"));
        FileUploadService service = new FileUploadService(policyRepository, fileInsteadOfDirectory.toString());

        assertThatThrownBy(() -> service.upload(new MockMultipartFile("file", "report.txt", "text/plain", "x".getBytes())))
            .isInstanceOf(ApiException.class).extracting("status").isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

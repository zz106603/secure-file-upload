package com.example.securefileupload.file;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.securefileupload.common.ApiException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;

class FileUploadControllerTest {

    @Test
    @DisplayName("파일이 없는 업로드 요청_거부하고 저장 서비스를 호출하지 않는다")
    void 파일이_없는_업로드_요청_거부하고_저장_서비스를_호출하지_않는다() {
        FileUploadService service = org.mockito.Mockito.mock(FileUploadService.class);
        FileUploadController controller = new FileUploadController(service);

        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();

        assertThatThrownBy(() -> controller.upload(request))
                .isInstanceOf(ApiException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("파일이 두 개인 업로드 요청_거부하고 저장 서비스를 호출하지 않는다")
    void 파일이_두_개인_업로드_요청_거부하고_저장_서비스를_호출하지_않는다() {
        FileUploadService service = org.mockito.Mockito.mock(FileUploadService.class);
        FileUploadController controller = new FileUploadController(service);

        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.addFile(new MockMultipartFile(
                "file", "first.txt", "text/plain", "first".getBytes()
        ));
        request.addFile(new MockMultipartFile(
                "another", "second.txt", "text/plain", "second".getBytes()
        ));

        assertThatThrownBy(() -> controller.upload(request))
                .isInstanceOf(ApiException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(service);
    }
}

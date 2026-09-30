package com.example.securefileupload.file;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.securefileupload.common.ApiException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;

class FileUploadControllerTest {

    @Test
    void rejectsMultipleFiles() {
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

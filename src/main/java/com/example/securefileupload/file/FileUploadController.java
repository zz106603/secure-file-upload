package com.example.securefileupload.file;

import com.example.securefileupload.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {
    private final FileUploadService service;

    public FileUploadController(FileUploadService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    FileUploadService.UploadResponse upload(MultipartHttpServletRequest request) {
        // file 필드만 확인하면 다른 이름으로 첨부된 추가 파일을 놓치므로 요청 전체를 센다.
        List<MultipartFile> files = request.getMultiFileMap()
                .values()
                .stream()
                .flatMap(List::stream)
                .toList();

        MultipartFile file = request.getFile("file");

        if (files.size() != 1 || file == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "파일은 한 번에 1개만 업로드할 수 있습니다."
            );
        }

        return service.upload(file);
    }
}

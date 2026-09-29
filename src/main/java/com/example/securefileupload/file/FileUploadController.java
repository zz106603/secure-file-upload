package com.example.securefileupload.file;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {
    private final FileUploadService service;

    public FileUploadController(FileUploadService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    FileUploadService.UploadResponse upload(@RequestParam("file") MultipartFile file) {
        return service.upload(file);
    }
}

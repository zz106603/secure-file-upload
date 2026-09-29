package com.example.securefileupload.common;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, Object>> handleApi(ApiException e) { return response(e.getStatus(), e.getMessage()); }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Map<String, Object>> handleMaxUploadSize(MaxUploadSizeExceededException e) { return response(HttpStatus.PAYLOAD_TOO_LARGE, "파일 크기는 최대 10MB까지 허용됩니다."); }
    @ExceptionHandler(MultipartException.class)
    ResponseEntity<Map<String, Object>> handleMultipart(MultipartException e) { return response(HttpStatus.BAD_REQUEST, "파일 업로드 요청 형식이 올바르지 않습니다."); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> handleUnexpected(Exception e) { return response(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."); }
    private ResponseEntity<Map<String, Object>> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("timestamp", Instant.now().toString(), "status", status.value(), "message", message));
    }
}

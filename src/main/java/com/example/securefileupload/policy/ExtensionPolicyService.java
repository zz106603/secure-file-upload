package com.example.securefileupload.policy;

import com.example.securefileupload.common.ApiException;
import com.example.securefileupload.common.ExtensionFormat;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional(readOnly = true)
public class ExtensionPolicyService {
    private static final Logger log = LoggerFactory.getLogger(ExtensionPolicyService.class);
    private static final int MAX_CUSTOM_COUNT = 200;
    private final FileExtensionPolicyRepository repository;

    public ExtensionPolicyService(FileExtensionPolicyRepository repository) {
        this.repository = repository;
    }

    public List<FileExtensionPolicy> fixedPolicies() {
        return repository.findAllByPolicyTypeOrderByExtensionAsc(ExtensionPolicyType.FIXED);
    }

    public List<FileExtensionPolicy> customPolicies() {
        return repository.findAllByPolicyTypeOrderByExtensionAsc(ExtensionPolicyType.CUSTOM);
    }

    @Transactional
    public FileExtensionPolicy changeFixedBlocked(Long id, boolean blocked) {
        FileExtensionPolicy policy = repository.findByIdAndPolicyType(id, ExtensionPolicyType.FIXED)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "고정 확장자를 찾을 수 없습니다."));
        policy.changeBlocked(blocked);
        log.info("Fixed extension policy changed: extension={}, blocked={}", policy.getExtension(), blocked);
        return policy;
    }

    @Transactional
    public FileExtensionPolicy addCustom(String rawExtension) {
        String extension = normalize(rawExtension);
        if (!ExtensionFormat.isValid(extension))
            throw new ApiException(HttpStatus.BAD_REQUEST, "확장자는 영문 소문자와 숫자로 된 1~20자여야 합니다.");
        if (repository.existsByExtension(extension)) throw new ApiException(HttpStatus.CONFLICT, "이미 등록된 확장자입니다.");
        if (repository.countByPolicyType(ExtensionPolicyType.CUSTOM) >= MAX_CUSTOM_COUNT)
            throw new ApiException(HttpStatus.CONFLICT, "커스텀 확장자는 최대 200개까지 등록할 수 있습니다.");
        // 사전 조회로 사용자 오류를 안내하고, 동시 중복 요청은 DB UNIQUE 제약으로 최종 차단한다.
        try {
            FileExtensionPolicy policy = repository.saveAndFlush(new FileExtensionPolicy(extension, ExtensionPolicyType.CUSTOM, true));
            log.info("Custom extension policy added: extension={}", extension);
            return policy;
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 등록된 확장자입니다.");
        }
    }

    @Transactional
    public void deleteCustom(Long id) {
        FileExtensionPolicy policy = repository.findByIdAndPolicyType(id, ExtensionPolicyType.CUSTOM)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "커스텀 확장자를 찾을 수 없습니다."));
        repository.delete(policy);
        log.info("Custom extension policy deleted: extension={}", policy.getExtension());
    }

    static String normalize(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase(java.util.Locale.ROOT).replaceFirst("^\\.+", "");
    }
}

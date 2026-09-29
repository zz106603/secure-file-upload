package com.example.securefileupload.policy;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileExtensionPolicyRepository extends JpaRepository<FileExtensionPolicy, Long> {
    List<FileExtensionPolicy> findAllByPolicyTypeOrderByExtensionAsc(ExtensionPolicyType policyType);
    boolean existsByExtension(String extension);
    boolean existsByExtensionAndBlockedTrue(String extension);
    long countByPolicyType(ExtensionPolicyType policyType);
    Optional<FileExtensionPolicy> findByIdAndPolicyType(Long id, ExtensionPolicyType policyType);
}

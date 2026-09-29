package com.example.securefileupload.policy;

import jakarta.persistence.*;

@Entity
@Table(name = "file_extension_policy")
public class FileExtensionPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "extension", nullable = false, unique = true, length = 20)
    private String extension;
    @Enumerated(EnumType.STRING)
    @Column(name = "policy_type", nullable = false, length = 10)
    private ExtensionPolicyType policyType;
    @Column(nullable = false)
    private boolean blocked;

    protected FileExtensionPolicy() {
    }

    public FileExtensionPolicy(String extension, ExtensionPolicyType policyType, boolean blocked) {
        this.extension = extension;
        this.policyType = policyType;
        this.blocked = blocked;
    }

    public Long getId() {
        return id;
    }

    public String getExtension() {
        return extension;
    }

    public ExtensionPolicyType getPolicyType() {
        return policyType;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void changeBlocked(boolean blocked) {
        this.blocked = blocked;
    }
}

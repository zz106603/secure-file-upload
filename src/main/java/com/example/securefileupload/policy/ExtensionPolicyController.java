package com.example.securefileupload.policy;

import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/extension-policies")
public class ExtensionPolicyController {
    private final ExtensionPolicyService service;
    public ExtensionPolicyController(ExtensionPolicyService service) { this.service = service; }
    @GetMapping("/fixed") List<PolicyResponse> fixed() { return service.fixedPolicies().stream().map(PolicyResponse::from).toList(); }
    @PatchMapping("/fixed/{id}") PolicyResponse changeFixed(@PathVariable Long id, @RequestBody FixedBlockedRequest request) {
        if (request.blocked() == null) throw new com.example.securefileupload.common.ApiException(HttpStatus.BAD_REQUEST, "blocked 값은 필수입니다.");
        return PolicyResponse.from(service.changeFixedBlocked(id, request.blocked()));
    }
    @GetMapping("/custom") List<PolicyResponse> custom() { return service.customPolicies().stream().map(PolicyResponse::from).toList(); }
    @PostMapping("/custom") ResponseEntity<PolicyResponse> addCustom(@RequestBody CustomExtensionRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(PolicyResponse.from(service.addCustom(request.extension()))); }
    @DeleteMapping("/custom/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void deleteCustom(@PathVariable Long id) { service.deleteCustom(id); }
    record FixedBlockedRequest(Boolean blocked) { }
    record CustomExtensionRequest(String extension) { }
    record PolicyResponse(Long id, String extension, ExtensionPolicyType policyType, boolean blocked) { static PolicyResponse from(FileExtensionPolicy p) { return new PolicyResponse(p.getId(), p.getExtension(), p.getPolicyType(), p.isBlocked()); } }
}

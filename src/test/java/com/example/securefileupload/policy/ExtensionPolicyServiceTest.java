package com.example.securefileupload.policy;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.securefileupload.common.ApiException;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.http.HttpStatus;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class ExtensionPolicyServiceTest {
    @Mock
    FileExtensionPolicyRepository repository;
    @InjectMocks
    ExtensionPolicyService service;

    @Test
    void customExtensionIsNormalizedBeforeSaving() {
        when(repository.existsByExtension("exe2")).thenReturn(false);
        when(repository.countByPolicyType(ExtensionPolicyType.CUSTOM)).thenReturn(0L);
        when(repository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        FileExtensionPolicy saved = service.addCustom("  .ExE2 ");
        assertThat(saved.getExtension()).isEqualTo("exe2");
        assertThat(saved.isBlocked()).isTrue();
    }

    @Test
    void fixedExtensionCannotBeAddedAsCustom() {
        when(repository.existsByExtension("exe")).thenReturn(true);
        assertThatThrownBy(() -> service.addCustom(".EXE"))
                .isInstanceOf(ApiException.class).extracting("status").isEqualTo(HttpStatus.CONFLICT);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void invalidExtensionIsRejected() {
        assertThatThrownBy(() -> service.addCustom("tar.gz"))
                .isInstanceOf(ApiException.class).extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void customLimitIsEnforced() {
        when(repository.existsByExtension("pdf")).thenReturn(false);
        when(repository.countByPolicyType(ExtensionPolicyType.CUSTOM)).thenReturn(200L);
        assertThatThrownBy(() -> service.addCustom("pdf"))
                .isInstanceOf(ApiException.class).extracting("status").isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void onlyFixedPolicyCanChangeBlockedState() {
        FileExtensionPolicy fixed = new FileExtensionPolicy("exe", ExtensionPolicyType.FIXED, false);
        when(repository.findByIdAndPolicyType(1L, ExtensionPolicyType.FIXED)).thenReturn(Optional.of(fixed));
        FileExtensionPolicy changed = service.changeFixedBlocked(1L, true);
        assertThat(changed.isBlocked()).isTrue();
    }
}

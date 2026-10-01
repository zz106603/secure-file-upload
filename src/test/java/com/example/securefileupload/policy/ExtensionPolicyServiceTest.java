package com.example.securefileupload.policy;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.securefileupload.common.ApiException;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("대소문자와 선행 점이 있는 커스텀 확장자_정규화해 차단 상태로 저장한다")
    void 대소문자와_선행점이_있는_커스텀_확장자_정규화해_차단_상태로_저장한다() {
        when(repository.existsByExtension("exe2")).thenReturn(false);
        when(repository.countByPolicyType(ExtensionPolicyType.CUSTOM)).thenReturn(0L);
        when(repository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        FileExtensionPolicy saved = service.addCustom("  .ExE2 ");
        assertThat(saved.getExtension()).isEqualTo("exe2");
        assertThat(saved.isBlocked()).isTrue();
    }

    @Test
    @DisplayName("고정 확장자와 중복된 커스텀 등록_거부한다")
    void 고정_확장자와_중복된_커스텀_등록_거부한다() {
        when(repository.existsByExtension("exe")).thenReturn(true);
        assertThatThrownBy(() -> service.addCustom(".EXE"))
                .isInstanceOf(ApiException.class).extracting("status").isEqualTo(HttpStatus.CONFLICT);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("이미 등록된 커스텀 확장자_정규화 후 중복 등록을 거부한다")
    void 이미_등록된_커스텀_확장자_정규화_후_중복_등록을_거부한다() {
        when(repository.existsByExtension("sh")).thenReturn(true);

        assertThatThrownBy(() -> service.addCustom(" .SH "))
                .isInstanceOf(ApiException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.CONFLICT);

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("여러 점을 포함한 확장자_등록을 거부한다")
    void 여러_점을_포함한_확장자_등록을_거부한다() {
        assertThatThrownBy(() -> service.addCustom("tar.gz"))
                .isInstanceOf(ApiException.class).extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("커스텀 확장자가 200개인 상태_추가 등록을 거부한다")
    void 커스텀_확장자가_200개인_상태_추가_등록을_거부한다() {
        when(repository.existsByExtension("pdf")).thenReturn(false);
        when(repository.countByPolicyType(ExtensionPolicyType.CUSTOM)).thenReturn(200L);
        assertThatThrownBy(() -> service.addCustom("pdf"))
                .isInstanceOf(ApiException.class).extracting("status").isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("고정 확장자 차단 상태 변경_요청한 상태를 반영한다")
    void 고정_확장자_차단_상태_변경_요청한_상태를_반영한다() {
        FileExtensionPolicy fixed = new FileExtensionPolicy("exe", ExtensionPolicyType.FIXED, false);
        when(repository.findByIdAndPolicyType(1L, ExtensionPolicyType.FIXED)).thenReturn(Optional.of(fixed));
        FileExtensionPolicy changed = service.changeFixedBlocked(1L, true);
        assertThat(changed.isBlocked()).isTrue();
    }
}

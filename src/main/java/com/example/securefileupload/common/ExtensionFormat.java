package com.example.securefileupload.common;

import java.util.regex.Pattern;

public final class ExtensionFormat {
    // 정책 등록과 실제 업로드가 같은 확장자 형식 기준을 사용해야 우회가 생기지 않는다.
    private static final Pattern VALID_EXTENSION = Pattern.compile("[a-z0-9]{1,20}");

    private ExtensionFormat() {
    }

    public static boolean isValid(String extension) {
        return VALID_EXTENSION.matcher(extension).matches();
    }
}

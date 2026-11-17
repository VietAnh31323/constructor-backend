package com.backend.constructor.core.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProjectDocumentFolder {
    CONTRACT("Hợp đồng"),
    DRAWING("Bản vẽ"),
    ESTIMATE("Dự toán"),
    MINUTES("Biên bản"),
    SITE_PHOTO("Ảnh hiện trường");

    private final String label;
}

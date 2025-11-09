package com.backend.constructor.core.domain.enums;

import lombok.Getter;

@Getter
public enum AuthScheme {
    BASIC("Basic"),
    BEARER("Bearer"),
    DIGEST("Digest"),
    HOBA("HOBA"),
    MUTUAL("Mutual"),
    AWS4_HMAC_SHA256("AWS4-HMAC-SHA256"),
    OAUTH("OAuth"),
    API_KEY("ApiKey"),
    TOKEN("Token");

    private final String value;

    AuthScheme(String value) {
        this.value = value;
    }

}

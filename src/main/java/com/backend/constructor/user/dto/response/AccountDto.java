package com.backend.constructor.user.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountDto {
    Long userId;
    String username;
    String firstName;
    String lastName;
    String token;
    Instant tokenExpiry;
    String refreshToken;
    Instant refreshTokenExpiry;
    String authScheme;
}

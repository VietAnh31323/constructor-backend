package com.backend.constructor.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserInfoDto {
    @NotNull(message = "Không thể để trống userId")
    Long userId;
    String username;
    @NotBlank(message = "Không thể để trống tên hiển thị")
    String displayName;
    String avatar;
}

package com.backend.constructor.app.dto.progress;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProgressDto {
    private Long id;

    @NotBlank(message = "{ERROR_0001}")
    private String code;

    @NotBlank(message = "{ERROR_0001}")
    private String name;

    private String description;

    private Boolean isActive;

    public void trimData() {
        this.code = StringUtils.trim(this.code);
        this.name = StringUtils.trim(this.name);
        this.description = StringUtils.trim(this.description);
    }
}

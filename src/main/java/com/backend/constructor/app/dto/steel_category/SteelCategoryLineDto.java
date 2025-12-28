package com.backend.constructor.app.dto.steel_category;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SteelCategoryLineDto {
    private Long id;

    @NotNull(message = "{ERROR_0001}")
    private String paramName;

    public void trimData() {
        this.paramName = StringUtils.trim(this.paramName);
    }
}

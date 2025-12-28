package com.backend.constructor.app.dto.steel_category;

import com.backend.constructor.app.dto.upload.UploadDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SteelCategoryDto {
    private Long id;

    private String code;

    @NotBlank(message = "{ERROR_0001}")
    private String name;

    private List<UploadDto> images;

    private String description;

    private Boolean isActive;

    @Valid
    @NotEmpty(message = "{ERROR_0001}")
    private List<SteelCategoryLineDto> steelCategoryLines;

    public void trimData() {
        this.code = StringUtils.trim(this.code);
        this.name = StringUtils.trim(this.name);
        this.description = StringUtils.trim(this.description);
    }
}

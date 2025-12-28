package com.backend.constructor.app.dto.steel_category;

import com.backend.constructor.app.dto.upload.UploadDto;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SteelCategoryOutput {
    private Long id;

    private String code;

    private String name;

    private List<UploadDto> images;

    private String description;

    private Boolean isActive;
}

package com.backend.constructor.app.dto.steel_project;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SteelImportErrorDto {
    private Integer rowNumber;
    private String column;
    private String rawValue;
    private String message;
}

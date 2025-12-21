package com.backend.constructor.app.dto.upload;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UploadDto {
    private String name;
    private String url;
    private String type;
}

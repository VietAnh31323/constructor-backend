package com.backend.constructor.app.dto.progress;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProgressFilterParam {
    private String search;
    private Boolean isActive;
}

package com.backend.constructor.app.dto.category;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryFilterParam {
    private String search;
    private Boolean isActive;
}

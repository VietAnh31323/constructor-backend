package com.backend.constructor.app.dto.steel_category;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SteelCategoryFilterParam {
    private String search;
    private Boolean isActive;
}

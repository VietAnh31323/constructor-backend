package com.backend.constructor.app.dto.project;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectFilterParam {
    private String search;
    private Long categoryId;
}

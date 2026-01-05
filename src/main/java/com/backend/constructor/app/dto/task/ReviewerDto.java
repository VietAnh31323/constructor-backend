package com.backend.constructor.app.dto.task;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReviewerDto {
    private Long id;
    private String avatar;
    private String email;
    private String name;
    private String firstName;
    private String lastName;
}

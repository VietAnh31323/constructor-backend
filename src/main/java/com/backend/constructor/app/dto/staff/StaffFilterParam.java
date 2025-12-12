package com.backend.constructor.app.dto.staff;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StaffFilterParam {
    private String search;
}

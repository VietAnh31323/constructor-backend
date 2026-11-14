package com.backend.constructor.app.dto.staff;

import com.backend.constructor.core.domain.enums.Department;
import com.backend.constructor.core.domain.enums.Position;
import com.backend.constructor.core.domain.enums.StaffStatus;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StaffFilterParam {
    private String search;
    private Position position;
    private Department department;
    private StaffStatus status;
}

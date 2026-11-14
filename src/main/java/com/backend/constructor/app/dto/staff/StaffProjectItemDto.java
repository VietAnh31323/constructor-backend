package com.backend.constructor.app.dto.staff;

import com.backend.constructor.core.domain.enums.ProjectState;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StaffProjectItemDto {
    private Long id;
    private String code;
    private String name;
    private String owner;
    private String address;
    private ProjectState state;
    private BigDecimal progressPercent;
    private LocalDate signDate;
    private LocalDate deliveryDate;
    private boolean overdue;
    private List<String> rolesInProject;
    private int staffTaskCount;
    private int staffActiveTaskCount;
}

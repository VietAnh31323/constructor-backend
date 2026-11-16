package com.backend.constructor.app.dto.project;

import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import com.backend.constructor.core.domain.enums.ProjectMemberRole;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectMemberDto {
    private Long id;

    private Long projectId;

    @NotNull(message = "{ERROR_0001}")
    private Long staffId;

    private CodeNameResponse staff;

    private String staffCode;

    private String staffName;

    private String staffPhone;

    private String staffEmail;

    private String staffDepartment;

    private String staffPosition;

    @NotNull(message = "{ERROR_0001}")
    private ProjectMemberRole memberRole;

    private String memberRoleLabel;

    private Boolean isPrimary;

    private LocalDate joinedDate;

    private String note;
}

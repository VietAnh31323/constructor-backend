package com.backend.constructor.app.dto.task;

import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskStaffMapDto {
    private Long id;
    @Valid
    @NotNull
    private CodeNameResponse staff;
}

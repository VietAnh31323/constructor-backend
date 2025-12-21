package com.backend.constructor.app.dto.staff;

import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AssignStaffDto {
    @NotNull
    private Long accountId;
    @NotNull
    @Valid
    private CodeNameResponse staff;
}

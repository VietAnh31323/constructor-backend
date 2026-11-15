package com.backend.constructor.app.dto.customer;

import com.backend.constructor.core.domain.enums.CustomerPipelineStage;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPipelineStageUpdateDto {
    @NotNull(message = "{ERROR_0001}")
    private CustomerPipelineStage pipelineStage;

    private String note;
}

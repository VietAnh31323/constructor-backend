package com.backend.constructor.app.dto.customer;

import com.backend.constructor.core.domain.enums.ContactStatus;
import com.backend.constructor.core.domain.enums.CustomerPipelineStage;
import com.backend.constructor.core.domain.enums.CustomerType;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerFilterParam {
    private String search;
    private CustomerType customerType;
    private CustomerPipelineStage pipelineStage;
    private ContactStatus contactStatus;
    private Long assignedStaffId;
    private Boolean isPotential;
}

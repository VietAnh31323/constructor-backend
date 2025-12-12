package com.backend.constructor.app.dto.customer;

import com.backend.constructor.core.domain.enums.ContactStatus;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerFilterParam {
    private String search;
    private ContactStatus contactStatus;
    private Boolean isPotential;
}

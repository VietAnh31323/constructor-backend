package com.backend.constructor.app.dto.customer;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PublicCustomerLeadResponseDto {
    private Long id;
    private String code;
    private String name;
    private boolean existingCustomer;
}

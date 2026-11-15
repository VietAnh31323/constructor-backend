package com.backend.constructor.app.api;

import com.backend.constructor.app.dto.customer.PublicCustomerLeadRequestDto;
import com.backend.constructor.app.dto.customer.PublicCustomerLeadResponseDto;

public interface PublicCustomerLeadApi {
    PublicCustomerLeadResponseDto submitPublicLead(PublicCustomerLeadRequestDto input);
}

package com.backend.constructor.app.rest;

import com.backend.constructor.app.api.PublicCustomerLeadApi;
import com.backend.constructor.app.dto.customer.PublicCustomerLeadRequestDto;
import com.backend.constructor.app.dto.customer.PublicCustomerLeadResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/customer-lead")
@RequiredArgsConstructor
public class PublicCustomerLeadController implements PublicCustomerLeadApi {
    private final PublicCustomerLeadApi customerService;

    @Override
    @PostMapping
    public PublicCustomerLeadResponseDto submitPublicLead(@RequestBody @Valid PublicCustomerLeadRequestDto input) {
        return customerService.submitPublicLead(input);
    }
}

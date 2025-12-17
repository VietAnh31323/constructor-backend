package com.backend.constructor.app.rest;

import com.backend.constructor.app.api.CustomerApi;
import com.backend.constructor.app.dto.customer.CustomerDto;
import com.backend.constructor.app.dto.customer.CustomerFilterParam;
import com.backend.constructor.common.base.dto.response.IdResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/customer")
@RequiredArgsConstructor
public class CustomerController implements CustomerApi {
    private final CustomerApi customerService;

    @Override
    @PostMapping
    public IdResponse create(@RequestBody @Valid CustomerDto input) {
        return customerService.create(input);
    }

    @Override
    @PutMapping
    public IdResponse update(@RequestBody @Valid CustomerDto input) {
        return customerService.update(input);
    }

    @Override
    @DeleteMapping
    public IdResponse delete(@RequestParam Long id) {
        return customerService.delete(id);
    }

    @Override
    @GetMapping
    public CustomerDto getDetail(@RequestParam Long id) {
        return customerService.getDetail(id);
    }

    @Override
    @GetMapping("/list")
    public Page<CustomerDto> getListStaff(@ParameterObject CustomerFilterParam param,
                                          @ParameterObject Pageable pageable) {
        return customerService.getListStaff(param, pageable);
    }
}

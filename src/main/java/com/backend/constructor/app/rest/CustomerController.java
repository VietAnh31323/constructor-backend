package com.backend.constructor.app.rest;

import com.backend.constructor.app.api.CustomerApi;
import com.backend.constructor.app.dto.customer.*;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.common.base.response.paging.HandsomePaging;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    @HandsomePaging
    public Page<CustomerDto> getListStaff(@ParameterObject CustomerFilterParam param,
                                          @ParameterObject Pageable pageable) {
        return customerService.getListStaff(param, pageable);
    }

    @Override
    @GetMapping("/pipeline-summary")
    public CustomerPipelineSummaryDto getPipelineSummary() {
        return customerService.getPipelineSummary();
    }

    @Override
    @PatchMapping("/{customerId}/pipeline-stage")
    public IdResponse updatePipelineStage(@PathVariable Long customerId,
                                          @RequestBody @Valid CustomerPipelineStageUpdateDto input) {
        return customerService.updatePipelineStage(customerId, input);
    }

    @Override
    @GetMapping("/{customerId}/care-logs")
    public List<CustomerCareLogDto> getCareLogs(@PathVariable Long customerId) {
        return customerService.getCareLogs(customerId);
    }

    @Override
    @PostMapping("/{customerId}/care-logs")
    public IdResponse addCareLog(@PathVariable Long customerId,
                                 @RequestBody @Valid CustomerCareLogDto input) {
        return customerService.addCareLog(customerId, input);
    }

    @Override
    @PutMapping("/{customerId}/care-logs/{logId}")
    public IdResponse updateCareLog(@PathVariable Long customerId,
                                    @PathVariable Long logId,
                                    @RequestBody @Valid CustomerCareLogDto input) {
        return customerService.updateCareLog(customerId, logId, input);
    }

    @Override
    @DeleteMapping("/{customerId}/care-logs/{logId}")
    public IdResponse deleteCareLog(@PathVariable Long customerId,
                                    @PathVariable Long logId) {
        return customerService.deleteCareLog(customerId, logId);
    }
}

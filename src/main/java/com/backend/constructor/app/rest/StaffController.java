package com.backend.constructor.app.rest;

import com.backend.constructor.app.api.StaffApi;
import com.backend.constructor.app.dto.staff.StaffDto;
import com.backend.constructor.app.dto.staff.StaffFilterParam;
import com.backend.constructor.app.dto.staff.StaffOutput;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.common.base.response.paging.HandsomePaging;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
public class StaffController implements StaffApi {
    private final StaffApi staffService;

    @Override
    @PostMapping
    public IdResponse create(@RequestBody @Valid StaffDto input) {
        return staffService.create(input);
    }

    @Override
    @PutMapping
    public IdResponse update(@RequestBody @Valid StaffDto input) {
        return staffService.update(input);
    }

    @Override
    @DeleteMapping
    public IdResponse delete(@RequestParam Long id) {
        return staffService.delete(id);
    }

    @Override
    @GetMapping
    public StaffDto getDetail(@RequestParam Long id) {
        return staffService.getDetail(id);
    }

    @Override
    @GetMapping("/list")
    @HandsomePaging
    public Page<StaffOutput> getListStaff(@ParameterObject StaffFilterParam param,
                                          @ParameterObject Pageable pageable) {
        return staffService.getListStaff(param, pageable);
    }
}

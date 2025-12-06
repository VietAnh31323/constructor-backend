package com.backend.constructor.app.rest;

import com.backend.constructor.app.api.ProgressApi;
import com.backend.constructor.app.dto.progress.ProgressDto;
import com.backend.constructor.app.dto.progress.ProgressFilterParam;
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
@RequestMapping("/api/v1/progress")
@RequiredArgsConstructor
public class ProgressController implements ProgressApi {
    private final ProgressApi progressService;

    @Override
    @PostMapping
    public IdResponse create(@RequestBody @Valid ProgressDto input) {
        return progressService.create(input);
    }

    @Override
    @PutMapping
    public IdResponse update(@RequestBody @Valid ProgressDto input) {
        return progressService.update(input);
    }

    @Override
    @DeleteMapping
    public IdResponse delete(@RequestParam Long id) {
        return progressService.delete(id);
    }

    @Override
    @GetMapping
    public ProgressDto getDetail(@RequestParam Long id) {
        return progressService.getDetail(id);
    }

    @Override
    @GetMapping("/list")
    public Page<ProgressDto> getListStaff(@ParameterObject ProgressFilterParam param,
                                          @ParameterObject Pageable pageable) {
        return progressService.getListStaff(param, pageable);
    }
}

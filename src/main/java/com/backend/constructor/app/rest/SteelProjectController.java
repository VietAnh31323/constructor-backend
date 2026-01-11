package com.backend.constructor.app.rest;

import com.backend.constructor.app.api.SteelProjectApi;
import com.backend.constructor.app.dto.steel_project.SteelProjectDto;
import com.backend.constructor.app.dto.steel_project.SteelProjectOutput;
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
@RequestMapping("/api/v1/steel-project")
@RequiredArgsConstructor
public class SteelProjectController implements SteelProjectApi {
    private final SteelProjectApi steelProjectService;

    @Override
    @PostMapping
    public IdResponse create(@RequestBody @Valid SteelProjectDto input) {
        return steelProjectService.create(input);
    }

    @Override
    @PutMapping
    public IdResponse update(@RequestBody @Valid SteelProjectDto input) {
        return steelProjectService.update(input);
    }

    @Override
    @GetMapping
    public SteelProjectDto getDetail(@RequestParam Long id) {
        return steelProjectService.getDetail(id);
    }

    @Override
    @HandsomePaging
    @GetMapping("/list")
    public Page<SteelProjectOutput> getListSteelProject(@RequestParam(required = false) String search,
                                                        @ParameterObject Pageable pageable) {
        return steelProjectService.getListSteelProject(search, pageable);
    }
}

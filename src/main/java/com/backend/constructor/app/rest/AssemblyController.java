package com.backend.constructor.app.rest;

import com.backend.constructor.app.api.AssemblyApi;
import com.backend.constructor.app.dto.assembly.AssemblyDto;
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
@RequestMapping("/api/v1/assembly")
@RequiredArgsConstructor
public class AssemblyController implements AssemblyApi {
    private final AssemblyApi assemblyService;

    @Override
    @PostMapping
    public IdResponse create(@RequestBody @Valid AssemblyDto input) {
        return assemblyService.create(input);
    }

    @Override
    @GetMapping("/list")
    @HandsomePaging
    public Page<AssemblyDto> getListAssembly(@RequestParam(required = false) String search,
                                             @ParameterObject Pageable pageable) {
        return assemblyService.getListAssembly(search, pageable);
    }
}

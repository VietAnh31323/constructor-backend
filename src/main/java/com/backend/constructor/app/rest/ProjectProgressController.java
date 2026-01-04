package com.backend.constructor.app.rest;

import com.backend.constructor.app.api.ProjectProgressApi;
import com.backend.constructor.app.dto.project_progress.ProjectInfoDto;
import com.backend.constructor.common.base.dto.response.IdResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/project-progress")
@RequiredArgsConstructor
public class ProjectProgressController implements ProjectProgressApi {
    private final ProjectProgressApi projectProgressService;

    @Operation(summary = "Thêm mới tiến độ dự án")
    @Override
    @PostMapping
    public IdResponse create(@RequestBody @Valid ProjectInfoDto input) {
        return projectProgressService.create(input);
    }

    @Override
    @GetMapping
    public ProjectInfoDto getDetail(@RequestParam Long id) {
        return projectProgressService.getDetail(id);
    }
}

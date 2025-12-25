package com.backend.constructor.app.rest;

import com.backend.constructor.app.api.ProjectApi;
import com.backend.constructor.app.dto.project.ProjectDto;
import com.backend.constructor.app.dto.project.ProjectFilterParam;
import com.backend.constructor.app.dto.project.ProjectOutput;
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
@RequestMapping("/api/v1/project")
@RequiredArgsConstructor
public class ProjectController implements ProjectApi {
    private final ProjectApi projectService;

    @Override
    @PostMapping
    public IdResponse create(@RequestBody @Valid ProjectDto input) {
        return projectService.create(input);
    }

    @Override
    @PutMapping
    public IdResponse update(@RequestBody @Valid ProjectDto input) {
        return projectService.update(input);
    }

    @Override
    @DeleteMapping
    public void delete(@RequestParam Long id) {
        projectService.delete(id);
    }

    @Override
    @GetMapping
    public ProjectDto getDetail(@RequestParam Long id) {
        return projectService.getDetail(id);
    }

    @Override
    @GetMapping("/list")
    public Page<ProjectOutput> getListProject(@ParameterObject ProjectFilterParam param,
                                              @ParameterObject Pageable pageable) {
        return projectService.getListProject(param, pageable);
    }
}

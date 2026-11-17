package com.backend.constructor.app.rest;

import com.backend.constructor.app.api.ProcessTemplateApi;
import com.backend.constructor.app.dto.process_template.ProcessTemplateDto;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.core.domain.enums.BuildingType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/process-template")
@RequiredArgsConstructor
public class ProcessTemplateController implements ProcessTemplateApi {
    private final ProcessTemplateApi processTemplateService;

    @Override
    @GetMapping("/list")
    public List<ProcessTemplateDto> getList(@RequestParam(required = false) BuildingType buildingType) {
        return processTemplateService.getList(buildingType);
    }

    @Override
    @GetMapping("/{id}")
    public ProcessTemplateDto getDetail(@PathVariable Long id) {
        return processTemplateService.getDetail(id);
    }

    @Override
    @PostMapping
    public IdResponse create(@RequestBody @Valid ProcessTemplateDto input) {
        return processTemplateService.create(input);
    }

    @Override
    @PutMapping("/{id}")
    public IdResponse update(@PathVariable Long id, @RequestBody @Valid ProcessTemplateDto input) {
        return processTemplateService.update(id, input);
    }

    @Override
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        processTemplateService.delete(id);
    }

    @Override
    @PostMapping("/apply/{projectId}")
    public void applyToProject(@PathVariable Long projectId,
                               @RequestParam(required = false) Long templateId) {
        processTemplateService.applyToProject(projectId, templateId);
    }
}

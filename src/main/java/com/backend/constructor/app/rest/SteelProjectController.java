package com.backend.constructor.app.rest;

import com.backend.constructor.app.api.SteelProjectApi;
import com.backend.constructor.app.dto.steel_project.SteelImportResultDto;
import com.backend.constructor.app.dto.steel_project.SteelProjectDto;
import com.backend.constructor.app.dto.steel_project.SteelProjectOutput;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.common.base.response.paging.HandsomePaging;
import com.backend.constructor.core.service.SteelExcelPdfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/v1/steel-project")
@RequiredArgsConstructor
public class SteelProjectController implements SteelProjectApi {
    private final SteelProjectApi steelProjectService;
    private final SteelExcelPdfService steelExcelPdfService;

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
                                                        @RequestParam(required = false) Long projectId,
                                                        @ParameterObject Pageable pageable) {
        return steelProjectService.getListSteelProject(search, projectId, pageable);
    }

    @Override
    @DeleteMapping
    public void delete(@RequestParam Long id) {
        steelProjectService.delete(id);
    }

    @GetMapping("/{id}/export-excel")
    @PreAuthorize("hasAnyAuthority('MATERIAL_VIEW', 'PROJECT_VIEW', 'SCOPE_ADMIN')")
    public ResponseEntity<byte[]> exportExcel(@PathVariable Long id) {
        byte[] bytes = steelExcelPdfService.exportExcel(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"thong-ke-thep-" + id + ".xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping("/{id}/export-pdf")
    @PreAuthorize("hasAnyAuthority('MATERIAL_VIEW', 'PROJECT_VIEW', 'SCOPE_ADMIN')")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) {
        byte[] bytes = steelExcelPdfService.exportPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"thong-ke-thep-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @GetMapping("/import-template")
    @PreAuthorize("hasAnyAuthority('MATERIAL_VIEW', 'MATERIAL_MANAGE', 'PROJECT_VIEW', 'PROJECT_UPDATE', 'SCOPE_ADMIN')")
    public ResponseEntity<byte[]> downloadImportTemplate() {
        byte[] bytes = steelExcelPdfService.exportImportTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mau-nhap-thong-ke-thep.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @PostMapping(value = "/{id}/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('MATERIAL_MANAGE', 'PROJECT_UPDATE', 'SCOPE_ADMIN')")
    public SteelImportResultDto importExcel(@PathVariable Long id,
                                            @RequestParam("file") MultipartFile file,
                                            @RequestParam(defaultValue = "false") boolean replaceExisting) {
        return steelExcelPdfService.importExcel(id, file, replaceExisting);
    }
}

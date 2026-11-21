package com.backend.constructor.app.rest;

import com.backend.constructor.app.dto.dashboard.DashboardOverviewDto;
import com.backend.constructor.app.dto.dashboard.ProjectReportDto;
import com.backend.constructor.core.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    @PreAuthorize("isAuthenticated()")
    public DashboardOverviewDto getOverview() {
        return dashboardService.getOverview();
    }

    @GetMapping("/project-report")
    @PreAuthorize("isAuthenticated()")
    public ProjectReportDto.SummaryResponse getProjectReports(@RequestParam(required = false) Long projectId) {
        return dashboardService.getProjectReports(projectId);
    }

    @GetMapping("/project-report/export-excel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportProjectReportExcel(@RequestParam(required = false) Long projectId) {
        byte[] bytes = dashboardService.exportProjectReportExcel(projectId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bao-cao-du-an.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}

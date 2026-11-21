package com.backend.constructor.app.rest;

import com.backend.constructor.core.domain.enums.ProjectState;
import com.backend.constructor.core.service.ListExcelExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/export")
@RequiredArgsConstructor
public class ExportController {

    private final ListExcelExportService listExcelExportService;

    private ResponseEntity<byte[]> xlsxResponse(byte[] bytes, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping("/projects")
    @PreAuthorize("hasAnyAuthority('PROJECT_VIEW', 'SCOPE_ADMIN')")
    public ResponseEntity<byte[]> exportProjects(@RequestParam(required = false) String search,
                                                 @RequestParam(required = false) ProjectState state) {
        return xlsxResponse(listExcelExportService.exportProjects(search, state), "danh-sach-du-an.xlsx");
    }

    @GetMapping("/customers")
    @PreAuthorize("hasAnyAuthority('CUSTOMER_VIEW', 'SCOPE_ADMIN')")
    public ResponseEntity<byte[]> exportCustomers(@RequestParam(required = false) String search) {
        return xlsxResponse(listExcelExportService.exportCustomers(search), "danh-sach-khach-hang.xlsx");
    }

    @GetMapping("/staffs")
    @PreAuthorize("hasAnyAuthority('USER_VIEW', 'SCOPE_ADMIN')")
    public ResponseEntity<byte[]> exportStaffs(@RequestParam(required = false) String search) {
        return xlsxResponse(listExcelExportService.exportStaffs(search), "danh-sach-nhan-su.xlsx");
    }

    @GetMapping("/accounts")
    @PreAuthorize("hasAnyAuthority('USER_VIEW', 'USER_MANAGE', 'SCOPE_ADMIN')")
    public ResponseEntity<byte[]> exportAccounts(@RequestParam(required = false) String search) {
        return xlsxResponse(listExcelExportService.exportAccounts(search), "danh-sach-tai-khoan.xlsx");
    }

    @GetMapping("/categories")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportCategories(@RequestParam(required = false) String search) {
        return xlsxResponse(listExcelExportService.exportCategories(search), "danh-muc-hang-muc.xlsx");
    }

    @GetMapping("/steel-projects")
    @PreAuthorize("hasAnyAuthority('MATERIAL_VIEW', 'PROJECT_VIEW', 'SCOPE_ADMIN')")
    public ResponseEntity<byte[]> exportSteelProjects(@RequestParam(required = false) String search,
                                                      @RequestParam(required = false) Long projectId) {
        return xlsxResponse(listExcelExportService.exportSteelProjects(search, projectId), "danh-sach-thong-ke-thep.xlsx");
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasAnyAuthority('AUDIT_LOG_VIEW', 'SCOPE_ADMIN')")
    public ResponseEntity<byte[]> exportAuditLogs(@RequestParam(required = false) String search) {
        return xlsxResponse(listExcelExportService.exportAuditLogs(search), "nhat-ky-he-thong.xlsx");
    }
}

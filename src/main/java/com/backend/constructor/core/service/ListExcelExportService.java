package com.backend.constructor.core.service;

import com.backend.constructor.app.dto.account.AccountStaffDto;
import com.backend.constructor.app.dto.audit_log.AuditLogDto;
import com.backend.constructor.app.dto.audit_log.AuditLogFilterParam;
import com.backend.constructor.app.dto.category.CategoryDto;
import com.backend.constructor.app.dto.category.CategoryFilterParam;
import com.backend.constructor.app.dto.customer.CustomerDto;
import com.backend.constructor.app.dto.customer.CustomerFilterParam;
import com.backend.constructor.app.dto.project.ProjectFilterParam;
import com.backend.constructor.app.dto.project.ProjectOutput;
import com.backend.constructor.app.dto.staff.StaffFilterParam;
import com.backend.constructor.app.dto.staff.StaffOutput;
import com.backend.constructor.app.dto.steel_project.SteelProjectOutput;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.core.domain.enums.ProjectState;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListExcelExportService {

    private final ProjectService projectService;
    private final CustomerService customerService;
    private final StaffService staffService;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final SteelProjectService steelProjectService;
    private final AuditLogService auditLogService;

    private static final Pageable EXPORT_PAGE = PageRequest.of(0, 5000);

    public byte[] exportProjects(String search, ProjectState state) {
        ProjectFilterParam param = ProjectFilterParam.builder().search(search).state(state).build();
        List<ProjectOutput> items = projectService.getListProject(param, EXPORT_PAGE).getContent();
        String[] headers = {"Mã dự án", "Tên dự án", "Chủ đầu tư", "Trạng thái", "Tiến độ (%)", "Giá trị HĐ (VNĐ)", "Ngày ký", "Ngày giao dự kiến"};
        List<List<Object>> rows = new ArrayList<>();
        for (ProjectOutput p : items) {
            rows.add(List.of(
                    str(p.getCode()),
                    str(p.getName()),
                    str(p.getOwner()),
                    p.getState() != null ? p.getState().getLabel() : "",
                    num(p.getProgressPercent()),
                    num(p.getContractValue()),
                    str(p.getSignDate()),
                    str(p.getDeliveryDate())
            ));
        }
        return writeWorkbook("Danh sach du an", headers, rows);
    }

    public byte[] exportCustomers(String search) {
        CustomerFilterParam param = new CustomerFilterParam();
        param.setSearch(search);
        List<CustomerDto> items = customerService.getListStaff(param, EXPORT_PAGE).getContent();
        String[] headers = {"Mã KH", "Tên khách hàng", "Loại KH", "Số điện thoại", "Email", "Địa chỉ", "Số dự án", "Tổng giá trị HĐ"};
        List<List<Object>> rows = new ArrayList<>();
        for (CustomerDto c : items) {
            rows.add(List.of(
                    str(c.getCode()),
                    str(c.getName()),
                    c.getCustomerType() != null ? c.getCustomerType().getLabel() : "",
                    str(c.getPhone()),
                    str(c.getEmail()),
                    str(c.getAddress()),
                    c.getProjectCount() != null ? c.getProjectCount() : 0,
                    num(c.getTotalContractValue())
            ));
        }
        return writeWorkbook("Danh sach khach hang", headers, rows);
    }

    public byte[] exportStaffs(String search) {
        StaffFilterParam param = StaffFilterParam.builder().search(search).build();
        List<StaffOutput> items = staffService.getListStaff(param, EXPORT_PAGE).getContent();
        String[] headers = {"Mã NV", "Họ và tên", "Chức danh", "Phòng ban", "Số điện thoại", "Email", "Tài khoản"};
        List<List<Object>> rows = new ArrayList<>();
        for (StaffOutput s : items) {
            rows.add(List.of(
                    str(s.getCode()),
                    str(s.getName()),
                    s.getPosition() != null ? s.getPosition().name() : "",
                    str(s.getDepartment()),
                    str(s.getPhone()),
                    str(s.getEmail()),
                    str(s.getAccountUsername())
            ));
        }
        return writeWorkbook("Danh sach nhan su", headers, rows);
    }

    public byte[] exportAccounts(String search) {
        List<AccountStaffDto> items = accountService.getPageAccount(search, null, EXPORT_PAGE).getContent();
        String[] headers = {"Tên đăng nhập", "Họ tên", "Nhân sự liên kết", "Trạng thái", "Vai trò", "Ngày tạo"};
        List<List<Object>> rows = new ArrayList<>();
        for (AccountStaffDto a : items) {
            String fullName = (str(a.getFirstName()) + " " + str(a.getLastName())).trim();
            String roles = a.getRoles() != null ? String.join(", ", a.getRoles().stream().map(r -> str(r.getName())).toList()) : "";
            rows.add(List.of(
                    str(a.getUsername()),
                    fullName,
                    a.getStaff() != null ? str(a.getStaff().getName()) : "",
                    a.getStatus() != null ? a.getStatus().name() : "",
                    roles,
                    str(a.getCreatedAt())
            ));
        }
        return writeWorkbook("Danh sach tai khoan", headers, rows);
    }

    public byte[] exportCategories(String search) {
        CategoryFilterParam param = new CategoryFilterParam();
        param.setSearch(search);
        List<CategoryDto> items = categoryService.getListCategory(param, EXPORT_PAGE).getContent();
        String[] headers = {"Mã hạng mục", "Tên hạng mục", "Hạng mục cha", "Đơn vị", "Đơn giá mặc định", "Mô tả"};
        List<List<Object>> rows = new ArrayList<>();
        for (CategoryDto c : items) {
            rows.add(List.of(
                    str(c.getCode()),
                    str(c.getName()),
                    str(c.getParentName()),
                    str(c.getUnit()),
                    num(c.getDefaultUnitPrice()),
                    str(c.getDescription())
            ));
        }
        return writeWorkbook("Danh muc hang muc", headers, rows);
    }

    public byte[] exportSteelProjects(String search, Long projectId) {
        List<SteelProjectOutput> items = steelProjectService.getListSteelProject(search, projectId, EXPORT_PAGE).getContent();
        String[] headers = {"Mã bảng thép", "Tên công trình", "Dự án chính", "Chủ đầu tư", "Số cấu kiện", "Tổng dài (m)", "Tổng khối lượng (kg)", "Ngày ký"};
        List<List<Object>> rows = new ArrayList<>();
        for (SteelProjectOutput sp : items) {
            rows.add(List.of(
                    str(sp.getCode()),
                    str(sp.getName()),
                    sp.getProjectCode() != null ? sp.getProjectCode() + " - " + str(sp.getProjectName()) : "",
                    str(sp.getOwner()),
                    sp.getAssemblyCount() != null ? sp.getAssemblyCount() : 0,
                    num(sp.getTotalLength()),
                    num(sp.getTotalWeight()),
                    str(sp.getSignDate())
            ));
        }
        return writeWorkbook("Danh sach thong ke thep", headers, rows);
    }

    public byte[] exportAuditLogs(String search) {
        AuditLogFilterParam param = new AuditLogFilterParam();
        param.setSearch(search);
        List<AuditLogDto> items = auditLogService.getListAuditLog(param, EXPORT_PAGE).getContent();
        String[] headers = {"Thời gian", "Người thực hiện", "Hành động", "Phân hệ", "Đối tượng", "Mô tả"};
        List<List<Object>> rows = new ArrayList<>();
        for (AuditLogDto log : items) {
            rows.add(List.of(
                    str(log.getCreatedAt()),
                    str(log.getUsername()),
                    str(log.getAction()),
                    str(log.getModule()),
                    str(log.getEntityName()),
                    str(log.getDescription())
            ));
        }
        return writeWorkbook("Nhat ky he thong", headers, rows);
    }

    private byte[] writeWorkbook(String sheetName, String[] headers, List<List<Object>> rows) {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(sheetName);
            CellStyle headerStyle = wb.createCellStyle();
            Font hf = wb.createFont();
            hf.setBold(true);
            hf.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(hf);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle numStyle = wb.createCellStyle();
            numStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0.##"));

            Row h = sheet.createRow(0);
            h.setHeightInPoints(24);
            for (int i = 0; i < headers.length; i++) {
                Cell c = h.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }

            int rIdx = 1;
            for (List<Object> rowVals : rows) {
                Row r = sheet.createRow(rIdx++);
                for (int cIdx = 0; cIdx < rowVals.size(); cIdx++) {
                    Object v = rowVals.get(cIdx);
                    Cell cell = r.createCell(cIdx);
                    if (v instanceof Number n) {
                        cell.setCellValue(n.doubleValue());
                        cell.setCellStyle(numStyle);
                    } else {
                        cell.setCellValue(Objects.toString(v, ""));
                    }
                }
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw BusinessException.exception("EXP001", "Không thể xuất file Excel: " + e.getMessage());
        }
    }

    private String str(Object o) {
        return o != null ? String.valueOf(o) : "";
    }

    private BigDecimal num(BigDecimal b) {
        return b != null ? b : BigDecimal.ZERO;
    }
}

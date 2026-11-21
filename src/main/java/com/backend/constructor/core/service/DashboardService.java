package com.backend.constructor.core.service;

import com.backend.constructor.app.dto.dashboard.DashboardOverviewDto;
import com.backend.constructor.app.dto.dashboard.DashboardOverviewDto.*;
import com.backend.constructor.app.dto.dashboard.ProjectReportDto;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.core.domain.entity.*;
import com.backend.constructor.core.domain.enums.*;
import com.backend.constructor.infras.repository.jpa.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final ProjectJpaRepository projectJpaRepository;
    private final ProjectPaymentJpaRepository projectPaymentJpaRepository;
    private final ProjectLineJpaRepository projectLineJpaRepository;
    private final ProjectCategoryMapJpaRepository projectCategoryMapJpaRepository;
    private final ProjectProgressJpaRepository projectProgressJpaRepository;
    private final TaskJpaRepository taskJpaRepository;
    private final TaskStaffMapJpaRepository taskStaffMapJpaRepository;
    private final SteelProjectJpaRepository steelProjectJpaRepository;
    private final SteelProjectAssemblyMapJpaRepository steelProjectAssemblyMapJpaRepository;
    private final CurrentUserService currentUserService;
    private final ProgressCalculationService progressCalculationService;

    private static final DateTimeFormatter YM_FMT = DateTimeFormatter.ofPattern("MM/yyyy");

    public DashboardOverviewDto getOverview() {
        LocalDate today = LocalDate.now();
        YearMonth currentYm = YearMonth.from(today);

        List<ProjectEntity> projects = projectJpaRepository.findAll();
        Map<Long, ProjectEntity> projectMap = projects.stream()
                .collect(Collectors.toMap(ProjectEntity::getId, Function.identity()));

        Map<ProjectState, Long> stateCounts = new EnumMap<>(ProjectState.class);
        for (ProjectState s : ProjectState.values()) {
            stateCounts.put(s, 0L);
        }

        long activeProjects = 0L;
        long overdueProjects = 0L;
        BigDecimal totalContractValue = BigDecimal.ZERO;
        List<DashboardAlertDto> alerts = new ArrayList<>();

        for (ProjectEntity p : projects) {
            ProjectState state = p.getState() != null ? p.getState() : ProjectState.RECEIVED;
            stateCounts.merge(state, 1L, Long::sum);
            if (state.isActiveWorkload()) {
                activeProjects++;
            }
            if (p.getContractValue() != null) {
                totalContractValue = totalContractValue.add(p.getContractValue());
            }
            if (p.getDeliveryDate() != null && p.getDeliveryDate().isBefore(today) && !state.isCompletedOrClosed()) {
                overdueProjects++;
                alerts.add(DashboardAlertDto.builder()
                        .alertType("OVERDUE_PROJECT")
                        .severity("ERROR")
                        .title("Dự án trễ hạn: " + p.getName())
                        .subtitle("Mã " + p.getCode() + " • Hạn bàn giao: " + p.getDeliveryDate())
                        .dueDate(p.getDeliveryDate())
                        .link("/Constructor/Project/" + p.getId())
                        .build());
            }
        }

        List<StateCountDto> projectsByState = Arrays.stream(ProjectState.values())
                .map(s -> StateCountDto.builder()
                        .state(s.name())
                        .label(s.getLabel())
                        .count(stateCounts.getOrDefault(s, 0L))
                        .build())
                .filter(sc -> sc.getCount() > 0 || ProjectState.valueOf(sc.getState()).isMainFlow())
                .toList();

        // Công nợ & dòng tiền
        List<ProjectPaymentEntity> payments = projectPaymentJpaRepository.findAll();
        BigDecimal totalCollected = BigDecimal.ZERO;
        BigDecimal collectedThisMonth = BigDecimal.ZERO;
        Map<YearMonth, BigDecimal> collectedByMonth = new HashMap<>();

        for (ProjectPaymentEntity pay : payments) {
            if (pay.getStatus() != ProjectPaymentStatus.CONFIRMED || pay.getAmount() == null) {
                continue;
            }
            totalCollected = totalCollected.add(pay.getAmount());
            if (pay.getPaymentDate() != null) {
                YearMonth ym = YearMonth.from(pay.getPaymentDate());
                collectedByMonth.merge(ym, pay.getAmount(), BigDecimal::add);
                if (ym.equals(currentYm)) {
                    collectedThisMonth = collectedThisMonth.add(pay.getAmount());
                }
            }
        }

        BigDecimal receivableTotal = totalContractValue.subtract(totalCollected);
        if (receivableTotal.compareTo(BigDecimal.ZERO) < 0) {
            receivableTotal = BigDecimal.ZERO;
        }

        List<ProjectLineEntity> paymentPlans = projectLineJpaRepository.findAll();
        Map<YearMonth, BigDecimal> plannedByMonth = new HashMap<>();
        for (ProjectLineEntity line : paymentPlans) {
            if (line.getPaymentDate() != null && line.getPaymentAmount() != null) {
                plannedByMonth.merge(YearMonth.from(line.getPaymentDate()), line.getPaymentAmount(), BigDecimal::add);
            }
        }

        List<MonthlyCashflowDto> monthlyCashflow = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = currentYm.minusMonths(i);
            monthlyCashflow.add(MonthlyCashflowDto.builder()
                    .month(ym.format(YM_FMT))
                    .collectedAmount(collectedByMonth.getOrDefault(ym, BigDecimal.ZERO))
                    .plannedAmount(plannedByMonth.getOrDefault(ym, BigDecimal.ZERO))
                    .build());
        }

        // Công việc trễ hạn, chờ duyệt & công việc của tôi
        List<TaskEntity> allTasks = taskJpaRepository.findAll();
        long overdueTasks = 0L;
        long pendingApprovalTasks = 0L;

        for (TaskEntity t : allTasks) {
            if (t.getState() == TaskState.PENDING_REVIEW) {
                pendingApprovalTasks++;
            }
            if (t.getEndDate() != null && t.getEndDate().isBefore(today) && t.getState() != TaskState.COMPLETED) {
                overdueTasks++;
                if (alerts.size() < 25) {
                    Long pid = progressCalculationService.resolveProjectIdOfTask(t.getId());
                    ProjectEntity p = pid != null ? projectMap.get(pid) : null;
                    alerts.add(DashboardAlertDto.builder()
                            .alertType("OVERDUE_TASK")
                            .severity("WARNING")
                            .title("Công việc trễ hạn: " + t.getName())
                            .subtitle((p != null ? p.getCode() + " • " : "") + "Hạn: " + t.getEndDate())
                            .dueDate(t.getEndDate())
                            .link(TaskWorkflowService.buildTaskLink(pid, t.getId()))
                            .build());
                }
            }
        }

        // Đợt thanh toán sắp đến hạn trong 7 ngày tới
        for (ProjectLineEntity line : paymentPlans) {
            if (line.getPaymentDate() != null
                    && !line.getPaymentDate().isAfter(today.plusDays(7))
                    && line.getStatus() != PaymentPlanStatus.PAID
                    && alerts.size() < 35) {
                ProjectEntity p = line.getProjectId() != null ? projectMap.get(line.getProjectId()) : null;
                alerts.add(DashboardAlertDto.builder()
                        .alertType("PAYMENT_DUE")
                        .severity(line.getPaymentDate().isBefore(today) ? "ERROR" : "INFO")
                        .title("Đợt thanh toán " + (line.getPaymentNo() != null ? "#" + line.getPaymentNo() : "")
                                + (p != null ? " - " + p.getName() : ""))
                        .subtitle("Ngày dự kiến: " + line.getPaymentDate()
                                + (line.getPaymentAmount() != null ? " • " + line.getPaymentAmount().toPlainString() + " VNĐ" : ""))
                        .dueDate(line.getPaymentDate())
                        .link(p != null ? "/Constructor/Project/" + p.getId() + "?tab=finance" : "/Constructor/Project")
                        .build());
            }
        }

        Long currentStaffId = currentUserService.getCurrentStaffId();
        List<MyTaskItemDto> myTasks = new ArrayList<>();
        if (currentStaffId != null) {
            Set<Long> myTaskIds = taskStaffMapJpaRepository.findAll().stream()
                    .filter(m -> Objects.equals(m.getStaffId(), currentStaffId))
                    .map(TaskStaffMapEntity::getTaskId)
                    .collect(Collectors.toSet());
            for (TaskEntity t : allTasks) {
                if (myTaskIds.contains(t.getId()) && t.getState() != TaskState.COMPLETED) {
                    Long pid = progressCalculationService.resolveProjectIdOfTask(t.getId());
                    ProjectEntity p = pid != null ? projectMap.get(pid) : null;
                    boolean isLate = t.getEndDate() != null && t.getEndDate().isBefore(today);
                    myTasks.add(MyTaskItemDto.builder()
                            .id(t.getId())
                            .code(t.getCode())
                            .name(t.getName())
                            .projectId(pid)
                            .projectName(p != null ? p.getName() : null)
                            .state(t.getState() != null ? t.getState().name() : TaskState.NOT_STARTED.name())
                            .stateLabel(t.getState() != null ? t.getState().getLabel() : TaskState.NOT_STARTED.getLabel())
                            .progressPercent(zeroIfNull(t.getProgressPercent()))
                            .endDate(t.getEndDate())
                            .overdue(isLate)
                            .build());
                }
            }
            myTasks.sort(Comparator.comparing(MyTaskItemDto::getEndDate, Comparator.nullsLast(Comparator.naturalOrder())));
        }

        return DashboardOverviewDto.builder()
                .totalProjects(projects.size())
                .activeProjects(activeProjects)
                .overdueProjects(overdueProjects)
                .overdueTasks(overdueTasks)
                .pendingApprovalTasks(pendingApprovalTasks)
                .myOpenTasksCount(myTasks.size())
                .totalContractValue(totalContractValue)
                .totalCollectedAmount(totalCollected)
                .receivableTotal(receivableTotal)
                .collectedThisMonth(collectedThisMonth)
                .projectsByState(projectsByState)
                .monthlyCashflow(monthlyCashflow)
                .alerts(alerts)
                .myTasks(myTasks.stream().limit(15).toList())
                .build();
    }

    public ProjectReportDto.SummaryResponse getProjectReports(Long projectId) {
        LocalDate today = LocalDate.now();
        List<ProjectEntity> projects = projectId != null
                ? projectJpaRepository.findById(projectId).map(List::of).orElse(Collections.emptyList())
                : projectJpaRepository.findAll();

        Map<Long, BigDecimal> collectedByProject = new HashMap<>();
        for (ProjectPaymentEntity pay : projectPaymentJpaRepository.findAll()) {
            if (pay.getStatus() == ProjectPaymentStatus.CONFIRMED && pay.getProjectId() != null && pay.getAmount() != null) {
                collectedByProject.merge(pay.getProjectId(), pay.getAmount(), BigDecimal::add);
            }
        }

        Map<Long, BigDecimal> boqCostByProject = new HashMap<>();
        for (ProjectCategoryMapEntity map : projectCategoryMapJpaRepository.findAll()) {
            if (map.getProjectId() != null && map.getTotalAmount() != null) {
                boqCostByProject.merge(map.getProjectId(), map.getTotalAmount(), BigDecimal::add);
            }
        }

        Map<Long, int[]> phaseStatsByProject = new HashMap<>();
        for (ProjectProgressEntity ph : projectProgressJpaRepository.findAll()) {
            if (ph.getProjectId() == null) continue;
            int[] arr = phaseStatsByProject.computeIfAbsent(ph.getProjectId(), k -> new int[2]);
            arr[1]++;
            if (ph.getProgressPercent() != null && ph.getProgressPercent().compareTo(BigDecimal.valueOf(100)) >= 0) {
                arr[0]++;
            }
        }

        Map<Long, List<Long>> steelProjectIdsByMainProject = new HashMap<>();
        for (SteelProjectEntity sp : steelProjectJpaRepository.findAll()) {
            if (sp.getProjectId() != null) {
                steelProjectIdsByMainProject.computeIfAbsent(sp.getProjectId(), k -> new ArrayList<>()).add(sp.getId());
            }
        }
        Map<Long, BigDecimal> weightBySteelProject = new HashMap<>();
        for (SteelProjectAssemblyMapEntity asm : steelProjectAssemblyMapJpaRepository.findAll()) {
            if (asm.getSteelProjectId() != null && asm.getTotalWeight() != null) {
                weightBySteelProject.merge(asm.getSteelProjectId(), asm.getTotalWeight(), BigDecimal::add);
            }
        }

        List<ProjectReportDto> rows = new ArrayList<>();
        BigDecimal sumContract = BigDecimal.ZERO;
        BigDecimal sumCollected = BigDecimal.ZERO;
        BigDecimal sumReceivable = BigDecimal.ZERO;
        BigDecimal sumCost = BigDecimal.ZERO;
        BigDecimal sumProfit = BigDecimal.ZERO;
        BigDecimal sumProgress = BigDecimal.ZERO;

        for (ProjectEntity p : projects) {
            BigDecimal contractVal = zeroIfNull(p.getContractValue());
            BigDecimal collected = collectedByProject.getOrDefault(p.getId(), BigDecimal.ZERO);
            BigDecimal receivable = contractVal.subtract(collected).max(BigDecimal.ZERO);
            BigDecimal estCost = boqCostByProject.getOrDefault(p.getId(), BigDecimal.ZERO);
            BigDecimal estProfit = contractVal.subtract(estCost);
            BigDecimal margin = contractVal.compareTo(BigDecimal.ZERO) > 0
                    ? estProfit.multiply(BigDecimal.valueOf(100)).divide(contractVal, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal steelWeight = BigDecimal.ZERO;
            for (Long spId : steelProjectIdsByMainProject.getOrDefault(p.getId(), Collections.emptyList())) {
                steelWeight = steelWeight.add(weightBySteelProject.getOrDefault(spId, BigDecimal.ZERO));
            }

            int[] phaseStats = phaseStatsByProject.getOrDefault(p.getId(), new int[]{0, 0});
            ProjectState state = p.getState() != null ? p.getState() : ProjectState.RECEIVED;
            boolean overdue = p.getDeliveryDate() != null && p.getDeliveryDate().isBefore(today) && !state.isCompletedOrClosed();
            BigDecimal progress = zeroIfNull(p.getProgressPercent());

            rows.add(ProjectReportDto.builder()
                    .projectId(p.getId())
                    .projectCode(p.getCode())
                    .projectName(p.getName())
                    .customerName(p.getOwner())
                    .state(state.name())
                    .stateLabel(state.getLabel())
                    .signDate(p.getSignDate())
                    .deliveryDate(p.getDeliveryDate())
                    .overdue(overdue)
                    .progressPercent(progress)
                    .contractValue(contractVal)
                    .collectedAmount(collected)
                    .remainingReceivable(receivable)
                    .estimatedCost(estCost)
                    .estimatedProfit(estProfit)
                    .profitMarginPercent(margin)
                    .totalSteelWeightKg(steelWeight.setScale(2, RoundingMode.HALF_UP))
                    .completedPhases(phaseStats[0])
                    .totalPhases(phaseStats[1])
                    .build());

            sumContract = sumContract.add(contractVal);
            sumCollected = sumCollected.add(collected);
            sumReceivable = sumReceivable.add(receivable);
            sumCost = sumCost.add(estCost);
            sumProfit = sumProfit.add(estProfit);
            sumProgress = sumProgress.add(progress);
        }

        BigDecimal avgProgress = rows.isEmpty()
                ? BigDecimal.ZERO
                : sumProgress.divide(BigDecimal.valueOf(rows.size()), 2, RoundingMode.HALF_UP);

        return ProjectReportDto.SummaryResponse.builder()
                .totalContractValue(sumContract)
                .totalCollected(sumCollected)
                .totalReceivable(sumReceivable)
                .totalEstimatedCost(sumCost)
                .totalEstimatedProfit(sumProfit)
                .averageProgressPercent(avgProgress)
                .projects(rows)
                .build();
    }

    public byte[] exportProjectReportExcel(Long projectId) {
        ProjectReportDto.SummaryResponse report = getProjectReports(projectId);
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Bao cao theo du an");
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

            String[] cols = {
                    "Mã dự án", "Tên dự án", "Chủ đầu tư", "Trạng thái", "Tiến độ (%)",
                    "Giá trị HĐ (VNĐ)", "Đã thu (VNĐ)", "Còn phải thu (VNĐ)",
                    "Chi phí dự toán (VNĐ)", "Lợi nhuận tạm tính (VNĐ)", "Biên LN (%)", "Khối lượng thép (kg)"
            };
            Row h = sheet.createRow(0);
            for (int i = 0; i < cols.length; i++) {
                Cell c = h.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }

            int rIdx = 1;
            for (ProjectReportDto p : report.getProjects()) {
                Row r = sheet.createRow(rIdx++);
                r.createCell(0).setCellValue(Objects.toString(p.getProjectCode(), ""));
                r.createCell(1).setCellValue(Objects.toString(p.getProjectName(), ""));
                r.createCell(2).setCellValue(Objects.toString(p.getCustomerName(), ""));
                r.createCell(3).setCellValue(Objects.toString(p.getStateLabel(), ""));
                setNum(r, 4, p.getProgressPercent(), numStyle);
                setNum(r, 5, p.getContractValue(), numStyle);
                setNum(r, 6, p.getCollectedAmount(), numStyle);
                setNum(r, 7, p.getRemainingReceivable(), numStyle);
                setNum(r, 8, p.getEstimatedCost(), numStyle);
                setNum(r, 9, p.getEstimatedProfit(), numStyle);
                setNum(r, 10, p.getProfitMarginPercent(), numStyle);
                setNum(r, 11, p.getTotalSteelWeightKg(), numStyle);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw BusinessException.exception("RPT001", "Không thể xuất Excel báo cáo dự án: " + e.getMessage());
        }
    }

    private void setNum(Row r, int col, BigDecimal val, CellStyle style) {
        Cell c = r.createCell(col);
        c.setCellValue(zeroIfNull(val).doubleValue());
        c.setCellStyle(style);
    }

    private BigDecimal zeroIfNull(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }
}

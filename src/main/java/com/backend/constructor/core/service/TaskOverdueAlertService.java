package com.backend.constructor.core.service;

import com.backend.constructor.core.domain.entity.ProjectEntity;
import com.backend.constructor.core.domain.entity.ProjectLineEntity;
import com.backend.constructor.core.domain.entity.TaskEntity;
import com.backend.constructor.core.domain.entity.TaskStaffMapEntity;
import com.backend.constructor.core.domain.enums.NotificationType;
import com.backend.constructor.core.port.repository.ProjectRepository;
import com.backend.constructor.core.port.repository.TaskRepository;
import com.backend.constructor.core.port.repository.TaskStaffMapRepository;
import com.backend.constructor.infras.repository.jpa.ProjectLineJpaRepository;
import com.backend.constructor.infras.repository.jpa.TaskJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 2.26: Cảnh báo trễ hạn – quét định kỳ các công việc quá hạn kế hoạch nhưng chưa hoàn thành,
 * gửi thông báo cho người làm, người duyệt và chủ nhiệm dự án (tối đa 1 lần / ngày / công việc).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskOverdueAlertService {
    private final TaskJpaRepository taskJpaRepository;
    private final TaskRepository taskRepository;
    private final TaskStaffMapRepository taskStaffMapRepository;
    private final ProjectRepository projectRepository;
    private final ProgressCalculationService progressCalculationService;
    private final NotificationService notificationService;
    private final ProjectLineJpaRepository projectLineJpaRepository;

    /** Chạy mỗi ngày lúc 07:00 và 13:00 */
    @Scheduled(cron = "${app.task-overdue-alert.cron:0 0 7,13 * * *}")
    public void scheduledScan() {
        try {
            int count = scanAndNotify();
            if (count > 0) {
                log.info("[OverdueAlert] Đã gửi cảnh báo trễ hạn cho {} công việc", count);
            }
        } catch (Exception ex) {
            log.error("[OverdueAlert] Lỗi khi quét công việc trễ hạn", ex);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void scanOnStartup() {
        scheduledScan();
    }

    @Transactional
    public int scanAndNotify() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        List<TaskEntity> overdue = taskJpaRepository.findOverdueTasksForAlert(today, startOfToday);
        if (overdue.isEmpty()) return 0;

        // Chỉ cảnh báo công việc lá (việc cha được suy ra từ việc con)
        Set<Long> overdueIds = overdue.stream().map(TaskEntity::getId).collect(Collectors.toSet());
        Set<Long> parentIds = taskRepository.getListTaskByParentIds(overdueIds).stream()
                .map(TaskEntity::getParentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Set<Long>> assigneesByTask = taskStaffMapRepository.getListByTaskIds(overdueIds).stream()
                .filter(m -> m.getTaskId() != null && m.getStaffId() != null)
                .collect(Collectors.groupingBy(TaskStaffMapEntity::getTaskId,
                        Collectors.mapping(TaskStaffMapEntity::getStaffId, Collectors.toSet())));

        Map<Long, Optional<ProjectEntity>> projectCache = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        int notified = 0;
        List<TaskEntity> toSave = new ArrayList<>();
        for (TaskEntity task : overdue) {
            if (parentIds.contains(task.getId())) {
                task.setOverdueNotifiedAt(now);
                toSave.add(task);
                continue;
            }
            Long projectId = progressCalculationService.resolveProjectIdOfTask(task.getId());
            ProjectEntity project = projectId == null ? null
                    : projectCache.computeIfAbsent(projectId, projectRepository::findById).orElse(null);

            Set<Long> recipients = new HashSet<>(assigneesByTask.getOrDefault(task.getId(), Set.of()));
            if (task.getReviewerId() != null) recipients.add(task.getReviewerId());
            if (project != null && project.getManagerId() != null) recipients.add(project.getManagerId());

            long lateDays = ChronoUnit.DAYS.between(task.getEndDate(), today);
            String stateLabel = task.getState() != null ? task.getState().getLabel() : "Chưa làm";
            notificationService.notifyStaffs(recipients, NotificationType.TASK_OVERDUE,
                    "Trễ hạn " + lateDays + " ngày: " + task.getName(),
                    "Công việc " + task.getCode()
                            + (project != null ? " (dự án " + project.getCode() + ")" : "")
                            + " có hạn kế hoạch " + task.getEndDate() + " nhưng vẫn ở trạng thái \"" + stateLabel
                            + "\". Vui lòng cập nhật tiến độ hoặc điều chỉnh kế hoạch.",
                    TaskWorkflowService.buildTaskLink(projectId, task.getId()),
                    "TASK", task.getId(), null);
            task.setOverdueNotifiedAt(now);
            toSave.add(task);
            notified++;
        }
        taskRepository.saveAll(toSave);

        // 3.09: Quét công việc sắp đến hạn (trong 2 ngày tới)
        List<TaskEntity> dueSoon = taskJpaRepository.findDueSoonTasksForAlert(today, today.plusDays(2), startOfToday);
        if (!dueSoon.isEmpty()) {
            Set<Long> dueIds = dueSoon.stream().map(TaskEntity::getId).collect(Collectors.toSet());
            Map<Long, Set<Long>> dueAssignees = taskStaffMapRepository.getListByTaskIds(dueIds).stream()
                    .filter(m -> m.getTaskId() != null && m.getStaffId() != null)
                    .collect(Collectors.groupingBy(TaskStaffMapEntity::getTaskId,
                            Collectors.mapping(TaskStaffMapEntity::getStaffId, Collectors.toSet())));
            List<TaskEntity> dueToSave = new ArrayList<>();
            for (TaskEntity task : dueSoon) {
                Long projectId = progressCalculationService.resolveProjectIdOfTask(task.getId());
                ProjectEntity project = projectId == null ? null
                        : projectCache.computeIfAbsent(projectId, projectRepository::findById).orElse(null);
                Set<Long> recipients = new HashSet<>(dueAssignees.getOrDefault(task.getId(), Set.of()));
                if (task.getReviewerId() != null) recipients.add(task.getReviewerId());
                long daysLeft = ChronoUnit.DAYS.between(today, task.getEndDate());
                notificationService.notifyStaffs(recipients, NotificationType.TASK_DUE_SOON,
                        "Sắp đến hạn (" + (daysLeft == 0 ? "hôm nay" : "còn " + daysLeft + " ngày") + "): " + task.getName(),
                        "Công việc " + task.getCode()
                                + (project != null ? " (dự án " + project.getCode() + ")" : "")
                                + " có hạn kế hoạch " + task.getEndDate() + ".",
                        TaskWorkflowService.buildTaskLink(projectId, task.getId()),
                        "TASK", task.getId(), null);
                task.setOverdueNotifiedAt(now);
                dueToSave.add(task);
                notified++;
            }
            taskRepository.saveAll(dueToSave);
        }

        // 3.09: Quét đợt thanh toán sắp/đến hạn (trong 3 ngày tới hoặc đã quá hạn)
        List<ProjectLineEntity> duePayments = projectLineJpaRepository.findDuePaymentsForAlert(today.plusDays(3), startOfToday);
        if (!duePayments.isEmpty()) {
            List<ProjectLineEntity> linesToSave = new ArrayList<>();
            for (ProjectLineEntity line : duePayments) {
                if (line.getProjectId() == null) continue;
                ProjectEntity project = projectCache.computeIfAbsent(line.getProjectId(), projectRepository::findById).orElse(null);
                if (project == null) continue;
                Set<Long> recipients = new HashSet<>();
                if (project.getManagerId() != null) recipients.add(project.getManagerId());
                if (project.getSupporterId() != null) recipients.add(project.getSupporterId());
                notificationService.notifyStaffs(recipients, NotificationType.PAYMENT_DUE,
                        "Đến hạn thanh toán đợt " + (line.getPaymentNo() != null ? line.getPaymentNo() : "") + " - " + project.getName(),
                        "Dự án " + project.getCode() + " có đợt thanh toán dự kiến ngày " + line.getPaymentDate()
                                + (line.getPaymentAmount() != null ? " (Số tiền: " + line.getPaymentAmount().toPlainString() + " VNĐ)" : ""),
                        "/Constructor/Project/" + project.getId() + "?tab=finance",
                        "PROJECT_PAYMENT", line.getId(), null);
                line.setLastDueAlertAt(now);
                linesToSave.add(line);
                notified++;
            }
            projectLineJpaRepository.saveAll(linesToSave);
        }

        return notified;
    }
}

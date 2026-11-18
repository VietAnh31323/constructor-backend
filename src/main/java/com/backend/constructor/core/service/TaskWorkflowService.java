package com.backend.constructor.core.service;

import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.core.domain.entity.ProjectEntity;
import com.backend.constructor.core.domain.entity.TaskEntity;
import com.backend.constructor.core.domain.entity.TaskStaffMapEntity;
import com.backend.constructor.core.domain.enums.NotificationType;
import com.backend.constructor.core.domain.enums.TaskState;
import com.backend.constructor.core.port.repository.ProjectRepository;
import com.backend.constructor.core.port.repository.TaskRepository;
import com.backend.constructor.core.port.repository.TaskStaffMapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 2.23: Workflow trạng thái công việc
 * <pre>
 *   NOT_STARTED ──(người làm)──▶ IN_PROGRESS ──(người làm)──▶ PENDING_REVIEW
 *   PENDING_REVIEW ──(người duyệt)──▶ COMPLETED
 *   PENDING_REVIEW ──(người duyệt, bắt buộc lý do)──▶ RETURNED ──(người làm)──▶ IN_PROGRESS
 *   COMPLETED ──(người duyệt)──▶ IN_PROGRESS   (mở lại)
 * </pre>
 * Chỉ người duyệt (reviewer của công việc), chủ nhiệm dự án hoặc Admin mới được Hoàn thành / Trả lại.
 * Việc cha (có việc con) không chuyển tay – trạng thái được tự suy ra (2.22).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskWorkflowService {
    private static final Map<TaskState, List<TaskState>> TRANSITIONS = Map.of(
            TaskState.NOT_STARTED, List.of(TaskState.IN_PROGRESS),
            TaskState.IN_PROGRESS, List.of(TaskState.PENDING_REVIEW, TaskState.NOT_STARTED),
            TaskState.PENDING_REVIEW, List.of(TaskState.COMPLETED, TaskState.RETURNED),
            TaskState.RETURNED, List.of(TaskState.IN_PROGRESS),
            TaskState.COMPLETED, List.of(TaskState.IN_PROGRESS)
    );
    private static final Set<TaskState> REVIEWER_ONLY_TARGETS = EnumSet.of(TaskState.COMPLETED, TaskState.RETURNED);

    private final TaskRepository taskRepository;
    private final TaskStaffMapRepository taskStaffMapRepository;
    private final ProjectRepository projectRepository;
    private final ProgressCalculationService progressCalculationService;
    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    /** Ngữ cảnh người dùng hiện tại – tính 1 lần rồi dùng cho nhiều công việc. */
    public record Actor(Long staffId, boolean admin) {
    }

    public Actor currentActor() {
        return new Actor(currentUserService.getCurrentStaffId(), currentUserService.isAdmin());
    }

    public boolean isReviewer(TaskEntity task, Long projectManagerId, Actor actor) {
        if (actor.admin()) return true;
        if (actor.staffId() == null) return false;
        return Objects.equals(actor.staffId(), task.getReviewerId())
                || Objects.equals(actor.staffId(), projectManagerId);
    }

    public boolean isWorker(TaskEntity task, Set<Long> assigneeIds, Long projectManagerId, Actor actor) {
        if (isReviewer(task, projectManagerId, actor)) return true;
        if (actor.staffId() == null) return false;
        // Công việc chưa phân công: mọi thành viên có quyền cập nhật công việc đều được thực hiện
        return assigneeIds == null || assigneeIds.isEmpty() || assigneeIds.contains(actor.staffId());
    }

    public List<TaskState> allowedNextStates(TaskEntity task,
                                             boolean hasChildren,
                                             Set<Long> assigneeIds,
                                             Long projectManagerId,
                                             Actor actor) {
        if (hasChildren) return Collections.emptyList();
        TaskState current = task.getState() != null ? task.getState() : TaskState.NOT_STARTED;
        boolean reviewer = isReviewer(task, projectManagerId, actor);
        boolean worker = isWorker(task, assigneeIds, projectManagerId, actor);
        List<TaskState> result = new ArrayList<>();
        for (TaskState target : TRANSITIONS.getOrDefault(current, List.of())) {
            boolean needReviewer = REVIEWER_ONLY_TARGETS.contains(target)
                    || (current == TaskState.COMPLETED && target == TaskState.IN_PROGRESS);
            if (needReviewer ? reviewer : worker) {
                result.add(target);
            }
        }
        return result;
    }

    @Transactional
    public TaskEntity transition(Long taskId, TaskState target, String reason) {
        if (target == null) throw BusinessException.exception("CST019");
        TaskEntity task = taskRepository.getTaskById(taskId);
        TaskState current = task.getState() != null ? task.getState() : TaskState.NOT_STARTED;
        if (current == target) return task;

        if (!taskRepository.getListTaskByParentId(task.getId()).isEmpty()) {
            throw BusinessException.exception("CST058");
        }
        if (!TRANSITIONS.getOrDefault(current, List.of()).contains(target)) {
            throw BusinessException.exception("CST056");
        }

        Long projectId = progressCalculationService.resolveProjectIdOfTask(task.getId());
        ProjectEntity project = projectId != null ? projectRepository.findById(projectId).orElse(null) : null;
        Long pmId = project != null ? project.getManagerId() : null;
        Set<Long> assigneeIds = taskStaffMapRepository.getListByTaskId(task.getId()).stream()
                .map(TaskStaffMapEntity::getStaffId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Actor actor = currentActor();

        boolean needReviewer = REVIEWER_ONLY_TARGETS.contains(target)
                || (current == TaskState.COMPLETED && target == TaskState.IN_PROGRESS);
        if (needReviewer && !isReviewer(task, pmId, actor)) {
            throw BusinessException.exception("CST059");
        }
        if (!needReviewer && !isWorker(task, assigneeIds, pmId, actor)) {
            throw BusinessException.exception("CST055");
        }
        String trimmedReason = StringUtils.trimToNull(reason);
        if (target == TaskState.RETURNED && trimmedReason == null) {
            throw BusinessException.exception("CST057");
        }

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        task.setState(target);
        switch (target) {
            case IN_PROGRESS -> {
                if (task.getActualStartDate() == null) task.setActualStartDate(today);
                task.setActualEndDate(null);
            }
            case PENDING_REVIEW -> {
                if (task.getActualStartDate() == null) task.setActualStartDate(today);
                task.setSubmittedAt(now);
                task.setReturnReason(null);
            }
            case COMPLETED -> {
                if (task.getActualStartDate() == null) task.setActualStartDate(today);
                task.setActualEndDate(today);
                task.setReviewedAt(now);
                task.setReviewedById(actor.staffId());
                task.setReturnReason(null);
            }
            case RETURNED -> {
                task.setReviewedAt(now);
                task.setReviewedById(actor.staffId());
                task.setReturnReason(trimmedReason);
                task.setActualEndDate(null);
            }
            case NOT_STARTED -> {
                task.setActualStartDate(null);
                task.setActualEndDate(null);
            }
            default -> {
            }
        }
        task.setProgressPercent(java.math.BigDecimal.valueOf(target.getDefaultPercent()));
        taskRepository.save(task);

        if (projectId != null) {
            progressCalculationService.recalculateProject(projectId);
        }

        sendWorkflowNotifications(task, current, target, trimmedReason, project, assigneeIds, actor);

        auditLogService.logExplicit(
                "CHANGE_TASK_STATE",
                "PROGRESS",
                "TaskEntity",
                String.valueOf(task.getId()),
                Map.of("state", current.name(), "label", current.getLabel()),
                Map.of("state", target.name(), "label", target.getLabel(),
                        "reason", trimmedReason != null ? trimmedReason : ""),
                "Chuyển trạng thái công việc " + task.getCode() + " (" + task.getName() + "): "
                        + current.getLabel() + " → " + target.getLabel(),
                null
        );
        return task;
    }

    private void sendWorkflowNotifications(TaskEntity task,
                                           TaskState from,
                                           TaskState to,
                                           String reason,
                                           ProjectEntity project,
                                           Set<Long> assigneeIds,
                                           Actor actor) {
        String link = buildTaskLink(project != null ? project.getId() : null, task.getId());
        String projectLabel = project != null ? " – dự án " + project.getCode() : "";
        String actorName = currentUserService.getCurrentDisplayName();
        switch (to) {
            case PENDING_REVIEW -> {
                Set<Long> reviewers = new HashSet<>();
                if (task.getReviewerId() != null) reviewers.add(task.getReviewerId());
                if (reviewers.isEmpty() && project != null && project.getManagerId() != null) {
                    reviewers.add(project.getManagerId());
                }
                notificationService.notifyStaffs(reviewers, NotificationType.TASK_SUBMITTED,
                        "Công việc chờ duyệt: " + task.getName(),
                        actorName + " đã trình duyệt công việc " + task.getCode() + projectLabel + ".",
                        link, "TASK", task.getId(), actor.staffId());
            }
            case COMPLETED -> notificationService.notifyStaffs(assigneeIds, NotificationType.TASK_APPROVED,
                    "Công việc đã được duyệt hoàn thành: " + task.getName(),
                    actorName + " đã duyệt hoàn thành công việc " + task.getCode() + projectLabel + ".",
                    link, "TASK", task.getId(), actor.staffId());
            case RETURNED -> notificationService.notifyStaffs(assigneeIds, NotificationType.TASK_RETURNED,
                    "Công việc bị trả lại: " + task.getName(),
                    actorName + " đã trả lại công việc " + task.getCode() + projectLabel + ". Lý do: " + reason,
                    link, "TASK", task.getId(), actor.staffId());
            default -> {
            }
        }
    }

    public static String buildTaskLink(Long projectId, Long taskId) {
        if (projectId == null) return null;
        return "/Constructor/Project/" + projectId + "?actionType=VIEW&tab=gantt&taskId=" + taskId;
    }
}

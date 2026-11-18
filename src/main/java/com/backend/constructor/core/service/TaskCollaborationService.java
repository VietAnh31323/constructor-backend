package com.backend.constructor.core.service;

import com.backend.constructor.app.dto.task.TaskCommentDto;
import com.backend.constructor.app.dto.task.TaskSiteReportDto;
import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.core.domain.entity.*;
import com.backend.constructor.core.domain.enums.NotificationType;
import com.backend.constructor.core.domain.enums.SiteReportPhase;
import com.backend.constructor.core.port.repository.*;
import com.backend.constructor.infras.repository.jpa.TaskCommentJpaRepository;
import com.backend.constructor.infras.repository.jpa.TaskSiteReportJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 2.25: Báo cáo hiện trường (ảnh trước / trong / sau, ghi chú, GPS, thời gian)
 * 2.27: Bình luận, @mention, đính kèm file trong công việc
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskCollaborationService {
    private final TaskRepository taskRepository;
    private final TaskStaffMapRepository taskStaffMapRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final StaffRepository staffRepository;
    private final TaskSiteReportJpaRepository siteReportRepository;
    private final TaskCommentJpaRepository commentRepository;
    private final ProgressCalculationService progressCalculationService;
    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    // ============================== 2.25: BÁO CÁO HIỆN TRƯỜNG ==============================

    public List<TaskSiteReportDto> getSiteReports(Long taskId) {
        TaskContext ctx = loadContext(taskId);
        Long staffId = currentUserService.getCurrentStaffId();
        boolean admin = currentUserService.isAdmin();
        return siteReportRepository.findAllByTaskIdOrderByCreatedAtDesc(ctx.task().getId()).stream()
                .map(e -> toSiteReportDto(e, staffId, admin))
                .toList();
    }

    public List<TaskSiteReportDto> getProjectSiteReports(Long projectId) {
        checkProjectAccess(projectId);
        Long staffId = currentUserService.getCurrentStaffId();
        boolean admin = currentUserService.isAdmin();
        return siteReportRepository.findAllByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(e -> toSiteReportDto(e, staffId, admin))
                .toList();
    }

    @Transactional
    public TaskSiteReportDto createSiteReport(Long taskId, TaskSiteReportDto input) {
        TaskContext ctx = loadContext(taskId);
        boolean hasImages = input.getImages() != null && !input.getImages().isEmpty();
        String note = StringUtils.trimToNull(input.getNote());
        if (!hasImages && note == null) {
            throw BusinessException.exception("CST060");
        }
        Long staffId = currentUserService.getCurrentStaffId();
        TaskSiteReportEntity entity = TaskSiteReportEntity.builder()
                .taskId(ctx.task().getId())
                .projectId(ctx.projectId())
                .reportPhase(input.getReportPhase() != null ? input.getReportPhase() : SiteReportPhase.DURING)
                .note(note)
                .images(hasImages ? input.getImages() : new ArrayList<>())
                .latitude(input.getLatitude() != null ? input.getLatitude().setScale(7, RoundingMode.HALF_UP) : null)
                .longitude(input.getLongitude() != null ? input.getLongitude().setScale(7, RoundingMode.HALF_UP) : null)
                .locationAccuracy(input.getLocationAccuracy() != null ? input.getLocationAccuracy().setScale(2, RoundingMode.HALF_UP) : null)
                .locationAddress(StringUtils.abbreviate(StringUtils.trimToNull(input.getLocationAddress()), 495))
                .capturedAt(input.getCapturedAt() != null ? input.getCapturedAt() : LocalDateTime.now())
                .reporterStaffId(staffId)
                .reporterName(currentUserService.getCurrentDisplayName())
                .build();
        siteReportRepository.save(entity);

        Set<Long> recipients = new HashSet<>(ctx.assigneeIds());
        if (ctx.task().getReviewerId() != null) recipients.add(ctx.task().getReviewerId());
        if (ctx.project() != null && ctx.project().getManagerId() != null) recipients.add(ctx.project().getManagerId());
        notificationService.notifyStaffs(recipients, NotificationType.TASK_SITE_REPORT,
                "Báo cáo hiện trường mới: " + ctx.task().getName(),
                entity.getReporterName() + " đã gửi báo cáo " + entity.getReportPhase().getLabel().toLowerCase()
                        + " cho công việc " + ctx.task().getCode()
                        + (hasImages ? " (" + entity.getImages().size() + " ảnh)" : "") + ".",
                TaskWorkflowService.buildTaskLink(ctx.projectId(), ctx.task().getId()),
                "TASK", ctx.task().getId(), staffId);

        return toSiteReportDto(entity, staffId, currentUserService.isAdmin());
    }

    @Transactional
    public void deleteSiteReport(Long taskId, Long reportId) {
        TaskSiteReportEntity entity = siteReportRepository.findById(reportId)
                .orElseThrow(() -> BusinessException.exception("CST002"));
        if (!Objects.equals(entity.getTaskId(), taskId)) {
            throw BusinessException.exception("CST002");
        }
        Long staffId = currentUserService.getCurrentStaffId();
        if (!currentUserService.isAdmin() && !Objects.equals(entity.getReporterStaffId(), staffId)) {
            throw BusinessException.exception("CST062");
        }
        siteReportRepository.delete(entity);
    }

    private TaskSiteReportDto toSiteReportDto(TaskSiteReportEntity e, Long staffId, boolean admin) {
        return TaskSiteReportDto.builder()
                .id(e.getId())
                .taskId(e.getTaskId())
                .projectId(e.getProjectId())
                .reportPhase(e.getReportPhase())
                .reportPhaseLabel(e.getReportPhase() != null ? e.getReportPhase().getLabel() : null)
                .note(e.getNote())
                .images(e.getImages() != null ? e.getImages() : new ArrayList<>())
                .latitude(e.getLatitude())
                .longitude(e.getLongitude())
                .locationAccuracy(e.getLocationAccuracy())
                .locationAddress(e.getLocationAddress())
                .capturedAt(e.getCapturedAt())
                .reporterStaffId(e.getReporterStaffId())
                .reporterName(e.getReporterName())
                .createdAt(e.getCreatedAt())
                .canDelete(admin || Objects.equals(e.getReporterStaffId(), staffId))
                .build();
    }

    // ============================== 2.27: BÌNH LUẬN & @MENTION ==============================

    public List<TaskCommentDto> getComments(Long taskId) {
        loadContext(taskId);
        List<TaskCommentEntity> entities = commentRepository.findAllByTaskIdOrderByCreatedAtAscIdAsc(taskId);
        Set<Long> mentionIds = entities.stream()
                .flatMap(e -> parseIds(e.getMentionedStaffIds()).stream())
                .collect(Collectors.toSet());
        Map<Long, CodeNameResponse> staffMap = mentionIds.isEmpty()
                ? Collections.emptyMap()
                : staffRepository.getMapSimpleStaffByIds(mentionIds);
        Long staffId = currentUserService.getCurrentStaffId();
        boolean admin = currentUserService.isAdmin();
        return entities.stream().map(e -> toCommentDto(e, staffMap, staffId, admin)).toList();
    }

    @Transactional
    public TaskCommentDto createComment(Long taskId, TaskCommentDto input) {
        TaskContext ctx = loadContext(taskId);
        String content = StringUtils.trimToNull(input.getContent());
        boolean hasAttachments = input.getAttachments() != null && !input.getAttachments().isEmpty();
        if (content == null && !hasAttachments) {
            throw BusinessException.exception("CST061");
        }
        Long staffId = currentUserService.getCurrentStaffId();
        Set<Long> mentionIds = sanitizeIds(input.getMentionedStaffIds());
        TaskCommentEntity entity = TaskCommentEntity.builder()
                .taskId(ctx.task().getId())
                .parentId(input.getParentId())
                .content(content != null ? content : "(Đính kèm tệp)")
                .mentionedStaffIds(joinIds(mentionIds))
                .attachments(hasAttachments ? input.getAttachments() : new ArrayList<>())
                .authorStaffId(staffId)
                .authorName(currentUserService.getCurrentDisplayName())
                .edited(Boolean.FALSE)
                .build();
        commentRepository.save(entity);

        notifyForComment(ctx, entity, mentionIds, staffId);

        Map<Long, CodeNameResponse> staffMap = mentionIds.isEmpty()
                ? Collections.emptyMap()
                : staffRepository.getMapSimpleStaffByIds(mentionIds);
        return toCommentDto(entity, staffMap, staffId, currentUserService.isAdmin());
    }

    @Transactional
    public TaskCommentDto updateComment(Long taskId, Long commentId, TaskCommentDto input) {
        TaskContext ctx = loadContext(taskId);
        TaskCommentEntity entity = commentRepository.findById(commentId)
                .orElseThrow(() -> BusinessException.exception("CST002"));
        if (!Objects.equals(entity.getTaskId(), taskId)) {
            throw BusinessException.exception("CST002");
        }
        Long staffId = currentUserService.getCurrentStaffId();
        if (!currentUserService.isAdmin() && !Objects.equals(entity.getAuthorStaffId(), staffId)) {
            throw BusinessException.exception("CST062");
        }
        String content = StringUtils.trimToNull(input.getContent());
        if (content == null) {
            throw BusinessException.exception("CST061");
        }
        Set<Long> oldMentions = parseIds(entity.getMentionedStaffIds());
        Set<Long> newMentions = sanitizeIds(input.getMentionedStaffIds());
        entity.setContent(content);
        entity.setMentionedStaffIds(joinIds(newMentions));
        if (input.getAttachments() != null) {
            entity.setAttachments(input.getAttachments());
        }
        entity.setEdited(Boolean.TRUE);
        commentRepository.save(entity);

        Set<Long> added = new HashSet<>(newMentions);
        added.removeAll(oldMentions);
        if (!added.isEmpty()) {
            notificationService.notifyStaffs(added, NotificationType.TASK_MENTION,
                    entity.getAuthorName() + " đã nhắc đến bạn trong công việc " + ctx.task().getCode(),
                    StringUtils.abbreviate(content, 300),
                    TaskWorkflowService.buildTaskLink(ctx.projectId(), ctx.task().getId()),
                    "TASK", ctx.task().getId(), staffId);
        }
        Map<Long, CodeNameResponse> staffMap = newMentions.isEmpty()
                ? Collections.emptyMap()
                : staffRepository.getMapSimpleStaffByIds(newMentions);
        return toCommentDto(entity, staffMap, staffId, currentUserService.isAdmin());
    }

    @Transactional
    public void deleteComment(Long taskId, Long commentId) {
        TaskCommentEntity entity = commentRepository.findById(commentId)
                .orElseThrow(() -> BusinessException.exception("CST002"));
        if (!Objects.equals(entity.getTaskId(), taskId)) {
            throw BusinessException.exception("CST002");
        }
        Long staffId = currentUserService.getCurrentStaffId();
        if (!currentUserService.isAdmin() && !Objects.equals(entity.getAuthorStaffId(), staffId)) {
            throw BusinessException.exception("CST062");
        }
        commentRepository.delete(entity);
    }

    private void notifyForComment(TaskContext ctx, TaskCommentEntity entity, Set<Long> mentionIds, Long authorId) {
        String link = TaskWorkflowService.buildTaskLink(ctx.projectId(), ctx.task().getId());
        String preview = StringUtils.abbreviate(entity.getContent(), 300);
        if (!mentionIds.isEmpty()) {
            notificationService.notifyStaffs(mentionIds, NotificationType.TASK_MENTION,
                    entity.getAuthorName() + " đã nhắc đến bạn trong công việc " + ctx.task().getCode(),
                    preview, link, "TASK", ctx.task().getId(), authorId);
        }
        // Những người liên quan khác (người làm, người duyệt) nhận thông báo bình luận mới
        Set<Long> followers = new HashSet<>(ctx.assigneeIds());
        if (ctx.task().getReviewerId() != null) followers.add(ctx.task().getReviewerId());
        followers.removeAll(mentionIds);
        if (!followers.isEmpty()) {
            notificationService.notifyStaffs(followers, NotificationType.TASK_COMMENT,
                    "Bình luận mới trong công việc " + ctx.task().getCode(),
                    entity.getAuthorName() + ": " + preview, link, "TASK", ctx.task().getId(), authorId);
        }
    }

    private TaskCommentDto toCommentDto(TaskCommentEntity e,
                                        Map<Long, CodeNameResponse> staffMap,
                                        Long staffId,
                                        boolean admin) {
        Set<Long> ids = parseIds(e.getMentionedStaffIds());
        return TaskCommentDto.builder()
                .id(e.getId())
                .taskId(e.getTaskId())
                .parentId(e.getParentId())
                .content(e.getContent())
                .mentionedStaffIds(new ArrayList<>(ids))
                .mentionedStaffs(ids.stream().map(staffMap::get).filter(Objects::nonNull).toList())
                .attachments(e.getAttachments() != null ? e.getAttachments() : new ArrayList<>())
                .authorStaffId(e.getAuthorStaffId())
                .authorName(e.getAuthorName())
                .edited(e.getEdited())
                .canEdit(admin || Objects.equals(e.getAuthorStaffId(), staffId))
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    // ============================== SỐ LƯỢNG (hiển thị trên Gantt) ==============================

    public Map<Long, Long> countCommentsByTaskIds(Collection<Long> taskIds) {
        return toCountMap(taskIds == null || taskIds.isEmpty()
                ? Collections.emptyList()
                : commentRepository.countGroupByTaskIds(taskIds));
    }

    public Map<Long, Long> countSiteReportsByTaskIds(Collection<Long> taskIds) {
        return toCountMap(taskIds == null || taskIds.isEmpty()
                ? Collections.emptyList()
                : siteReportRepository.countGroupByTaskIds(taskIds));
    }

    private Map<Long, Long> toCountMap(List<Object[]> rows) {
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            if (row[0] != null && row[1] != null) {
                result.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
            }
        }
        return result;
    }

    // ============================== HELPERS ==============================

    private record TaskContext(TaskEntity task, Long projectId, ProjectEntity project, Set<Long> assigneeIds) {
    }

    private TaskContext loadContext(Long taskId) {
        TaskEntity task = taskRepository.getTaskById(taskId);
        Long projectId = progressCalculationService.resolveProjectIdOfTask(task.getId());
        ProjectEntity project = projectId != null ? projectRepository.findById(projectId).orElse(null) : null;
        Set<Long> assigneeIds = taskStaffMapRepository.getListByTaskId(task.getId()).stream()
                .map(TaskStaffMapEntity::getStaffId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        checkTaskAccess(task, project, assigneeIds);
        return new TaskContext(task, projectId, project, assigneeIds);
    }

    /** Người dùng phải là Admin, thành viên dự án, người làm hoặc người duyệt công việc. */
    private void checkTaskAccess(TaskEntity task, ProjectEntity project, Set<Long> assigneeIds) {
        if (currentUserService.isAdmin()) return;
        Long staffId = currentUserService.getCurrentStaffId();
        if (staffId == null) return;
        if (assigneeIds.contains(staffId) || Objects.equals(task.getReviewerId(), staffId)) return;
        if (project == null) return;
        if (isProjectParticipant(project, staffId)) return;
        throw BusinessException.exception("CST050");
    }

    private void checkProjectAccess(Long projectId) {
        if (currentUserService.isAdmin()) return;
        Long staffId = currentUserService.getCurrentStaffId();
        if (staffId == null) return;
        ProjectEntity project = projectRepository.getProjectById(projectId);
        if (!isProjectParticipant(project, staffId)) {
            throw BusinessException.exception("CST050");
        }
    }

    private boolean isProjectParticipant(ProjectEntity project, Long staffId) {
        return Objects.equals(project.getManagerId(), staffId)
                || Objects.equals(project.getCreatorId(), staffId)
                || Objects.equals(project.getSupporterId(), staffId)
                || projectMemberRepository.findByProjectIdAndStaffId(project.getId(), staffId).isPresent();
    }

    private static Set<Long> sanitizeIds(Collection<Long> ids) {
        if (ids == null) return new LinkedHashSet<>();
        return ids.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<Long> parseIds(String raw) {
        Set<Long> result = new LinkedHashSet<>();
        if (StringUtils.isBlank(raw)) return result;
        for (String part : raw.split(",")) {
            String p = part.trim();
            if (p.isEmpty()) continue;
            try {
                result.add(Long.parseLong(p));
            } catch (NumberFormatException ignored) {
                // bỏ qua giá trị lỗi
            }
        }
        return result;
    }

    private static String joinIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return null;
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}

package com.backend.constructor.core.service;

import com.backend.constructor.core.domain.entity.ProjectEntity;
import com.backend.constructor.core.domain.entity.ProjectProgressEntity;
import com.backend.constructor.core.domain.entity.ProjectProgressTaskMapEntity;
import com.backend.constructor.core.domain.entity.TaskEntity;
import com.backend.constructor.core.domain.enums.TaskState;
import com.backend.constructor.core.port.repository.ProjectProgressRepository;
import com.backend.constructor.core.port.repository.ProjectProgressTaskMapRepository;
import com.backend.constructor.core.port.repository.ProjectRepository;
import com.backend.constructor.core.port.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 2.22: Tự động tính % tiến độ theo trọng số
 * <pre>
 *   Việc con (lá)  : % suy ra từ trạng thái (Chưa làm 0, Đang làm 50, Trả lại 50, Chờ duyệt 90, Hoàn thành 100)
 *   Việc cha       : bình quân gia quyền % các việc con theo weight; trạng thái suy ra từ việc con
 *   Giai đoạn      : bình quân gia quyền % các việc gốc theo weight
 *   Dự án          : bình quân gia quyền % các giai đoạn (có công việc) theo weight giai đoạn
 * </pre>
 * Người dùng không còn nhập tay progress_percent.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressCalculationService {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int MAX_DEPTH = 10;

    private final ProjectRepository projectRepository;
    private final ProjectProgressRepository projectProgressRepository;
    private final ProjectProgressTaskMapRepository projectProgressTaskMapRepository;
    private final TaskRepository taskRepository;

    /** Tính lại toàn bộ cây tiến độ của dự án chứa công việc này. */
    @Transactional
    public void recalculateFromTask(Long taskId) {
        Long projectId = resolveProjectIdOfTask(taskId);
        if (projectId != null) {
            recalculateProject(projectId);
        }
    }

    /** Trả về projectId của công việc (đi ngược lên việc gốc rồi tra bảng giai đoạn). */
    public Long resolveProjectIdOfTask(Long taskId) {
        if (taskId == null) return null;
        TaskEntity cursor = taskRepository.findById(taskId).orElse(null);
        int guard = 0;
        while (cursor != null && cursor.getParentId() != null && guard++ < MAX_DEPTH) {
            cursor = taskRepository.findById(cursor.getParentId()).orElse(null);
        }
        if (cursor == null) return null;
        ProjectEntity project = projectRepository.getProjectByTaskId(cursor.getId());
        return project != null ? project.getId() : null;
    }

    /** Trả về id giai đoạn (project_progress) chứa công việc gốc của task. */
    public Long resolveProjectProgressIdOfTask(Long taskId) {
        if (taskId == null) return null;
        TaskEntity cursor = taskRepository.findById(taskId).orElse(null);
        int guard = 0;
        while (cursor != null && cursor.getParentId() != null && guard++ < MAX_DEPTH) {
            cursor = taskRepository.findById(cursor.getParentId()).orElse(null);
        }
        if (cursor == null) return null;
        return projectProgressTaskMapRepository.getListByTaskIds(Set.of(cursor.getId())).stream()
                .map(ProjectProgressTaskMapEntity::getProjectProgressId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @Transactional
    public BigDecimal recalculateProject(Long projectId) {
        ProjectEntity project = projectRepository.findById(projectId).orElse(null);
        if (project == null) return BigDecimal.ZERO;

        List<ProjectProgressEntity> phases = projectProgressRepository.getListByProjectId(projectId);
        Set<Long> phaseIds = phases.stream().map(ProjectProgressEntity::getId).collect(Collectors.toSet());
        List<ProjectProgressTaskMapEntity> maps = phaseIds.isEmpty()
                ? Collections.emptyList()
                : projectProgressTaskMapRepository.getListByProjectProgressIds(phaseIds);

        Map<Long, List<Long>> rootIdsByPhase = new HashMap<>();
        Set<Long> rootIds = new HashSet<>();
        for (ProjectProgressTaskMapEntity m : maps) {
            if (m.getProjectProgressId() == null || m.getTaskId() == null) continue;
            rootIdsByPhase.computeIfAbsent(m.getProjectProgressId(), k -> new ArrayList<>()).add(m.getTaskId());
            rootIds.add(m.getTaskId());
        }

        // Nạp toàn bộ cây công việc theo từng tầng
        Map<Long, TaskEntity> allTasks = new HashMap<>();
        Map<Long, List<TaskEntity>> childrenByParent = new HashMap<>();
        if (!rootIds.isEmpty()) {
            taskRepository.findAllByIds(rootIds).forEach(t -> allTasks.put(t.getId(), t));
            Set<Long> frontier = new HashSet<>(allTasks.keySet());
            int depth = 0;
            while (!frontier.isEmpty() && depth++ < MAX_DEPTH) {
                List<TaskEntity> children = taskRepository.getListTaskByParentIds(frontier);
                frontier = new HashSet<>();
                for (TaskEntity c : children) {
                    if (allTasks.containsKey(c.getId())) continue;
                    allTasks.put(c.getId(), c);
                    childrenByParent.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c);
                    frontier.add(c.getId());
                }
            }
        }

        Set<TaskEntity> dirty = new HashSet<>();
        Map<Long, BigDecimal> pctCache = new HashMap<>();
        for (Long rid : rootIds) {
            TaskEntity root = allTasks.get(rid);
            if (root != null) {
                computeTask(root, childrenByParent, pctCache, dirty, 0);
            }
        }
        if (!dirty.isEmpty()) {
            taskRepository.saveAll(new ArrayList<>(dirty));
        }

        // Giai đoạn
        BigDecimal projectWeighted = BigDecimal.ZERO;
        BigDecimal projectWeightSum = BigDecimal.ZERO;
        List<ProjectProgressEntity> dirtyPhases = new ArrayList<>();
        for (ProjectProgressEntity phase : phases) {
            List<TaskEntity> roots = rootIdsByPhase.getOrDefault(phase.getId(), Collections.emptyList()).stream()
                    .map(allTasks::get)
                    .filter(Objects::nonNull)
                    .toList();
            BigDecimal phasePct = weightedAverage(roots, pctCache);
            LocalDate actualStart = roots.stream()
                    .map(TaskEntity::getActualStartDate)
                    .filter(Objects::nonNull)
                    .min(LocalDate::compareTo)
                    .orElse(null);
            boolean allDone = !roots.isEmpty() && roots.stream().allMatch(t -> t.getState() == TaskState.COMPLETED);
            LocalDate actualEnd = allDone
                    ? roots.stream().map(TaskEntity::getActualEndDate).filter(Objects::nonNull).max(LocalDate::compareTo).orElse(LocalDate.now())
                    : null;

            boolean changed = !sameDecimal(phase.getProgressPercent(), phasePct)
                    || !Objects.equals(phase.getActualStartDate(), actualStart)
                    || !Objects.equals(phase.getActualEndDate(), actualEnd);
            if (changed) {
                phase.setProgressPercent(phasePct);
                phase.setActualStartDate(actualStart);
                phase.setActualEndDate(actualEnd);
                dirtyPhases.add(phase);
            }
            if (!roots.isEmpty()) {
                BigDecimal w = weightOf(phase.getWeight());
                projectWeighted = projectWeighted.add(phasePct.multiply(w));
                projectWeightSum = projectWeightSum.add(w);
            }
        }
        if (!dirtyPhases.isEmpty()) {
            projectProgressRepository.saveAll(dirtyPhases);
        }

        BigDecimal projectPct = projectWeightSum.compareTo(BigDecimal.ZERO) > 0
                ? projectWeighted.divide(projectWeightSum, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (!sameDecimal(project.getProgressPercent(), projectPct)) {
            project.setProgressPercent(projectPct);
            projectRepository.save(project);
        }
        return projectPct;
    }

    private BigDecimal computeTask(TaskEntity task,
                                   Map<Long, List<TaskEntity>> childrenByParent,
                                   Map<Long, BigDecimal> pctCache,
                                   Set<TaskEntity> dirty,
                                   int depth) {
        if (pctCache.containsKey(task.getId())) {
            return pctCache.get(task.getId());
        }
        List<TaskEntity> children = childrenByParent.getOrDefault(task.getId(), Collections.emptyList());
        BigDecimal pct;
        if (children.isEmpty() || depth >= MAX_DEPTH) {
            TaskState st = task.getState() != null ? task.getState() : TaskState.NOT_STARTED;
            pct = BigDecimal.valueOf(st.getDefaultPercent()).setScale(2, RoundingMode.HALF_UP);
        } else {
            for (TaskEntity c : children) {
                computeTask(c, childrenByParent, pctCache, dirty, depth + 1);
            }
            pct = weightedAverage(children, pctCache);
            TaskState derived = deriveParentState(children);
            if (task.getState() != derived) {
                task.setState(derived);
                dirty.add(task);
            }
            LocalDate actualStart = children.stream()
                    .map(TaskEntity::getActualStartDate)
                    .filter(Objects::nonNull)
                    .min(LocalDate::compareTo)
                    .orElse(null);
            LocalDate actualEnd = derived == TaskState.COMPLETED
                    ? children.stream().map(TaskEntity::getActualEndDate).filter(Objects::nonNull).max(LocalDate::compareTo).orElse(LocalDate.now())
                    : null;
            if (!Objects.equals(task.getActualStartDate(), actualStart) || !Objects.equals(task.getActualEndDate(), actualEnd)) {
                task.setActualStartDate(actualStart);
                task.setActualEndDate(actualEnd);
                dirty.add(task);
            }
        }
        if (!sameDecimal(task.getProgressPercent(), pct)) {
            task.setProgressPercent(pct);
            dirty.add(task);
        }
        pctCache.put(task.getId(), pct);
        return pct;
    }

    /** Trạng thái việc cha được suy ra từ việc con. */
    public static TaskState deriveParentState(List<TaskEntity> children) {
        if (children == null || children.isEmpty()) return TaskState.NOT_STARTED;
        boolean allDone = children.stream().allMatch(c -> c.getState() == TaskState.COMPLETED);
        if (allDone) return TaskState.COMPLETED;
        boolean allNotStarted = children.stream()
                .allMatch(c -> c.getState() == null || c.getState() == TaskState.NOT_STARTED);
        if (allNotStarted) return TaskState.NOT_STARTED;
        boolean anyReturned = children.stream().anyMatch(c -> c.getState() == TaskState.RETURNED);
        if (anyReturned) return TaskState.RETURNED;
        boolean allDoneOrReview = children.stream()
                .allMatch(c -> c.getState() == TaskState.COMPLETED || c.getState() == TaskState.PENDING_REVIEW);
        if (allDoneOrReview) return TaskState.PENDING_REVIEW;
        return TaskState.IN_PROGRESS;
    }

    private BigDecimal weightedAverage(List<TaskEntity> tasks, Map<Long, BigDecimal> pctCache) {
        if (tasks == null || tasks.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal wSum = BigDecimal.ZERO;
        for (TaskEntity t : tasks) {
            BigDecimal w = weightOf(t.getWeight());
            BigDecimal p = pctCache.getOrDefault(t.getId(),
                    BigDecimal.valueOf((t.getState() != null ? t.getState() : TaskState.NOT_STARTED).getDefaultPercent()));
            sum = sum.add(p.multiply(w));
            wSum = wSum.add(w);
        }
        return wSum.compareTo(BigDecimal.ZERO) > 0
                ? sum.divide(wSum, 2, RoundingMode.HALF_UP).min(HUNDRED)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal weightOf(BigDecimal w) {
        return (w == null || w.compareTo(BigDecimal.ZERO) <= 0) ? BigDecimal.ONE : w;
    }

    private static boolean sameDecimal(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return a == b;
        return a.compareTo(b) == 0;
    }
}

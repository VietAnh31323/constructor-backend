package com.backend.constructor.core.service;

import com.backend.constructor.app.api.ProcessTemplateApi;
import com.backend.constructor.app.dto.process_template.ProcessTemplateDto;
import com.backend.constructor.app.dto.process_template.ProcessTemplateStageDto;
import com.backend.constructor.app.dto.process_template.ProcessTemplateTaskDto;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.core.domain.entity.*;
import com.backend.constructor.core.domain.enums.BuildingType;
import com.backend.constructor.core.domain.enums.PriorityLevel;
import com.backend.constructor.core.domain.enums.TaskState;
import com.backend.constructor.core.port.repository.*;
import com.backend.constructor.infras.repository.jpa.ProcessTemplateJpaRepository;
import com.backend.constructor.infras.repository.jpa.ProcessTemplateStageJpaRepository;
import com.backend.constructor.infras.repository.jpa.ProcessTemplateTaskJpaRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProcessTemplateService implements ProcessTemplateApi {
    private final ProcessTemplateJpaRepository templateRepository;
    private final ProcessTemplateStageJpaRepository stageRepository;
    private final ProcessTemplateTaskJpaRepository taskTemplateRepository;
    private final ProjectRepository projectRepository;
    private final ProgressRepository progressRepository;
    private final ProjectProgressRepository projectProgressRepository;
    private final TaskRepository taskRepository;
    private final ProjectProgressTaskMapRepository projectProgressTaskMapRepository;

    @Override
    public List<ProcessTemplateDto> getList(BuildingType buildingType) {
        List<ProcessTemplateEntity> templates = buildingType != null
                ? templateRepository.findAllByBuildingTypeOrderByIsDefaultDescIdDesc(buildingType)
                : templateRepository.findAllByOrderByIdDesc();
        if (templates.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> templateIds = templates.stream().map(ProcessTemplateEntity::getId).toList();
        List<ProcessTemplateStageEntity> allStages = stageRepository.findAllByTemplateIdIn(templateIds);
        List<Long> stageIds = allStages.stream().map(ProcessTemplateStageEntity::getId).toList();
        List<ProcessTemplateTaskEntity> allTasks = stageIds.isEmpty()
                ? Collections.emptyList()
                : taskTemplateRepository.findAllByStageIdInOrderBySortOrderAscIdAsc(stageIds);

        Map<Long, List<ProcessTemplateStageEntity>> stagesByTpl = allStages.stream()
                .collect(Collectors.groupingBy(ProcessTemplateStageEntity::getTemplateId));
        Map<Long, List<ProcessTemplateTaskEntity>> tasksByStage = allTasks.stream()
                .collect(Collectors.groupingBy(ProcessTemplateTaskEntity::getStageId));

        List<ProcessTemplateDto> result = new ArrayList<>();
        for (ProcessTemplateEntity tpl : templates) {
            List<ProcessTemplateStageEntity> stages = stagesByTpl.getOrDefault(tpl.getId(), Collections.emptyList());
            int totalTasks = stages.stream()
                    .mapToInt(s -> tasksByStage.getOrDefault(s.getId(), Collections.emptyList()).size())
                    .sum();
            result.add(ProcessTemplateDto.builder()
                    .id(tpl.getId())
                    .code(tpl.getCode())
                    .name(tpl.getName())
                    .buildingType(tpl.getBuildingType())
                    .buildingTypeLabel(tpl.getBuildingType() != null ? tpl.getBuildingType().getLabel() : null)
                    .description(tpl.getDescription())
                    .isActive(tpl.getIsActive())
                    .isDefault(tpl.getIsDefault())
                    .stageCount(stages.size())
                    .taskCount(totalTasks)
                    .build());
        }
        return result;
    }

    @Override
    public ProcessTemplateDto getDetail(Long id) {
        ProcessTemplateEntity tpl = templateRepository.findById(id)
                .orElseThrow(() -> BusinessException.exception("CST002"));
        List<ProcessTemplateStageEntity> stages = stageRepository.findAllByTemplateIdOrderBySortOrderAscIdAsc(id);
        List<Long> stageIds = stages.stream().map(ProcessTemplateStageEntity::getId).toList();
        List<ProcessTemplateTaskEntity> tasks = stageIds.isEmpty()
                ? Collections.emptyList()
                : taskTemplateRepository.findAllByStageIdInOrderBySortOrderAscIdAsc(stageIds);

        Map<Long, List<ProcessTemplateTaskEntity>> tasksByStage = tasks.stream()
                .collect(Collectors.groupingBy(ProcessTemplateTaskEntity::getStageId));

        List<ProcessTemplateStageDto> stageDtos = new ArrayList<>();
        for (ProcessTemplateStageEntity stage : stages) {
            List<ProcessTemplateTaskDto> taskDtos = tasksByStage.getOrDefault(stage.getId(), Collections.emptyList())
                    .stream()
                    .map(t -> ProcessTemplateTaskDto.builder()
                            .id(t.getId())
                            .stageId(t.getStageId())
                            .name(t.getName())
                            .sortOrder(t.getSortOrder())
                            .offsetDays(t.getOffsetDays())
                            .durationDays(t.getDurationDays())
                            .priority(t.getPriority())
                            .description(t.getDescription())
                            .build())
                    .toList();
            stageDtos.add(ProcessTemplateStageDto.builder()
                    .id(stage.getId())
                    .templateId(stage.getTemplateId())
                    .name(stage.getName())
                    .sortOrder(stage.getSortOrder())
                    .durationDays(stage.getDurationDays())
                    .description(stage.getDescription())
                    .tasks(taskDtos)
                    .build());
        }

        return ProcessTemplateDto.builder()
                .id(tpl.getId())
                .code(tpl.getCode())
                .name(tpl.getName())
                .buildingType(tpl.getBuildingType())
                .buildingTypeLabel(tpl.getBuildingType() != null ? tpl.getBuildingType().getLabel() : null)
                .description(tpl.getDescription())
                .isActive(tpl.getIsActive())
                .isDefault(tpl.getIsDefault())
                .stageCount(stageDtos.size())
                .taskCount(tasks.size())
                .stages(stageDtos)
                .build();
    }

    @Override
    @Transactional
    public IdResponse create(ProcessTemplateDto input) {
        String code = StringUtils.isNotBlank(input.getCode())
                ? input.getCode().trim().toUpperCase()
                : "TPL-" + input.getBuildingType().name() + "-" + System.currentTimeMillis() % 10000;
        if (templateRepository.findByCode(code).isPresent()) {
            throw BusinessException.exception("ERROR_0005");
        }
        ProcessTemplateEntity tpl = ProcessTemplateEntity.builder()
                .code(code)
                .name(input.getName().trim())
                .buildingType(input.getBuildingType())
                .description(StringUtils.trimToNull(input.getDescription()))
                .isActive(input.getIsActive() != null ? input.getIsActive() : Boolean.TRUE)
                .isDefault(input.getIsDefault() != null ? input.getIsDefault() : Boolean.FALSE)
                .build();
        templateRepository.save(tpl);
        saveStagesAndTasks(tpl.getId(), input.getStages());
        return IdResponse.builder().id(tpl.getId()).build();
    }

    @Override
    @Transactional
    public IdResponse update(Long id, ProcessTemplateDto input) {
        ProcessTemplateEntity tpl = templateRepository.findById(id)
                .orElseThrow(() -> BusinessException.exception("CST002"));
        tpl.setName(input.getName().trim());
        tpl.setBuildingType(input.getBuildingType());
        tpl.setDescription(StringUtils.trimToNull(input.getDescription()));
        if (input.getIsActive() != null) tpl.setIsActive(input.getIsActive());
        if (input.getIsDefault() != null) tpl.setIsDefault(input.getIsDefault());
        templateRepository.save(tpl);

        List<ProcessTemplateStageEntity> oldStages = stageRepository.findAllByTemplateIdOrderBySortOrderAscIdAsc(id);
        List<Long> oldStageIds = oldStages.stream().map(ProcessTemplateStageEntity::getId).toList();
        if (!oldStageIds.isEmpty()) {
            taskTemplateRepository.deleteAllByStageIdIn(oldStageIds);
        }
        stageRepository.deleteAllByTemplateId(id);

        saveStagesAndTasks(id, input.getStages());
        return IdResponse.builder().id(id).build();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ProcessTemplateEntity tpl = templateRepository.findById(id)
                .orElseThrow(() -> BusinessException.exception("CST002"));
        List<ProcessTemplateStageEntity> oldStages = stageRepository.findAllByTemplateIdOrderBySortOrderAscIdAsc(id);
        List<Long> oldStageIds = oldStages.stream().map(ProcessTemplateStageEntity::getId).toList();
        if (!oldStageIds.isEmpty()) {
            taskTemplateRepository.deleteAllByStageIdIn(oldStageIds);
        }
        stageRepository.deleteAllByTemplateId(id);
        templateRepository.delete(tpl);
    }

    @Override
    @Transactional
    public void applyToProject(Long projectId, Long templateId) {
        ProjectEntity project = projectRepository.getProjectById(projectId);
        ProcessTemplateEntity template = null;
        if (templateId != null) {
            template = templateRepository.findById(templateId)
                    .orElseThrow(() -> BusinessException.exception("CST002"));
        } else if (project.getBuildingType() != null) {
            template = templateRepository
                    .findFirstByBuildingTypeAndIsActiveTrueOrderByIsDefaultDescIdAsc(project.getBuildingType())
                    .orElse(null);
        }
        if (template == null) {
            throw BusinessException.exception("CST002");
        }
        generateWorkflowFromTemplate(project, template);
    }

    @Transactional
    public void applyDefaultTemplateIfEligible(ProjectEntity project) {
        if (project == null || project.getId() == null || project.getBuildingType() == null) {
            return;
        }
        List<ProjectProgressEntity> existing = projectProgressRepository.getListByProjectId(project.getId());
        if (existing != null && !existing.isEmpty()) {
            return;
        }
        templateRepository
                .findFirstByBuildingTypeAndIsActiveTrueOrderByIsDefaultDescIdAsc(project.getBuildingType())
                .ifPresent(tpl -> generateWorkflowFromTemplate(project, tpl));
    }

    private void generateWorkflowFromTemplate(ProjectEntity project, ProcessTemplateEntity template) {
        List<ProcessTemplateStageEntity> stages = stageRepository.findAllByTemplateIdOrderBySortOrderAscIdAsc(template.getId());
        if (stages.isEmpty()) {
            return;
        }
        List<Long> stageIds = stages.stream().map(ProcessTemplateStageEntity::getId).toList();
        Map<Long, List<ProcessTemplateTaskEntity>> tasksByStage = taskTemplateRepository
                .findAllByStageIdInOrderBySortOrderAscIdAsc(stageIds)
                .stream()
                .collect(Collectors.groupingBy(ProcessTemplateTaskEntity::getStageId));

        List<ProgressEntity> allProgress = progressRepository.findAll();
        Map<String, ProgressEntity> progressByName = new HashMap<>();
        for (ProgressEntity p : allProgress) {
            if (StringUtils.isNotBlank(p.getName())) {
                progressByName.putIfAbsent(p.getName().trim().toLowerCase(), p);
            }
        }

        LocalDate cursorDate = project.getSignDate() != null ? project.getSignDate() : LocalDate.now();
        int taskCounter = 1;

        for (ProcessTemplateStageEntity stage : stages) {
            String stageName = stage.getName().trim();
            ProgressEntity progress = progressByName.get(stageName.toLowerCase());
            if (progress == null) {
                progress = ProgressEntity.builder()
                        .code("GD-" + System.currentTimeMillis() % 100000 + "-" + stage.getSortOrder())
                        .name(stageName)
                        .description(stage.getDescription())
                        .isActive(Boolean.TRUE)
                        .build();
                progressRepository.save(progress);
                progressByName.put(stageName.toLowerCase(), progress);
            }

            ProjectProgressEntity projectProgress = ProjectProgressEntity.builder()
                    .projectId(project.getId())
                    .progressId(progress.getId())
                    .build();
            projectProgressRepository.save(projectProgress);

            List<ProcessTemplateTaskEntity> tplTasks = tasksByStage.getOrDefault(stage.getId(), Collections.emptyList());
            for (ProcessTemplateTaskEntity tplTask : tplTasks) {
                int offset = tplTask.getOffsetDays() != null ? tplTask.getOffsetDays() : 0;
                int dur = tplTask.getDurationDays() != null && tplTask.getDurationDays() > 0 ? tplTask.getDurationDays() : 7;
                LocalDate startDate = cursorDate.plusDays(offset);
                LocalDate endDate = startDate.plusDays(dur);

                TaskEntity task = TaskEntity.builder()
                        .code(String.format("CV-%d-%03d", project.getId(), taskCounter++))
                        .name(tplTask.getName())
                        .description(tplTask.getDescription())
                        .startDate(startDate)
                        .endDate(endDate)
                        .priorityLevel(tplTask.getPriority() != null ? tplTask.getPriority() : PriorityLevel.MEDIUM)
                        .state(TaskState.NOT_STARTED)
                        .progressPercent(BigDecimal.ZERO)
                        .build();
                taskRepository.save(task);

                ProjectProgressTaskMapEntity map = ProjectProgressTaskMapEntity.builder()
                        .projectProgressId(projectProgress.getId())
                        .taskId(task.getId())
                        .build();
                projectProgressTaskMapRepository.save(map);
            }

            int stageDur = stage.getDurationDays() != null && stage.getDurationDays() > 0 ? stage.getDurationDays() : 14;
            cursorDate = cursorDate.plusDays(stageDur);
        }
    }

    private void saveStagesAndTasks(Long templateId, List<ProcessTemplateStageDto> stages) {
        if (stages == null || stages.isEmpty()) {
            return;
        }
        int stageIdx = 1;
        for (ProcessTemplateStageDto sDto : stages) {
            if (sDto == null || StringUtils.isBlank(sDto.getName())) continue;
            ProcessTemplateStageEntity stage = ProcessTemplateStageEntity.builder()
                    .templateId(templateId)
                    .name(sDto.getName().trim())
                    .sortOrder(sDto.getSortOrder() != null ? sDto.getSortOrder() : stageIdx)
                    .durationDays(sDto.getDurationDays() != null ? sDto.getDurationDays() : 14)
                    .description(StringUtils.trimToNull(sDto.getDescription()))
                    .build();
            stageRepository.save(stage);

            if (sDto.getTasks() != null && !sDto.getTasks().isEmpty()) {
                int taskIdx = 1;
                List<ProcessTemplateTaskEntity> taskEntities = new ArrayList<>();
                for (ProcessTemplateTaskDto tDto : sDto.getTasks()) {
                    if (tDto == null || StringUtils.isBlank(tDto.getName())) continue;
                    taskEntities.add(ProcessTemplateTaskEntity.builder()
                            .stageId(stage.getId())
                            .name(tDto.getName().trim())
                            .sortOrder(tDto.getSortOrder() != null ? tDto.getSortOrder() : taskIdx)
                            .offsetDays(tDto.getOffsetDays() != null ? tDto.getOffsetDays() : 0)
                            .durationDays(tDto.getDurationDays() != null ? tDto.getDurationDays() : 7)
                            .priority(tDto.getPriority() != null ? tDto.getPriority() : PriorityLevel.MEDIUM)
                            .description(StringUtils.trimToNull(tDto.getDescription()))
                            .build());
                    taskIdx++;
                }
                if (!taskEntities.isEmpty()) {
                    taskTemplateRepository.saveAll(taskEntities);
                }
            }
            stageIdx++;
        }
    }
}

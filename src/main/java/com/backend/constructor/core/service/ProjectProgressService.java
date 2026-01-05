package com.backend.constructor.core.service;

import com.backend.constructor.app.api.ProjectProgressApi;
import com.backend.constructor.app.dto.project_progress.ProjectInfoDto;
import com.backend.constructor.app.dto.project_progress.ProjectProgressDto;
import com.backend.constructor.app.dto.task.ReviewerDto;
import com.backend.constructor.app.dto.task.TaskOutput;
import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.core.domain.entity.*;
import com.backend.constructor.core.port.mapper.ProjectMapper;
import com.backend.constructor.core.port.mapper.TaskMapper;
import com.backend.constructor.core.port.repository.*;
import com.backend.constructor.core.service.dto.DataCollect;
import com.backend.constructor.core.service.internal.InternalTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static com.backend.constructor.core.service.HelperService.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectProgressService implements ProjectProgressApi {
    private final ProjectRepository projectRepository;
    private final ProjectProgressRepository projectProgressRepository;
    private final ProjectProgressTaskMapRepository projectProgressTaskMapRepository;
    private final StaffRepository staffRepository;
    private final ProjectMapper projectMapper;
    private final ProgressRepository progressRepository;
    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final TaskStaffMapRepository taskStaffMapRepository;
    private final InternalTaskService internalTaskService;

    @Override
    @Transactional
    public IdResponse create(ProjectInfoDto input) {
        ProjectEntity project = projectRepository.getProjectById(input.getId());
        List<ProjectProgressTaskMapEntity> progressTaskMapEntities = new ArrayList<>();
        for (ProjectProgressDto projectProgress : input.getProjectProgress()) {
            CodeNameResponse progress = projectProgress.getProgress();
            if (progress == null) continue;
            ProjectProgressEntity projectProgressEntity = ProjectProgressEntity.builder()
                    .projectId(project.getId())
                    .progressId(progress.getId())
                    .build();
            projectProgressRepository.save(projectProgressEntity);
            addAllIfNotNull(progressTaskMapEntities, getProgressTaskMapEntities(projectProgressEntity.getId(), projectProgress.getTasks()));
        }
        projectProgressTaskMapRepository.saveAll(progressTaskMapEntities);
        return IdResponse.builder().id(project.getId()).build();
    }

    @Override
    public ProjectInfoDto getDetail(Long id) {
        ProjectEntity project = projectRepository.getProjectById(id);
        Set<Long> staffIds = new HashSet<>();
        ProjectInfoDto projectInfoDto = projectMapper.toInfoDto(project);
        addIfNotNull(staffIds, project.getCreatorId());
        addIfNotNull(staffIds, project.getManagerId());
        addIfNotNull(staffIds, project.getSupporterId());
        Map<Long, CodeNameResponse> staffMap = staffRepository.getMapSimpleStaffByIds(staffIds);
        projectInfoDto.setCreator(staffMap.get(project.getCreatorId()));
        projectInfoDto.setManager(staffMap.get(project.getManagerId()));
        projectInfoDto.setSupporter(staffMap.get(project.getSupporterId()));
        projectInfoDto.setProjectProgress(getProjectProgress(projectProgressRepository.getListByProjectId(project.getId())));
        return projectInfoDto;
    }

    private List<ProjectProgressTaskMapEntity> getProgressTaskMapEntities(Long projectProgressId,
                                                                          List<TaskOutput> tasks) {
        if (CollectionUtils.isEmpty(tasks)) return new ArrayList<>();
        List<ProjectProgressTaskMapEntity> projectProgressTaskMapEntities = new ArrayList<>();
        for (TaskOutput task : tasks) {
            ProjectProgressTaskMapEntity projectProgressTaskMap = ProjectProgressTaskMapEntity.builder()
                    .projectProgressId(projectProgressId)
                    .taskId(task.getId())
                    .build();
            projectProgressTaskMapEntities.add(projectProgressTaskMap);
        }
        return projectProgressTaskMapEntities;
    }

    private List<ProjectProgressDto> getProjectProgress(List<ProjectProgressEntity> projectProgressEntities) {
        Set<Long> projectProgressIds = new HashSet<>();
        Set<Long> progressIds = new HashSet<>();
        for (ProjectProgressEntity projectProgressEntity : projectProgressEntities) {
            addIfNotNull(projectProgressIds, projectProgressEntity.getId());
            addIfNotNull(progressIds, projectProgressEntity.getProgressId());
        }
        Map<Long, CodeNameResponse> progressMap = progressRepository.getMapProgressSimpleByIds(progressIds);
        Map<Long, List<TaskOutput>> taskOutputMap = getTaskOutputMapGroupByProgressId(projectProgressTaskMapRepository.getListByProjectProgressIds(projectProgressIds));
        List<ProjectProgressDto> projectProgress = new ArrayList<>();
        for (ProjectProgressEntity projectProgressEntity : projectProgressEntities) {
            ProjectProgressDto projectProgressDto = ProjectProgressDto.builder()
                    .progress(getData(progressMap, projectProgressEntity.getProgressId()))
                    .tasks(getData(taskOutputMap, projectProgressEntity.getId()))
                    .build();
            projectProgress.add(projectProgressDto);
        }
        return projectProgress;
    }

    private Map<Long, List<TaskOutput>> getTaskOutputMapGroupByProgressId(List<ProjectProgressTaskMapEntity> progressTaskMapEntities) {
        Set<Long> taskIds = new HashSet<>();
        for (ProjectProgressTaskMapEntity progressTaskMapEntity : progressTaskMapEntities) {
            addIfNotNull(taskIds, progressTaskMapEntity.getTaskId());
        }
        List<TaskEntity> taskEntities = taskRepository.findAllByIds(taskIds);
        List<TaskStaffMapEntity> taskStaffMapEntities = taskStaffMapRepository.getListByTaskIds(taskIds);
        DataCollect dataCollect = internalTaskService.getTaskDataCollectByIds(taskEntities, taskStaffMapEntities);
        Map<Long, List<TaskOutput>> taskOutputMap = new HashMap<>();
        for (ProjectProgressTaskMapEntity progressTaskMapEntity : progressTaskMapEntities) {
            taskOutputMap.computeIfAbsent(progressTaskMapEntity.getProjectProgressId(), k -> new ArrayList<>())
                    .add(buildTaskOutput(getData(dataCollect.getTaskEntityMap(), progressTaskMapEntity.getTaskId()), dataCollect));
        }
        return taskOutputMap;
    }

    private TaskOutput buildTaskOutput(TaskEntity taskEntity,
                                       DataCollect dataCollect) {
        TaskOutput taskOutput = taskMapper.toOutput(taskEntity);
        taskOutput.setReviewer(getReviewerDto(getData(dataCollect.getStaffEntityMap(), taskEntity.getReviewerId())));
        taskOutput.setStaffs(getData(dataCollect.getStaffSimpleListMap(), taskEntity.getId()));
        return taskOutput;
    }

    private ReviewerDto getReviewerDto(StaffEntity staffEntity) {
        if (staffEntity == null) return null;
        return ReviewerDto.builder()
                .id(staffEntity.getId())
                .name(staffEntity.getName())
                .email(staffEntity.getEmail())
                .avatar(staffEntity.getAvatar())
                .firstName(staffEntity.getFirstName())
                .lastName(staffEntity.getLastName())
                .build();
    }
}

package com.backend.constructor.core.service;

import com.backend.constructor.app.api.ProjectApi;
import com.backend.constructor.app.dto.project.*;
import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.core.domain.entity.ProjectCategoryMapEntity;
import com.backend.constructor.core.domain.entity.ProjectEntity;
import com.backend.constructor.core.domain.entity.ProjectLineEntity;
import com.backend.constructor.core.domain.enums.ProjectState;
import com.backend.constructor.core.port.mapper.ProjectLineMapper;
import com.backend.constructor.core.port.mapper.ProjectMapper;
import com.backend.constructor.core.port.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.backend.constructor.core.service.HelperService.addIfNotNull;
import static com.backend.constructor.core.service.HelperService.getData;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService implements ProjectApi {
    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final ProjectLineMapper projectLineMapper;
    private final ProjectLineRepository projectLineRepository;
    private final ProjectCategoryMapRepository projectCategoryMapRepository;
    private final StaffRepository staffRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public IdResponse create(ProjectDto input) {
        input.trimData();
        ProjectEntity projectEntity = projectMapper.toEntity(input);
        projectEntity.setState(ProjectState.NOT_STARTED);
        projectRepository.save(projectEntity);
        saveProjectLines(projectEntity, input.getProjectLines());
        saveProjectCategoryMaps(projectEntity, input.getProjectCategoryMaps());
        return IdResponse.builder().id(projectEntity.getId()).build();
    }

    @Override
    @Transactional
    public IdResponse update(ProjectDto input) {
        ProjectEntity projectEntity = projectRepository.getProjectById(input.getId());
        input.trimData();
        projectMapper.update(input, projectEntity);
        projectRepository.save(projectEntity);
        deleteAllRelations(projectEntity);
        saveProjectLines(projectEntity, input.getProjectLines());
        saveProjectCategoryMaps(projectEntity, input.getProjectCategoryMaps());
        return IdResponse.builder().id(projectEntity.getId()).build();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ProjectEntity projectEntity = projectRepository.getProjectById(id);
        deleteAllRelations(projectEntity);
        projectRepository.delete(projectEntity);
    }

    @Override
    public ProjectDto getDetail(Long id) {
        ProjectEntity projectEntity = projectRepository.getProjectById(id);
        ProjectDto projectDto = projectMapper.toDto(projectEntity);
        Set<Long> staffIds = new HashSet<>();
        addIfNotNull(staffIds, projectEntity.getCreatorId());
        addIfNotNull(staffIds, projectEntity.getManagerId());
        addIfNotNull(staffIds, projectEntity.getSupporterId());
        Map<Long, CodeNameResponse> staffMap = staffRepository.getMapSimpleStaffByIds(staffIds);
        projectDto.setCreator(staffMap.get(projectEntity.getCreatorId()));
        projectDto.setManager(staffMap.get(projectEntity.getManagerId()));
        projectDto.setSupporter(staffMap.get(projectEntity.getSupporterId()));
        List<ProjectLineEntity> projectLineEntities = projectLineRepository.getListByProjectId(projectEntity.getId());
        List<ProjectCategoryMapEntity> projectCategoryMapEntities = projectCategoryMapRepository.getListByProjectId(projectEntity.getId());
        setProjectCategoryMaps(projectDto, projectCategoryMapEntities);
        setProjectCategoryLines(projectDto, projectLineEntities);
        return projectDto;
    }

    @Override
    public Page<ProjectOutput> getListProject(ProjectFilterParam param, Pageable pageable) {
        return null;
    }

    private void saveProjectLines(ProjectEntity projectEntity,
                                  List<ProjectLineDto> projectLines) {
        List<ProjectLineEntity> projectLineEntities = new ArrayList<>();
        for (ProjectLineDto projectLineDto : projectLines) {
            ProjectLineEntity entity = projectLineMapper.toEntity(projectLineDto);
            entity.setProjectId(projectEntity.getId());
            projectLineEntities.add(entity);
        }
        projectLineRepository.saveAll(projectLineEntities);
    }

    private void saveProjectCategoryMaps(ProjectEntity projectEntity,
                                         List<ProjectCategoryMapDto> projectCategoryMaps) {
        List<ProjectCategoryMapEntity> projectCategoryMapEntities = new ArrayList<>();
        for (ProjectCategoryMapDto projectCategoryMap : projectCategoryMaps) {
            CodeNameResponse category = projectCategoryMap.getCategory();
            if (category == null) {
                throw BusinessException.exception("CST015");
            }
            ProjectCategoryMapEntity projectCategoryMapEntity = ProjectCategoryMapEntity.builder()
                    .projectId(projectEntity.getId())
                    .categoryId(category.getId())
                    .build();
            projectCategoryMapEntities.add(projectCategoryMapEntity);
        }
        projectCategoryMapRepository.saveAll(projectCategoryMapEntities);
    }

    private void deleteAllRelations(ProjectEntity projectEntity) {
        List<ProjectLineEntity> projectLineEntities = projectLineRepository.getListByProjectId(projectEntity.getId());
        List<ProjectCategoryMapEntity> projectCategoryMapEntities = projectCategoryMapRepository.getListByProjectId(projectEntity.getId());
        projectLineRepository.deleteAllInBatch(projectLineEntities);
        projectCategoryMapRepository.deleteAllInBatch(projectCategoryMapEntities);
    }

    private void setProjectCategoryMaps(ProjectDto projectDto,
                                        List<ProjectCategoryMapEntity> projectCategoryMapEntities) {
        Set<Long> categoryIds = new HashSet<>();
        for (ProjectCategoryMapEntity projectCategoryMapEntity : projectCategoryMapEntities) {
            addIfNotNull(categoryIds, projectCategoryMapEntity.getCategoryId());
        }
        Map<Long, CodeNameResponse> categoryMap = categoryRepository.getMapSimpleCategoryByIds(categoryIds);
        List<ProjectCategoryMapDto> projectCategoryMaps = new ArrayList<>();
        for (ProjectCategoryMapEntity projectCategoryMapEntity : projectCategoryMapEntities) {
            ProjectCategoryMapDto projectCategoryMapDto = ProjectCategoryMapDto.builder()
                    .id(projectCategoryMapEntity.getId())
                    .category(getData(categoryMap, projectCategoryMapEntity.getCategoryId()))
                    .build();
            projectCategoryMaps.add(projectCategoryMapDto);
        }
        projectDto.setProjectCategoryMaps(projectCategoryMaps);
    }

    private void setProjectCategoryLines(ProjectDto projectDto,
                                         List<ProjectLineEntity> projectLineEntities) {
        List<ProjectLineDto> projectLines = projectLineEntities.stream()
                .map(projectLineMapper::toDto)
                .toList();
        projectDto.setProjectLines(projectLines);
    }
}

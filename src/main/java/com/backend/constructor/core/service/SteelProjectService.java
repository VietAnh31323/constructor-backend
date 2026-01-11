package com.backend.constructor.core.service;

import com.backend.constructor.app.api.SteelProjectApi;
import com.backend.constructor.app.dto.steel.SteelOutput;
import com.backend.constructor.app.dto.steel_project.SteelProjectAssemblyMapDto;
import com.backend.constructor.app.dto.steel_project.SteelProjectDto;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.common.service.GenerateCodeService;
import com.backend.constructor.core.domain.constant.Constants;
import com.backend.constructor.core.domain.entity.SteelProjectAssemblyMapEntity;
import com.backend.constructor.core.domain.entity.SteelProjectEntity;
import com.backend.constructor.core.port.mapper.SteelProjectMapper;
import com.backend.constructor.core.port.repository.SteelProjectAssemblyMapRepository;
import com.backend.constructor.core.port.repository.SteelProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SteelProjectService implements SteelProjectApi {
    private final SteelProjectRepository steelProjectRepository;
    private final SteelProjectMapper steelProjectMapper;
    private final GenerateCodeService generateCodeService;
    private final SteelProjectAssemblyMapRepository steelProjectAssemblyMapRepository;

    @Override
    @Transactional
    public IdResponse create(SteelProjectDto input) {
        generateCodeService.generateCode(input, Constants.DATKT, SteelProjectEntity.class);
        SteelProjectEntity steelProject = steelProjectMapper.toEntity(input);
        steelProjectRepository.save(steelProject);
        saveSteelProjectAssemblyMaps(input.getSteelProjectAssemblyMaps(), steelProject.getId());
        return IdResponse.builder().id(steelProject.getId()).build();
    }

    @Override
    @Transactional
    public IdResponse update(SteelProjectDto input) {
        SteelProjectEntity steelProject = steelProjectRepository.getSteelProjectById(input.getId());
        generateCodeService.generateCode(input, Constants.DATKT, SteelProjectEntity.class);
        steelProjectMapper.update(input, steelProject);
        steelProjectRepository.save(steelProject);
        clearSteelProjectAssemblyMaps(steelProject.getId());
        saveSteelProjectAssemblyMaps(input.getSteelProjectAssemblyMaps(), steelProject.getId());
        return IdResponse.builder().id(steelProject.getId()).build();
    }

    @Override
    public SteelProjectDto getDetail(Long id) {
        SteelProjectEntity steelProject = steelProjectRepository.getSteelProjectById(id);
        SteelProjectDto steelProjectDto = steelProjectMapper.toDto(steelProject);
        List<SteelProjectAssemblyMapEntity> steelProjectAssemblyMapEntities = steelProjectAssemblyMapRepository.getListBySteelProjectId(steelProject.getId());
        steelProjectDto.setSteelProjectAssemblyMaps(getSteelProjectAssemblyMapDto(steelProjectAssemblyMapEntities));
        return steelProjectDto;
    }

    private void saveSteelProjectAssemblyMaps(List<SteelProjectAssemblyMapDto> steelProjectAssemblyMaps,
                                              Long steelProjectId) {
        List<SteelProjectAssemblyMapEntity> steelProjectAssemblyMapEntities = new ArrayList<>();
        for (SteelProjectAssemblyMapDto steelProjectAssemblyMap : steelProjectAssemblyMaps) {
            SteelProjectAssemblyMapEntity steelProjectAssemblyMapEntity = SteelProjectAssemblyMapEntity
                    .builder()
                    .steelProjectId(steelProjectId)
                    .assemblyName(steelProjectAssemblyMap.getAssemblyName())
                    .steelIds(steelProjectAssemblyMap.getSteels().stream().map(SteelOutput::getId).toList())
                    .build();
            steelProjectAssemblyMapEntities.add(steelProjectAssemblyMapEntity);
        }
        steelProjectAssemblyMapRepository.saveAll(steelProjectAssemblyMapEntities);
    }

    private void clearSteelProjectAssemblyMaps(Long steelProjectId) {
        List<SteelProjectAssemblyMapEntity> projectAssemblyMapEntities = steelProjectAssemblyMapRepository.getListBySteelProjectId(steelProjectId);
        steelProjectAssemblyMapRepository.deleteAll(projectAssemblyMapEntities);
    }

    private List<SteelProjectAssemblyMapDto> getSteelProjectAssemblyMapDto(List<SteelProjectAssemblyMapEntity> steelProjectAssemblyMapEntities) {
        List<SteelProjectAssemblyMapDto> steelProjectAssemblyMaps = new ArrayList<>();
        for (SteelProjectAssemblyMapEntity steelProjectAssemblyMapEntity : steelProjectAssemblyMapEntities) {
            SteelProjectAssemblyMapDto steelProjectAssemblyMapDto = SteelProjectAssemblyMapDto.builder()
                    .id(steelProjectAssemblyMapEntity.getId())
                    .assemblyName(steelProjectAssemblyMapEntity.getAssemblyName())
                    .sameQuantity(steelProjectAssemblyMapEntity.getSameQuantity())
                    .build();
            steelProjectAssemblyMaps.add(steelProjectAssemblyMapDto);
        }
        return steelProjectAssemblyMaps;
    }
}

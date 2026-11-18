package com.backend.constructor.core.service;

import com.backend.constructor.app.api.SteelApi;
import com.backend.constructor.app.dto.steel.SteelDto;
import com.backend.constructor.app.dto.steel.SteelLineDto;
import com.backend.constructor.app.dto.steel.SteelOutput;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.core.domain.entity.SteelEntity;
import com.backend.constructor.core.domain.entity.SteelLineEntity;
import com.backend.constructor.core.domain.entity.SteelProjectAssemblyMapEntity;
import com.backend.constructor.core.port.mapper.SteelMapper;
import com.backend.constructor.core.port.repository.SteelLineRepository;
import com.backend.constructor.core.port.repository.SteelProjectAssemblyMapRepository;
import com.backend.constructor.core.port.repository.SteelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SteelService implements SteelApi {
    private final SteelRepository steelRepository;
    private final SteelMapper steelMapper;
    private final SteelLineRepository steelLineRepository;
    private final SteelProjectAssemblyMapRepository steelProjectAssemblyMapRepository;
    private final SteelCalculationService steelCalculationService;
    private final com.backend.constructor.infras.repository.jpa.SteelJpaRepository steelJpaRepository;

    @Override
    @Transactional
    public IdResponse create(SteelDto input) {
        SteelEntity steelEntity = steelMapper.toEntity(input);
        steelRepository.save(steelEntity);
        List<SteelLineEntity> savedLines = saveSteelLines(steelEntity, input.getSteelLines());
        Long sameQty = resolveSameQuantity(steelEntity.getAssemblyMapId());
        steelCalculationService.calculateBar(steelEntity, savedLines, sameQty);
        steelRepository.save(steelEntity);
        return IdResponse.builder().id(steelEntity.getId()).build();
    }

    @Override
    @Transactional
    public IdResponse update(SteelDto input) {
        SteelEntity steelEntity = steelRepository.getSteelById(input.getId());
        steelMapper.update(input, steelEntity);
        steelRepository.save(steelEntity);
        clearSteelLines(steelEntity.getId());
        List<SteelLineEntity> savedLines = saveSteelLines(steelEntity, input.getSteelLines());
        Long sameQty = resolveSameQuantity(steelEntity.getAssemblyMapId());
        steelCalculationService.calculateBar(steelEntity, savedLines, sameQty);
        steelRepository.save(steelEntity);
        return IdResponse.builder().id(steelEntity.getId()).build();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SteelEntity steelEntity = steelRepository.getSteelById(id);
        clearSteelLines(steelEntity.getId());
        steelRepository.delete(steelEntity);
    }

    @Override
    public SteelDto getDetail(Long id) {
        SteelEntity steelEntity = steelRepository.getSteelById(id);
        SteelDto steelDto = steelMapper.toDto(steelEntity);
        List<SteelLineEntity> steelLineEntities = steelLineRepository.getListBySteelId(steelEntity.getId());
        steelDto.setSteelLines(getSteelLines(steelLineEntities));
        return steelDto;
    }

    @Override
    public List<SteelOutput> getListSteelByAssemblyId(Long assemblyId) {
        List<SteelEntity> steelEntities = steelJpaRepository.findAllByAssemblyMapIdOrderBySortOrderAscIdAsc(assemblyId);
        if (steelEntities.isEmpty()) {
            SteelProjectAssemblyMapEntity steelProjectAssemblyMapEntity = steelProjectAssemblyMapRepository.getSteelProjectAssemblyMapById(assemblyId);
            if (steelProjectAssemblyMapEntity.getSteelIds() != null && !steelProjectAssemblyMapEntity.getSteelIds().isEmpty()) {
                steelEntities = steelRepository.findAllByIds(steelProjectAssemblyMapEntity.getSteelIds());
            }
        }
        return steelEntities.stream().map(steelMapper::toOutput).toList();
    }

    private Long resolveSameQuantity(Long assemblyMapId) {
        if (assemblyMapId == null) {
            return 1L;
        }
        SteelProjectAssemblyMapEntity assembly = steelProjectAssemblyMapRepository.getSteelProjectAssemblyMapById(assemblyMapId);
        return assembly.getSameQuantity() != null && assembly.getSameQuantity() > 0 ? assembly.getSameQuantity() : 1L;
    }

    private List<SteelLineEntity> saveSteelLines(SteelEntity steelEntity,
                                                 List<SteelLineDto> steelLines) {
        List<SteelLineEntity> steelLineEntities = new ArrayList<>();
        BigDecimal length = BigDecimal.ZERO;
        if (steelLines != null) {
            for (SteelLineDto steelLine : steelLines) {
                BigDecimal val = steelLine.getValue() != null ? steelLine.getValue() : BigDecimal.ZERO;
                SteelLineEntity steelLineEntity = SteelLineEntity.builder()
                        .steelId(steelEntity.getId())
                        .paramName(steelLine.getParamName())
                        .value(val)
                        .build();
                steelLineEntities.add(steelLineEntity);
                length = length.add(val);
            }
            steelLineEntities = steelLineRepository.saveAll(steelLineEntities);
        }
        steelEntity.setLength(length);
        return steelLineEntities;
    }

    private void clearSteelLines(Long steelId) {
        List<SteelLineEntity> steelLineEntities = steelLineRepository.getListBySteelId(steelId);
        steelLineRepository.deleteAll(steelLineEntities);
    }

    private List<SteelLineDto> getSteelLines(List<SteelLineEntity> steelLineEntities) {
        List<SteelLineDto> steelLines = new ArrayList<>();
        for (SteelLineEntity steelLineEntity : steelLineEntities) {
            SteelLineDto steelLineDto = SteelLineDto.builder()
                    .id(steelLineEntity.getId())
                    .paramName(steelLineEntity.getParamName())
                    .value(steelLineEntity.getValue())
                    .build();
            steelLines.add(steelLineDto);
        }
        return steelLines;
    }
}

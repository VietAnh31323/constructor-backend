package com.backend.constructor.core.service;

import com.backend.constructor.app.api.SteelApi;
import com.backend.constructor.app.dto.steel.SteelDto;
import com.backend.constructor.app.dto.steel.SteelLineDto;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.core.domain.entity.SteelEntity;
import com.backend.constructor.core.domain.entity.SteelLineEntity;
import com.backend.constructor.core.port.mapper.SteelMapper;
import com.backend.constructor.core.port.repository.SteelLineRepository;
import com.backend.constructor.core.port.repository.SteelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SteelService implements SteelApi {
    private final SteelRepository steelRepository;
    private final SteelMapper steelMapper;
    private final SteelLineRepository steelLineRepository;

    @Override
    @Transactional
    public IdResponse create(SteelDto input) {
        SteelEntity steelEntity = steelMapper.toEntity(input);
        steelRepository.save(steelEntity);
        saveSteelLines(steelEntity.getId(), input.getSteelLines());
        return IdResponse.builder().id(steelEntity.getId()).build();
    }

    @Override
    @Transactional
    public IdResponse update(SteelDto input) {
        SteelEntity steelEntity = steelRepository.getSteelById(input.getId());
        steelMapper.update(input, steelEntity);
        steelRepository.save(steelEntity);
        clearSteelLines(steelEntity.getId());
        saveSteelLines(steelEntity.getId(), input.getSteelLines());
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
        return null;
    }

    private void saveSteelLines(Long steelId,
                                List<SteelLineDto> steelLines) {
        List<SteelLineEntity> steelLineEntities = new ArrayList<>();
        for (SteelLineDto steelLine : steelLines) {
            SteelLineEntity steelLineEntity = SteelLineEntity.builder()
                    .steelId(steelId)
                    .paramName(steelLine.getParamName())
                    .value(steelLine.getValue())
                    .build();
            steelLineEntities.add(steelLineEntity);
        }
        steelLineRepository.saveAll(steelLineEntities);
    }

    private void clearSteelLines(Long steelId) {
        List<SteelLineEntity> steelLineEntities = steelLineRepository.getListBySteelId(steelId);
        steelLineRepository.deleteAll(steelLineEntities);
    }
}

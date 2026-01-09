package com.backend.constructor.core.service;

import com.backend.constructor.app.api.SteelCategoryApi;
import com.backend.constructor.app.dto.steel_category.SteelCategoryDto;
import com.backend.constructor.app.dto.steel_category.SteelCategoryFilterParam;
import com.backend.constructor.app.dto.steel_category.SteelCategoryLineDto;
import com.backend.constructor.app.dto.steel_category.SteelCategoryOutput;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.common.service.GenerateCodeService;
import com.backend.constructor.common.validator.UniqueValidationService;
import com.backend.constructor.core.domain.constant.Constants;
import com.backend.constructor.core.domain.entity.SteelCategoryEntity;
import com.backend.constructor.core.domain.entity.SteelCategoryLineEntity;
import com.backend.constructor.core.port.mapper.SteelCategoryMapper;
import com.backend.constructor.core.port.repository.SteelCategoryLineRepository;
import com.backend.constructor.core.port.repository.SteelCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SteelCategoryService implements SteelCategoryApi {
    private final SteelCategoryRepository steelCategoryRepository;
    private final GenerateCodeService generateCodeService;
    private final SteelCategoryMapper steelCategoryMapper;
    private final UniqueValidationService uniqueValidationService;
    private final SteelCategoryLineRepository steelCategoryLineRepository;

    @Override
    @Transactional
    public IdResponse create(SteelCategoryDto input) {
        input.trimData();
        generateCodeService.generateCode(input, Constants.MTT, SteelCategoryEntity.class);
        SteelCategoryEntity steelCategoryEntity = steelCategoryMapper.toEntity(input);
        uniqueValidationService.validate(steelCategoryEntity);
        steelCategoryRepository.save(steelCategoryEntity);
        saveSteelCategoryLines(input.getSteelCategoryLines(), steelCategoryEntity.getId());
        return IdResponse.builder().id(steelCategoryEntity.getId()).build();
    }

    @Override
    @Transactional
    public IdResponse update(SteelCategoryDto input) {
        input.trimData();
        generateCodeService.generateCode(input, Constants.MTT, SteelCategoryEntity.class);
        SteelCategoryEntity steelCategoryEntity = steelCategoryRepository.getSteelCategoryById(input.getId());
        steelCategoryMapper.update(input, steelCategoryEntity);
        uniqueValidationService.validate(steelCategoryEntity);
        steelCategoryRepository.save(steelCategoryEntity);
        clearSteelCategoryLines(steelCategoryEntity.getId());
        saveSteelCategoryLines(input.getSteelCategoryLines(), steelCategoryEntity.getId());
        return IdResponse.builder().id(steelCategoryEntity.getId()).build();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SteelCategoryEntity steelCategoryEntity = steelCategoryRepository.getSteelCategoryById(id);
        if (Boolean.TRUE.equals(steelCategoryEntity.getIsActive())) {
            throw BusinessException.exception("ERROR_0007");
        }
        clearSteelCategoryLines(steelCategoryEntity.getId());
        steelCategoryRepository.delete(steelCategoryEntity);
    }

    @Override
    public SteelCategoryDto getDetail(Long id) {
        SteelCategoryEntity steelCategoryEntity = steelCategoryRepository.getSteelCategoryById(id);
        SteelCategoryDto steelCategoryDto = steelCategoryMapper.toDto(steelCategoryEntity);
        List<SteelCategoryLineEntity> steelCategoryLineEntities = steelCategoryLineRepository.getListByCategoryId(steelCategoryEntity.getId());
        List<SteelCategoryLineDto> steelCategoryLines = new ArrayList<>();
        for (SteelCategoryLineEntity steelCategoryLineEntity : steelCategoryLineEntities) {
            SteelCategoryLineDto steelCategoryLineDto = SteelCategoryLineDto.builder()
                    .id(steelCategoryLineEntity.getId())
                    .paramName(steelCategoryLineEntity.getParamName())
                    .build();
            steelCategoryLines.add(steelCategoryLineDto);
        }
        steelCategoryDto.setSteelCategoryLines(steelCategoryLines);
        return steelCategoryDto;
    }

    @Override
    public Page<SteelCategoryOutput> getListSteelCategory(SteelCategoryFilterParam param,
                                                          Pageable pageable) {
        Page<SteelCategoryEntity> steelCategoryEntities = steelCategoryRepository.getPageSteelCategory(param, pageable);
        return steelCategoryEntities.map(steelCategoryMapper::toOutput);
    }

    @Override
    public List<SteelCategoryLineDto> getListSteelCategoryLine(Long steelCategoryId) {
        List<SteelCategoryLineEntity> steelCategoryLineEntities = steelCategoryLineRepository.getListByCategoryId(steelCategoryId);
        return steelCategoryLineEntities.stream().map(this::buildSteelCategoryLineDto).toList();
    }

    private SteelCategoryLineDto buildSteelCategoryLineDto(SteelCategoryLineEntity entity) {
        return SteelCategoryLineDto.builder()
                .id(entity.getId())
                .paramName(entity.getParamName())
                .build();
    }

    private void saveSteelCategoryLines(List<SteelCategoryLineDto> steelCategoryLines,
                                        Long steelCategoryId) {
        List<SteelCategoryLineEntity> steelCategoryLineEntities = new ArrayList<>();
        for (SteelCategoryLineDto steelCategoryLine : steelCategoryLines) {
            SteelCategoryLineEntity steelCategoryEntity = SteelCategoryLineEntity.builder()
                    .steelCategoryId(steelCategoryId)
                    .paramName(steelCategoryLine.getParamName())
                    .build();
            steelCategoryLineEntities.add(steelCategoryEntity);
        }
        steelCategoryLineRepository.saveAll(steelCategoryLineEntities);
    }

    private void clearSteelCategoryLines(Long steelCategoryId) {
        List<SteelCategoryLineEntity> steelCategoryLineEntities = steelCategoryLineRepository.getListByCategoryId(steelCategoryId);
        steelCategoryLineRepository.deleteAll(steelCategoryLineEntities);
    }
}

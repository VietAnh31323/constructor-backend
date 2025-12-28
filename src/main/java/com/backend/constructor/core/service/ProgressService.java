package com.backend.constructor.core.service;

import com.backend.constructor.app.api.ProgressApi;
import com.backend.constructor.app.dto.progress.ProgressDto;
import com.backend.constructor.app.dto.progress.ProgressFilterParam;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.common.service.GenerateCodeService;
import com.backend.constructor.common.validator.UniqueValidationService;
import com.backend.constructor.core.domain.constant.Constants;
import com.backend.constructor.core.domain.entity.ProgressEntity;
import com.backend.constructor.core.port.mapper.ProgressMapper;
import com.backend.constructor.core.port.repository.ProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgressService implements ProgressApi {
    private final ProgressRepository progressRepository;
    private final ProgressMapper progressMapper;
    private final GenerateCodeService generateCodeService;
    private final UniqueValidationService uniqueValidationService;

    @Override
    @Transactional
    public IdResponse create(ProgressDto input) {
        input.trimData();
        generateCodeService.generateCode(input, Constants.TT, ProgressEntity.class);
        input.setId(null);
        ProgressEntity progressEntity = progressMapper.toEntity(input);
        uniqueValidationService.validate(progressEntity);
        progressRepository.save(progressEntity);
        return IdResponse.builder().id(progressEntity.getId()).build();
    }

    @Override
    @Transactional
    public IdResponse update(ProgressDto input) {
        input.trimData();
        generateCodeService.generateCode(input, Constants.TT, ProgressEntity.class);
        ProgressEntity progressEntity = progressRepository.getProgressById(input.getId());
        progressMapper.update(input, progressEntity);
        uniqueValidationService.validate(progressEntity);
        progressRepository.save(progressEntity);
        return IdResponse.builder().id(progressEntity.getId()).build();
    }

    @Override
    @Transactional
    public IdResponse delete(Long id) {
        ProgressEntity progressEntity = progressRepository.getProgressById(id);
        if (Boolean.TRUE.equals(progressEntity.getIsActive())) {
            throw BusinessException.exception("ERROR_0007");
        }
        progressRepository.delete(progressEntity);
        return IdResponse.builder().id(progressEntity.getId()).build();
    }

    @Override
    public ProgressDto getDetail(Long id) {
        ProgressEntity progressEntity = progressRepository.getProgressById(id);
        return progressMapper.toDto(progressEntity);
    }

    @Override
    public Page<ProgressDto> getListStaff(ProgressFilterParam param, Pageable pageable) {
        Page<ProgressEntity> progressEntities = progressRepository.getPageProgress(param, pageable);
        return progressEntities.map(progressMapper::toDto);
    }
}

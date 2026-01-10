package com.backend.constructor.core.service;

import com.backend.constructor.app.api.AssemblyApi;
import com.backend.constructor.app.dto.assembly.AssemblyDto;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.common.service.GenerateCodeService;
import com.backend.constructor.core.domain.constant.Constants;
import com.backend.constructor.core.domain.entity.AssemblyEntity;
import com.backend.constructor.core.port.repository.AssemblyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssemblyService implements AssemblyApi {
    private final AssemblyRepository assemblyRepository;
    private final GenerateCodeService generateCodeService;

    @Override
    @Transactional
    public IdResponse create(AssemblyDto input) {
        generateCodeService.generateCode(input, Constants.CK, AssemblyEntity.class);
        AssemblyEntity assemblyEntity = AssemblyEntity.builder()
                .code(input.getCode())
                .name(input.getName())
                .build();
        assemblyRepository.save(assemblyEntity);
        return IdResponse.builder().id(assemblyEntity.getId()).build();
    }

    @Override
    @Transactional
    public IdResponse update(AssemblyDto input) {
        generateCodeService.generateCode(input, Constants.CK, AssemblyEntity.class);
        AssemblyEntity assemblyEntity = assemblyRepository.getAssemblyById(input.getId());
        assemblyEntity.setCode(input.getCode());
        assemblyEntity.setName(input.getName());
        assemblyRepository.save(assemblyEntity);
        return IdResponse.builder().id(assemblyEntity.getId()).build();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AssemblyEntity assemblyEntity = assemblyRepository.getAssemblyById(id);
        assemblyRepository.delete(assemblyEntity);
    }

    @Override
    public Page<AssemblyDto> getListAssembly(String search, Pageable pageable) {
        Page<AssemblyEntity> assemblyEntities = assemblyRepository.getPageAssembly(search, pageable);
        return assemblyEntities.map(entity -> AssemblyDto.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .build());
    }
}

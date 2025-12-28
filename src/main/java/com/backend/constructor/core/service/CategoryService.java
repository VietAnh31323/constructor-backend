package com.backend.constructor.core.service;

import com.backend.constructor.app.api.CategoryApi;
import com.backend.constructor.app.dto.category.CategoryDto;
import com.backend.constructor.app.dto.category.CategoryFilterParam;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.common.service.GenerateCodeService;
import com.backend.constructor.common.validator.UniqueValidationService;
import com.backend.constructor.core.domain.constant.Constants;
import com.backend.constructor.core.domain.entity.CategoryEntity;
import com.backend.constructor.core.domain.entity.StaffEntity;
import com.backend.constructor.core.port.mapper.CategoryMapper;
import com.backend.constructor.core.port.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService implements CategoryApi {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final GenerateCodeService generateCodeService;
    private final UniqueValidationService uniqueValidationService;

    @Override
    @Transactional
    public IdResponse create(CategoryDto input) {
        input.trimData();
        generateCodeService.generateCode(input, Constants.DM, CategoryEntity.class);
        input.setId(null);
        CategoryEntity categoryEntity = categoryMapper.toEntity(input);
        uniqueValidationService.validate(categoryEntity);
        categoryRepository.save(categoryEntity);
        return IdResponse.builder().id(categoryEntity.getId()).build();
    }

    @Override
    @Transactional
    public IdResponse update(CategoryDto input) {
        input.trimData();
        generateCodeService.generateCode(input, Constants.DM, StaffEntity.class);
        CategoryEntity categoryEntity = categoryRepository.getCategoryById(input.getId());
        categoryMapper.update(input, categoryEntity);
        uniqueValidationService.validate(categoryEntity);
        categoryRepository.save(categoryEntity);
        return IdResponse.builder().id(categoryEntity.getId()).build();
    }

    @Override
    @Transactional
    public IdResponse delete(Long id) {
        CategoryEntity categoryEntity = categoryRepository.getCategoryById(id);
        if (Boolean.TRUE.equals(categoryEntity.getIsActive())) {
            throw BusinessException.exception("ERROR_0007");
        }
        categoryRepository.delete(categoryEntity);
        return IdResponse.builder().id(categoryEntity.getId()).build();
    }

    @Override
    public CategoryDto getDetail(Long id) {
        CategoryEntity categoryEntity = categoryRepository.getCategoryById(id);
        return categoryMapper.toDto(categoryEntity);
    }

    @Override
    public Page<CategoryDto> getListStaff(CategoryFilterParam param, Pageable pageable) {
        Page<CategoryEntity> pageCategory = categoryRepository.getPageCategory(param, pageable);
        return pageCategory.map(categoryMapper::toDto);
    }
}

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
import com.backend.constructor.core.port.mapper.CategoryMapper;
import com.backend.constructor.core.port.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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
        if (categoryEntity.getParentId() != null) {
            categoryRepository.getCategoryById(categoryEntity.getParentId());
        }
        uniqueValidationService.validate(categoryEntity);
        categoryRepository.save(categoryEntity);
        return IdResponse.builder().id(categoryEntity.getId()).build();
    }

    @Override
    @Transactional
    public IdResponse update(CategoryDto input) {
        input.trimData();
        generateCodeService.generateCode(input, Constants.DM, CategoryEntity.class);
        CategoryEntity categoryEntity = categoryRepository.getCategoryById(input.getId());
        if (input.getParentId() != null && Objects.equals(input.getParentId(), categoryEntity.getId())) {
            throw BusinessException.exception("CST002");
        }
        if (input.getParentId() != null) {
            categoryRepository.getCategoryById(input.getParentId());
        }
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
        CategoryDto dto = categoryMapper.toDto(categoryEntity);
        if (categoryEntity.getParentId() != null) {
            categoryRepository.findById(categoryEntity.getParentId())
                    .ifPresent(parent -> dto.setParentName(parent.getName()));
        }
        return dto;
    }

    @Override
    public Page<CategoryDto> getListCategory(CategoryFilterParam param, Pageable pageable) {
        Page<CategoryEntity> pageCategory = categoryRepository.getPageCategory(param, pageable);
        Set<Long> parentIds = pageCategory.getContent().stream()
                .map(CategoryEntity::getParentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> parentNames = parentIds.isEmpty()
                ? Collections.emptyMap()
                : categoryRepository.findAllByIds(new ArrayList<>(parentIds)).stream()
                        .collect(Collectors.toMap(CategoryEntity::getId, CategoryEntity::getName));
        return pageCategory.map(entity -> {
            CategoryDto dto = categoryMapper.toDto(entity);
            if (entity.getParentId() != null) {
                dto.setParentName(parentNames.get(entity.getParentId()));
            }
            return dto;
        });
    }

    @Override
    public List<CategoryDto> getTreeCategory() {
        List<CategoryEntity> all = categoryRepository.findAll();
        all.sort(Comparator.comparing(CategoryEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(CategoryEntity::getId, Comparator.nullsLast(Long::compareTo)));

        Map<Long, CategoryDto> dtoMap = new LinkedHashMap<>();
        for (CategoryEntity entity : all) {
            CategoryDto dto = categoryMapper.toDto(entity);
            dto.setChildren(new ArrayList<>());
            dtoMap.put(entity.getId(), dto);
        }

        List<CategoryDto> roots = new ArrayList<>();
        for (CategoryDto dto : dtoMap.values()) {
            if (dto.getParentId() != null && dtoMap.containsKey(dto.getParentId())
                    && !Objects.equals(dto.getParentId(), dto.getId())) {
                CategoryDto parent = dtoMap.get(dto.getParentId());
                dto.setParentName(parent.getName());
                parent.getChildren().add(dto);
            } else {
                roots.add(dto);
            }
        }
        return roots;
    }
}

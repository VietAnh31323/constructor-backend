package com.backend.constructor.app.rest;

import com.backend.constructor.app.api.CategoryApi;
import com.backend.constructor.app.dto.category.CategoryDto;
import com.backend.constructor.app.dto.category.CategoryFilterParam;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.common.base.response.paging.HandsomePaging;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/category")
@RequiredArgsConstructor
public class CategoryController implements CategoryApi {
    private final CategoryApi categoryService;

    @Override
    @PostMapping
    public IdResponse create(@RequestBody @Valid CategoryDto input) {
        return categoryService.create(input);
    }

    @Override
    @PutMapping
    public IdResponse update(@RequestBody @Valid CategoryDto input) {
        return categoryService.update(input);
    }

    @Override
    @DeleteMapping
    public IdResponse delete(@RequestParam Long id) {
        return categoryService.delete(id);
    }

    @Override
    @GetMapping
    public CategoryDto getDetail(@RequestParam Long id) {
        return categoryService.getDetail(id);
    }

    @Override
    @GetMapping("/list")
    @HandsomePaging
    public Page<CategoryDto> getListCategory(@ParameterObject CategoryFilterParam param,
                                             @ParameterObject Pageable pageable) {
        return categoryService.getListCategory(param, pageable);
    }
}

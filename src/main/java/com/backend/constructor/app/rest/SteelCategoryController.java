package com.backend.constructor.app.rest;

import com.backend.constructor.app.api.SteelCategoryApi;
import com.backend.constructor.app.dto.steel_category.SteelCategoryDto;
import com.backend.constructor.app.dto.steel_category.SteelCategoryFilterParam;
import com.backend.constructor.app.dto.steel_category.SteelCategoryLineDto;
import com.backend.constructor.app.dto.steel_category.SteelCategoryOutput;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.common.base.response.paging.HandsomePaging;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/steel-category")
@RequiredArgsConstructor
public class SteelCategoryController implements SteelCategoryApi {
    private final SteelCategoryApi steelCategoryService;

    @Override
    @PostMapping
    public IdResponse create(@RequestBody @Valid SteelCategoryDto input) {
        return steelCategoryService.create(input);
    }

    @Override
    @PutMapping
    public IdResponse update(@RequestBody @Valid SteelCategoryDto input) {
        return steelCategoryService.update(input);
    }

    @Override
    @DeleteMapping
    public void delete(@RequestParam Long id) {
        steelCategoryService.delete(id);
    }

    @Override
    @GetMapping
    public SteelCategoryDto getDetail(@RequestParam Long id) {
        return steelCategoryService.getDetail(id);
    }

    @Override
    @GetMapping("/list")
    @HandsomePaging
    public Page<SteelCategoryOutput> getListSteelCategory(@ParameterObject SteelCategoryFilterParam param,
                                                          @ParameterObject Pageable pageable) {
        return steelCategoryService.getListSteelCategory(param, pageable);
    }

    @Override
    @GetMapping("/line/list")
    public List<SteelCategoryLineDto> getListSteelCategoryLine(@RequestParam Long steelCategoryId) {
        return steelCategoryService.getListSteelCategoryLine(steelCategoryId);
    }
}

package com.backend.constructor.app.rest;

import com.backend.constructor.app.api.SteelApi;
import com.backend.constructor.app.dto.steel.SteelDto;
import com.backend.constructor.app.dto.steel.SteelOutput;
import com.backend.constructor.common.base.dto.response.IdResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/steel")
@RequiredArgsConstructor
public class SteelController implements SteelApi {
    private final SteelApi steelService;

    @Override
    @PostMapping
    public IdResponse create(@RequestBody @Valid SteelDto input) {
        return steelService.create(input);
    }

    @Override
    @PutMapping
    public IdResponse update(@RequestBody @Valid SteelDto input) {
        return steelService.update(input);
    }

    @Override
    @DeleteMapping
    public void delete(@RequestParam Long id) {
        steelService.delete(id);
    }

    @Override
    @GetMapping
    public SteelDto getDetail(Long id) {
        return steelService.getDetail(id);
    }

    @Override
    @GetMapping("/by-assembly/list")
    public List<SteelOutput> getListSteelByAssemblyId(@RequestParam Long assemblyId) {
        return steelService.getListSteelByAssemblyId(assemblyId);
    }
}

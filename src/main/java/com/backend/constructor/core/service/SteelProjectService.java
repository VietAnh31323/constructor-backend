package com.backend.constructor.core.service;

import com.backend.constructor.app.api.SteelProjectApi;
import com.backend.constructor.app.dto.steel.SteelLineDto;
import com.backend.constructor.app.dto.steel.SteelOutput;
import com.backend.constructor.app.dto.steel_project.DiameterSummaryDto;
import com.backend.constructor.app.dto.steel_project.SteelProjectAssemblyMapDto;
import com.backend.constructor.app.dto.steel_project.SteelProjectDto;
import com.backend.constructor.app.dto.steel_project.SteelProjectOutput;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.common.service.GenerateCodeService;
import com.backend.constructor.common.validator.UniqueValidationService;
import com.backend.constructor.core.domain.constant.Constants;
import com.backend.constructor.core.domain.entity.*;
import com.backend.constructor.core.port.mapper.SteelProjectMapper;
import com.backend.constructor.core.port.repository.SteelProjectRepository;
import com.backend.constructor.infras.repository.jpa.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SteelProjectService implements SteelProjectApi {
    private final SteelProjectRepository steelProjectRepository;
    private final SteelProjectMapper steelProjectMapper;
    private final GenerateCodeService generateCodeService;
    private final UniqueValidationService uniqueValidationService;
    private final SteelProjectAssemblyMapJpaRepository assemblyMapJpaRepository;
    private final SteelJpaRepository steelJpaRepository;
    private final SteelLineJpaRepository steelLineJpaRepository;
    private final SteelCategoryJpaRepository steelCategoryJpaRepository;
    private final ProjectJpaRepository projectJpaRepository;
    private final SteelCalculationService steelCalculationService;

    @Override
    @Transactional
    public IdResponse create(SteelProjectDto input) {
        input.trimData();
        validateProjectLink(input.getProjectId());
        generateCodeService.generateCode(input, Constants.DATKT, SteelProjectEntity.class);
        SteelProjectEntity steelProject = steelProjectMapper.toEntity(input);
        uniqueValidationService.validate(steelProject);
        steelProjectRepository.save(steelProject);
        saveSteelProjectAssemblyMaps(input.getSteelProjectAssemblyMaps(), steelProject.getId());
        return IdResponse.builder().id(steelProject.getId()).build();
    }

    @Override
    @Transactional
    public IdResponse update(SteelProjectDto input) {
        input.trimData();
        validateProjectLink(input.getProjectId());
        SteelProjectEntity steelProject = steelProjectRepository.getSteelProjectById(input.getId());
        generateCodeService.generateCode(input, Constants.DATKT, SteelProjectEntity.class);
        steelProjectMapper.update(input, steelProject);
        uniqueValidationService.validate(steelProject);
        steelProjectRepository.save(steelProject);
        clearSteelProjectAssemblyMaps(steelProject.getId());
        saveSteelProjectAssemblyMaps(input.getSteelProjectAssemblyMaps(), steelProject.getId());
        return IdResponse.builder().id(steelProject.getId()).build();
    }

    @Override
    public SteelProjectDto getDetail(Long id) {
        SteelProjectEntity steelProject = steelProjectRepository.getSteelProjectById(id);
        SteelProjectDto dto = steelProjectMapper.toDto(steelProject);

        if (steelProject.getProjectId() != null) {
            projectJpaRepository.findById(steelProject.getProjectId()).ifPresent(p -> {
                dto.setProjectCode(p.getCode());
                dto.setProjectName(p.getName());
            });
        }

        List<SteelProjectAssemblyMapEntity> assemblies =
                assemblyMapJpaRepository.findAllBySteelProjectIdOrderBySortOrderAscIdAsc(steelProject.getId());

        if (assemblies.isEmpty()) {
            dto.setTotalLength(BigDecimal.ZERO);
            dto.setTotalWeight(BigDecimal.ZERO);
            dto.setDiameterSummaries(Collections.emptyList());
            dto.setSteelProjectAssemblyMaps(Collections.emptyList());
            return dto;
        }

        List<Long> assemblyIds = assemblies.stream().map(SteelProjectAssemblyMapEntity::getId).toList();
        List<SteelEntity> directBars = steelJpaRepository.findAllByAssemblyMapIdInOrderBySortOrderAscIdAsc(assemblyIds);

        Map<Long, List<SteelEntity>> barsByAssemblyId = new LinkedHashMap<>();
        for (SteelEntity bar : directBars) {
            barsByAssemblyId.computeIfAbsent(bar.getAssemblyMapId(), k -> new ArrayList<>()).add(bar);
        }

        // Fallback cho dữ liệu cũ dùng steelIds
        Set<Long> legacyBarIds = new LinkedHashSet<>();
        for (SteelProjectAssemblyMapEntity assembly : assemblies) {
            if (!barsByAssemblyId.containsKey(assembly.getId()) && assembly.getSteelIds() != null && !assembly.getSteelIds().isEmpty()) {
                legacyBarIds.addAll(assembly.getSteelIds());
            }
        }
        if (!legacyBarIds.isEmpty()) {
            Map<Long, SteelEntity> legacyMap = steelJpaRepository.findAllById(legacyBarIds).stream()
                    .collect(Collectors.toMap(SteelEntity::getId, Function.identity()));
            for (SteelProjectAssemblyMapEntity assembly : assemblies) {
                if (!barsByAssemblyId.containsKey(assembly.getId()) && assembly.getSteelIds() != null) {
                    List<SteelEntity> list = new ArrayList<>();
                    for (Long sid : assembly.getSteelIds()) {
                        if (legacyMap.containsKey(sid)) {
                            list.add(legacyMap.get(sid));
                        }
                    }
                    barsByAssemblyId.put(assembly.getId(), list);
                }
            }
        }

        List<Long> allBarIds = barsByAssemblyId.values().stream()
                .flatMap(Collection::stream)
                .map(SteelEntity::getId)
                .toList();

        Map<Long, List<SteelLineEntity>> linesBySteelId = allBarIds.isEmpty()
                ? Collections.emptyMap()
                : steelLineJpaRepository.findAllBySteelIdIn(allBarIds).stream()
                        .collect(Collectors.groupingBy(SteelLineEntity::getSteelId));

        Set<Long> categoryIds = barsByAssemblyId.values().stream()
                .flatMap(Collection::stream)
                .map(SteelEntity::getSteelCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, SteelCategoryEntity> categoryMap = categoryIds.isEmpty()
                ? Collections.emptyMap()
                : steelCategoryJpaRepository.findAllById(categoryIds).stream()
                        .collect(Collectors.toMap(SteelCategoryEntity::getId, Function.identity()));

        BigDecimal projectTotalLength = BigDecimal.ZERO;
        BigDecimal projectTotalWeight = BigDecimal.ZERO;
        List<SteelProjectAssemblyMapDto> assemblyDtos = new ArrayList<>();

        for (SteelProjectAssemblyMapEntity assembly : assemblies) {
            List<SteelEntity> bars = barsByAssemblyId.getOrDefault(assembly.getId(), Collections.emptyList());
            long sameQty = assembly.getSameQuantity() != null && assembly.getSameQuantity() > 0
                    ? assembly.getSameQuantity()
                    : 1L;

            BigDecimal assemblyLength = BigDecimal.ZERO;
            BigDecimal assemblyWeight = BigDecimal.ZERO;
            List<SteelOutput> barOutputs = new ArrayList<>();

            for (SteelEntity bar : bars) {
                List<SteelLineEntity> lines = linesBySteelId.getOrDefault(bar.getId(), Collections.emptyList());
                SteelCalculationService.BarCalcResult calc = steelCalculationService.calculateBar(bar, lines, sameQty);
                assemblyLength = assemblyLength.add(calc.totalLengthMeter());
                assemblyWeight = assemblyWeight.add(calc.totalWeightKg());

                SteelCategoryEntity cat = bar.getSteelCategoryId() != null ? categoryMap.get(bar.getSteelCategoryId()) : null;
                List<SteelLineDto> lineDtos = lines.stream()
                        .map(l -> SteelLineDto.builder()
                                .id(l.getId())
                                .paramName(l.getParamName())
                                .value(l.getValue())
                                .build())
                        .toList();

                barOutputs.add(SteelOutput.builder()
                        .id(bar.getId())
                        .assemblyMapId(assembly.getId())
                        .assemblyName(assembly.getAssemblyName())
                        .steelCategoryId(bar.getSteelCategoryId())
                        .steelCategoryCode(cat != null ? cat.getCode() : null)
                        .steelCategoryName(cat != null ? cat.getName() : null)
                        .barCode(bar.getBarCode())
                        .barLabel(StringUtils.isNotBlank(bar.getBarLabel())
                                ? bar.getBarLabel()
                                : (bar.getBarCode() != null ? String.valueOf(bar.getBarCode()) : null))
                        .images((bar.getImages() != null && !bar.getImages().isEmpty())
                                ? bar.getImages()
                                : (cat != null ? cat.getImages() : Collections.emptyList()))
                        .barDiameter(bar.getBarDiameter())
                        .barQuantity(bar.getBarQuantity())
                        .spliceLength(bar.getSpliceLength())
                        .sliceLength(bar.getSpliceLength())
                        .bendDeduction(bar.getBendDeduction())
                        .segmentLength(calc.segmentLengthMm())
                        .cutLength(calc.cutLengthMm())
                        .cutLengthMeter(calc.cutLengthMeter())
                        .length(calc.cutLengthMeter())
                        .unitWeight(calc.unitWeightKg())
                        .totalBars(calc.totalBars())
                        .totalLength(calc.totalLengthMeter())
                        .totalWeight(calc.totalWeightKg())
                        .sortOrder(bar.getSortOrder())
                        .note(bar.getNote())
                        .steelLines(lineDtos)
                        .build());
            }

            assemblyLength = assemblyLength.setScale(4, RoundingMode.HALF_UP);
            assemblyWeight = assemblyWeight.setScale(4, RoundingMode.HALF_UP);
            projectTotalLength = projectTotalLength.add(assemblyLength);
            projectTotalWeight = projectTotalWeight.add(assemblyWeight);

            assemblyDtos.add(SteelProjectAssemblyMapDto.builder()
                    .id(assembly.getId())
                    .assemblyName(assembly.getAssemblyName())
                    .sameQuantity(sameQty)
                    .sortOrder(assembly.getSortOrder())
                    .note(assembly.getNote())
                    .totalLength(assemblyLength)
                    .totalWeight(assemblyWeight)
                    .steels(barOutputs)
                    .build());
        }

        dto.setTotalLength(projectTotalLength.setScale(4, RoundingMode.HALF_UP));
        dto.setTotalWeight(projectTotalWeight.setScale(4, RoundingMode.HALF_UP));
        dto.setDiameterSummaries(steelCalculationService.summarizeByDiameter(assemblies, barsByAssemblyId));
        dto.setSteelProjectAssemblyMaps(assemblyDtos);
        return dto;
    }

    @Override
    public Page<SteelProjectOutput> getListSteelProject(String search, Long projectId, Pageable pageable) {
        Page<SteelProjectEntity> page = steelProjectRepository.getPageSteelProject(search, projectId, pageable);
        List<SteelProjectEntity> content = page.getContent();
        if (content.isEmpty()) {
            return page.map(steelProjectMapper::toOutput);
        }

        Set<Long> mainProjectIds = content.stream()
                .map(SteelProjectEntity::getProjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, ProjectEntity> projectMap = mainProjectIds.isEmpty()
                ? Collections.emptyMap()
                : projectJpaRepository.findAllById(mainProjectIds).stream()
                        .collect(Collectors.toMap(ProjectEntity::getId, Function.identity()));

        Map<Long, List<SteelProjectAssemblyMapEntity>> assembliesBySteelProject = new HashMap<>();
        for (SteelProjectEntity sp : content) {
            assembliesBySteelProject.put(sp.getId(),
                    assemblyMapJpaRepository.findAllBySteelProjectIdOrderBySortOrderAscIdAsc(sp.getId()));
        }

        return page.map(entity -> {
            SteelProjectOutput out = steelProjectMapper.toOutput(entity);
            if (entity.getProjectId() != null && projectMap.containsKey(entity.getProjectId())) {
                ProjectEntity p = projectMap.get(entity.getProjectId());
                out.setProjectCode(p.getCode());
                out.setProjectName(p.getName());
            }
            List<SteelProjectAssemblyMapEntity> assemblies =
                    assembliesBySteelProject.getOrDefault(entity.getId(), Collections.emptyList());
            out.setAssemblyCount(assemblies.size());

            long barCount = 0L;
            BigDecimal totalLength = BigDecimal.ZERO;
            BigDecimal totalWeight = BigDecimal.ZERO;
            for (SteelProjectAssemblyMapEntity a : assemblies) {
                if (a.getSteelIds() != null) {
                    barCount += a.getSteelIds().size();
                }
                if (a.getTotalLength() != null) {
                    totalLength = totalLength.add(a.getTotalLength());
                }
                if (a.getTotalWeight() != null) {
                    totalWeight = totalWeight.add(a.getTotalWeight());
                }
            }
            out.setBarCount(barCount);
            out.setTotalLength(totalLength.setScale(4, RoundingMode.HALF_UP));
            out.setTotalWeight(totalWeight.setScale(4, RoundingMode.HALF_UP));
            return out;
        });
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SteelProjectEntity steelProject = steelProjectRepository.getSteelProjectById(id);
        clearSteelProjectAssemblyMaps(steelProject.getId());
        steelProjectRepository.delete(steelProject);
    }

    private void validateProjectLink(Long projectId) {
        if (projectId != null && !projectJpaRepository.existsById(projectId)) {
            throw BusinessException.exception("PRJ001", "Dự án chính không tồn tại");
        }
    }

    private void saveSteelProjectAssemblyMaps(List<SteelProjectAssemblyMapDto> assemblyDtos, Long steelProjectId) {
        if (assemblyDtos == null || assemblyDtos.isEmpty()) {
            return;
        }
        int assemblyIdx = 1;
        for (SteelProjectAssemblyMapDto dto : assemblyDtos) {
            long sameQty = dto.getSameQuantity() != null && dto.getSameQuantity() > 0 ? dto.getSameQuantity() : 1L;
            SteelProjectAssemblyMapEntity assembly = SteelProjectAssemblyMapEntity.builder()
                    .steelProjectId(steelProjectId)
                    .assemblyName(StringUtils.trimToEmpty(dto.getAssemblyName()))
                    .sameQuantity(sameQty)
                    .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : assemblyIdx)
                    .note(StringUtils.trimToNull(dto.getNote()))
                    .totalLength(BigDecimal.ZERO)
                    .totalWeight(BigDecimal.ZERO)
                    .build();
            assembly = assemblyMapJpaRepository.save(assembly);
            assemblyIdx++;

            List<SteelOutput> barInputs = dto.getSteels() != null ? dto.getSteels() : Collections.emptyList();
            List<SteelEntity> savedBars = new ArrayList<>();
            Map<Long, List<SteelLineEntity>> linesBySteelId = new HashMap<>();

            int barIdx = 1;
            for (SteelOutput barIn : barInputs) {
                BigDecimal splice = barIn.getSpliceLength() != null
                        ? barIn.getSpliceLength()
                        : (barIn.getSliceLength() != null ? barIn.getSliceLength() : BigDecimal.ZERO);
                BigDecimal bendDeduction = barIn.getBendDeduction() != null ? barIn.getBendDeduction() : BigDecimal.ZERO;
                String barLabel = StringUtils.trimToNull(barIn.getBarLabel());
                Long barCode = barIn.getBarCode();
                if (barCode == null && barLabel != null) {
                    String digits = barLabel.replaceAll("[^0-9]", "");
                    if (!digits.isEmpty()) {
                        try {
                            barCode = Long.parseLong(digits);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                if (barLabel == null && barCode != null) {
                    barLabel = String.valueOf(barCode);
                }

                SteelEntity bar = SteelEntity.builder()
                        .assemblyMapId(assembly.getId())
                        .steelCategoryId(barIn.getSteelCategoryId())
                        .barCode(barCode)
                        .barLabel(barLabel)
                        .barQuantity(barIn.getBarQuantity() != null ? barIn.getBarQuantity() : 0L)
                        .barDiameter(barIn.getBarDiameter() != null ? barIn.getBarDiameter() : BigDecimal.ZERO)
                        .spliceLength(splice)
                        .bendDeduction(bendDeduction)
                        .images(barIn.getImages())
                        .sortOrder(barIn.getSortOrder() != null ? barIn.getSortOrder() : barIdx)
                        .note(StringUtils.trimToNull(barIn.getNote()))
                        .build();
                bar = steelJpaRepository.save(bar);
                barIdx++;

                List<SteelLineEntity> savedLines = new ArrayList<>();
                if (barIn.getSteelLines() != null && !barIn.getSteelLines().isEmpty()) {
                    for (SteelLineDto lineDto : barIn.getSteelLines()) {
                        if (StringUtils.isBlank(lineDto.getParamName())) {
                            continue;
                        }
                        savedLines.add(SteelLineEntity.builder()
                                .steelId(bar.getId())
                                .paramName(lineDto.getParamName().trim())
                                .value(lineDto.getValue() != null ? lineDto.getValue() : BigDecimal.ZERO)
                                .build());
                    }
                    if (!savedLines.isEmpty()) {
                        savedLines = steelLineJpaRepository.saveAll(savedLines);
                    }
                }
                linesBySteelId.put(bar.getId(), savedLines);
                savedBars.add(bar);
            }

            steelCalculationService.calculateAssemblyTotals(assembly, savedBars, linesBySteelId);
            if (!savedBars.isEmpty()) {
                steelJpaRepository.saveAll(savedBars);
            }
            assembly.setSteelIds(savedBars.stream().map(SteelEntity::getId).toList());
            assemblyMapJpaRepository.save(assembly);
        }
    }

    private void clearSteelProjectAssemblyMaps(Long steelProjectId) {
        List<SteelProjectAssemblyMapEntity> assemblies = assemblyMapJpaRepository.findAllBySteelProjectId(steelProjectId);
        if (assemblies.isEmpty()) {
            return;
        }
        List<Long> assemblyIds = assemblies.stream().map(SteelProjectAssemblyMapEntity::getId).toList();
        List<SteelEntity> bars = steelJpaRepository.findAllByAssemblyMapIdInOrderBySortOrderAscIdAsc(assemblyIds);
        if (!bars.isEmpty()) {
            List<Long> barIds = bars.stream().map(SteelEntity::getId).toList();
            steelLineJpaRepository.deleteAllBySteelIdIn(barIds);
            steelJpaRepository.deleteAll(bars);
        }
        assemblyMapJpaRepository.deleteAll(assemblies);
    }
}

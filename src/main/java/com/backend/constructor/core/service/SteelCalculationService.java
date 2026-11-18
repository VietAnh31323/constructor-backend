package com.backend.constructor.core.service;

import com.backend.constructor.app.dto.steel_project.DiameterSummaryDto;
import com.backend.constructor.core.domain.entity.SteelEntity;
import com.backend.constructor.core.domain.entity.SteelLineEntity;
import com.backend.constructor.core.domain.entity.SteelProjectAssemblyMapEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class SteelCalculationService {

    private static final BigDecimal STEEL_DENSITY_FACTOR = new BigDecimal("0.00617");
    private static final BigDecimal THOUSAND = new BigDecimal("1000");

    public record BarCalcResult(
            BigDecimal segmentLengthMm,
            BigDecimal cutLengthMm,
            BigDecimal cutLengthMeter,
            BigDecimal unitWeightKg,
            long totalBars,
            BigDecimal totalLengthMeter,
            BigDecimal totalWeightKg
    ) {}

    public BarCalcResult calculateBar(SteelEntity bar, List<SteelLineEntity> lines, Long sameQuantity) {
        BigDecimal segmentMm = BigDecimal.ZERO;
        if (lines != null) {
            for (SteelLineEntity line : lines) {
                if (line.getValue() != null) {
                    segmentMm = segmentMm.add(line.getValue());
                }
            }
        }
        BigDecimal spliceMm = bar.getSpliceLength() != null ? bar.getSpliceLength() : BigDecimal.ZERO;
        BigDecimal bendDeductionMm = bar.getBendDeduction() != null ? bar.getBendDeduction() : BigDecimal.ZERO;

        BigDecimal cutLengthMm = segmentMm.add(spliceMm).subtract(bendDeductionMm);
        if (cutLengthMm.compareTo(BigDecimal.ZERO) < 0) {
            cutLengthMm = BigDecimal.ZERO;
        }
        cutLengthMm = cutLengthMm.setScale(2, RoundingMode.HALF_UP);

        BigDecimal cutLengthMeter = cutLengthMm.divide(THOUSAND, 4, RoundingMode.HALF_UP);
        BigDecimal diameter = bar.getBarDiameter() != null ? bar.getBarDiameter() : BigDecimal.ZERO;

        // Khối lượng 1 thanh (kg) = Ø² × 0.00617 × L(m)
        BigDecimal unitWeightKg = diameter
                .multiply(diameter)
                .multiply(STEEL_DENSITY_FACTOR)
                .multiply(cutLengthMeter)
                .setScale(4, RoundingMode.HALF_UP);

        long barQty = bar.getBarQuantity() != null && bar.getBarQuantity() > 0 ? bar.getBarQuantity() : 0L;
        long sameQty = sameQuantity != null && sameQuantity > 0 ? sameQuantity : 1L;
        long totalBars = barQty * sameQty;

        BigDecimal totalBarsBd = BigDecimal.valueOf(totalBars);
        BigDecimal totalLengthMeter = cutLengthMeter.multiply(totalBarsBd).setScale(4, RoundingMode.HALF_UP);
        BigDecimal totalWeightKg = unitWeightKg.multiply(totalBarsBd).setScale(4, RoundingMode.HALF_UP);

        bar.setCutLength(cutLengthMm);
        bar.setLength(cutLengthMeter);
        bar.setUnitWeight(unitWeightKg);
        bar.setTotalWeight(totalWeightKg);

        return new BarCalcResult(
                segmentMm.setScale(2, RoundingMode.HALF_UP),
                cutLengthMm,
                cutLengthMeter,
                unitWeightKg,
                totalBars,
                totalLengthMeter,
                totalWeightKg
        );
    }

    public void calculateAssemblyTotals(
            SteelProjectAssemblyMapEntity assembly,
            List<SteelEntity> bars,
            Map<Long, List<SteelLineEntity>> linesBySteelId
    ) {
        BigDecimal totalLength = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        long sameQty = assembly.getSameQuantity() != null && assembly.getSameQuantity() > 0
                ? assembly.getSameQuantity()
                : 1L;

        if (bars != null) {
            for (SteelEntity bar : bars) {
                List<SteelLineEntity> lines = linesBySteelId.getOrDefault(bar.getId(), Collections.emptyList());
                BarCalcResult res = calculateBar(bar, lines, sameQty);
                totalLength = totalLength.add(res.totalLengthMeter());
                totalWeight = totalWeight.add(res.totalWeightKg());
            }
        }
        assembly.setTotalLength(totalLength.setScale(4, RoundingMode.HALF_UP));
        assembly.setTotalWeight(totalWeight.setScale(4, RoundingMode.HALF_UP));
    }

    public List<DiameterSummaryDto> summarizeByDiameter(
            List<SteelProjectAssemblyMapEntity> assemblies,
            Map<Long, List<SteelEntity>> barsByAssemblyId
    ) {
        Map<BigDecimal, DiameterSummaryDto> grouped = new TreeMap<>();
        if (assemblies == null) {
            return Collections.emptyList();
        }
        for (SteelProjectAssemblyMapEntity assembly : assemblies) {
            long sameQty = assembly.getSameQuantity() != null && assembly.getSameQuantity() > 0
                    ? assembly.getSameQuantity()
                    : 1L;
            List<SteelEntity> bars = barsByAssemblyId.getOrDefault(assembly.getId(), Collections.emptyList());
            for (SteelEntity bar : bars) {
                if (bar.getBarDiameter() == null) {
                    continue;
                }
                BigDecimal key = bar.getBarDiameter().stripTrailingZeros();
                long barQty = bar.getBarQuantity() != null ? bar.getBarQuantity() : 0L;
                long totalBars = barQty * sameQty;
                BigDecimal cutLenM = bar.getLength() != null ? bar.getLength() : BigDecimal.ZERO;
                BigDecimal totalLenM = cutLenM.multiply(BigDecimal.valueOf(totalBars));
                BigDecimal totalWtKg = bar.getTotalWeight() != null ? bar.getTotalWeight() : BigDecimal.ZERO;

                DiameterSummaryDto acc = grouped.computeIfAbsent(key, d -> DiameterSummaryDto.builder()
                        .barDiameter(d)
                        .totalBars(0L)
                        .totalLengthMeter(BigDecimal.ZERO)
                        .totalWeightKg(BigDecimal.ZERO)
                        .build());
                acc.setTotalBars(acc.getTotalBars() + totalBars);
                acc.setTotalLengthMeter(acc.getTotalLengthMeter().add(totalLenM).setScale(4, RoundingMode.HALF_UP));
                acc.setTotalWeightKg(acc.getTotalWeightKg().add(totalWtKg).setScale(4, RoundingMode.HALF_UP));
            }
        }
        return new ArrayList<>(grouped.values());
    }
}

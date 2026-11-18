package com.backend.constructor.app.dto.steel_project;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SteelProjectOutput {
    private Long id;

    private String code;

    private String name;

    private Long projectId;

    private String projectCode;

    private String projectName;

    private String owner;

    private String address;

    private LocalDate signDate;

    private LocalDate deliveryDate;

    private String description;

    private Integer assemblyCount;

    private Long barCount;

    private BigDecimal totalLength;

    private BigDecimal totalWeight;
}

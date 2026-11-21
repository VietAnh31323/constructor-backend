package com.backend.constructor.app.dto.steel_project;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SteelImportResultDto {
    private boolean success;
    private int totalRows;
    private int validRows;
    private int errorCount;
    private int importedAssemblyCount;
    private int importedBarCount;
    private List<SteelImportErrorDto> errors;
    private SteelProjectDto steelProject;
}

package com.backend.constructor.app.dto.steel;

import com.backend.constructor.app.dto.upload.UploadDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SteelDto {
    private Long id;

    @NotEmpty
    private List<UploadDto> images;

    @NotNull
    private Long barCode;

    @NotNull
    private Long barQuantity;

    @NotNull
    private BigDecimal sliceLength;

    @NotNull
    private BigDecimal barDiameter;

    @Valid
    @NotEmpty
    private List<SteelLineDto> steelLines;
}

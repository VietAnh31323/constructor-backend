package com.backend.constructor.app.dto.steel;

import com.backend.constructor.app.dto.upload.UploadDto;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SteelOutput {
    @NotNull
    private Long id;

    private Long barCode;

    private String assemblyName;

    private List<UploadDto> images;

    private BigDecimal barDiameter;

    private Long barQuantity;

    private BigDecimal sliceLength;

    private BigDecimal length;
}

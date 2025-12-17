package com.backend.constructor.app.dto.customer;

import com.backend.constructor.core.domain.enums.ContactStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerDto {
    private Long id;

    @NotBlank(message = "{ERROR_0001}")
    private String code;

    @NotBlank(message = "{ERROR_0001}")
    private String name;

    private String phone;

    private String email;

    private String address;

    @Enumerated(EnumType.STRING)
    private ContactStatus contactStatus;

    private Boolean isPotential;

    private String description;

    private String note;

    public void trimData() {
        this.code = StringUtils.trim(this.code);
        this.name = StringUtils.trim(this.name);
        this.description = StringUtils.trim(this.description);
        this.note = StringUtils.trim(this.note);
    }
}

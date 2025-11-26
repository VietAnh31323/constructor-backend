package com.backend.constructor.app.dto;

import com.backend.constructor.core.domain.enums.Position;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StaffDto {
    private Long id;

    private String code;

    private String email;

    private Position position;

    private String firstName;

    @NotNull(message = "{ERROR_0001}")
    private String lastName;

    private String avatar;

    private LocalDate birthDate;

    private String address;

    private String phone;

    private String gender;

    private String description;

    public void trimData() {
        this.code = StringUtils.trim(this.code);
        this.firstName = StringUtils.trim(this.firstName);
        this.lastName = StringUtils.trim(this.lastName);
        this.address = StringUtils.trim(this.address);
        this.description = StringUtils.trim(this.description);
    }
}

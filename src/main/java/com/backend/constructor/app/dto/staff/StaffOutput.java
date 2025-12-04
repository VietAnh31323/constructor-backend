package com.backend.constructor.app.dto.staff;

import com.backend.constructor.core.domain.enums.Position;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StaffOutput {
    private Long id;

    private String code;

    private String email;

    private String name;

    private String firstName;

    private String lastName;

    private LocalDate birthDate;

    private String phone;

    private Position position;
}

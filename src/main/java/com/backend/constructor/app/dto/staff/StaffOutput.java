package com.backend.constructor.app.dto.staff;

import com.backend.constructor.app.dto.account.RoleDto;
import com.backend.constructor.common.enums.AccountStatus;
import com.backend.constructor.core.domain.enums.Department;
import com.backend.constructor.core.domain.enums.Position;
import com.backend.constructor.core.domain.enums.StaffStatus;
import com.backend.constructor.core.domain.enums.WorkloadLevel;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

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

    private Department department;

    private LocalDate joinDate;

    private StaffStatus status;

    private String idCardNumber;

    private Integer certificateCount;

    private Integer expiringSoonCertificateCount;

    private Integer expiredCertificateCount;

    // --- 2.03: Tóm tắt tài khoản liên kết ---
    private Boolean hasAccount;

    private Long accountId;

    private String accountUsername;

    private AccountStatus accountStatus;

    private List<RoleDto> roles;

    // --- 2.04: Tóm tắt tải dự án & công việc ---
    private Integer activeProjectCount;

    private Integer activeTaskCount;

    private Integer overdueTaskCount;

    private WorkloadLevel workloadLevel;
}

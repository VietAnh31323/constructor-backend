package com.backend.constructor.app.dto.staff;

import com.backend.constructor.app.dto.account.RoleDto;
import com.backend.constructor.common.enums.AccountStatus;
import com.backend.constructor.core.domain.enums.Department;
import com.backend.constructor.core.domain.enums.Position;
import com.backend.constructor.core.domain.enums.StaffStatus;
import jakarta.validation.Valid;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.util.List;

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

    private Department department;

    private LocalDate joinDate;

    private StaffStatus status;

    private String idCardNumber;

    private LocalDate idCardIssueDate;

    private String idCardIssuePlace;

    private String firstName;

    private String lastName;

    private String name;

    private String avatar;

    private LocalDate birthDate;

    private String address;

    private String phone;

    private String gender;

    private String description;

    private String genPassword;

    @Valid
    private List<StaffCertificateDto> certificates;

    // --- 2.03: Thiết lập / liên kết tài khoản & phân quyền ngay trên form nhân sự ---
    private Boolean createAccount;

    private Long assignAccountId;

    private String initialPassword;

    private Boolean mustChangePassword;

    private List<RoleDto> roles;

    private Boolean hasAccount;

    private Long accountId;

    private String accountUsername;

    private AccountStatus accountStatus;

    public void trimData() {
        this.code = StringUtils.trimToNull(this.code);
        this.email = StringUtils.trimToNull(this.email);
        this.firstName = StringUtils.trim(this.firstName);
        this.lastName = StringUtils.trim(this.lastName);
        this.idCardNumber = StringUtils.trimToNull(this.idCardNumber);
        this.idCardIssuePlace = StringUtils.trimToNull(this.idCardIssuePlace);
        this.phone = StringUtils.trimToNull(this.phone);
        this.address = StringUtils.trim(this.address);
        this.description = StringUtils.trim(this.description);
        this.initialPassword = StringUtils.trimToNull(this.initialPassword);
        if (this.certificates != null) {
            this.certificates.forEach(StaffCertificateDto::trimData);
        }
    }
}

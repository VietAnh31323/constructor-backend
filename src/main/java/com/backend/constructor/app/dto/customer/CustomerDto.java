package com.backend.constructor.app.dto.customer;

import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import com.backend.constructor.core.domain.enums.ContactStatus;
import com.backend.constructor.core.domain.enums.CustomerPipelineStage;
import com.backend.constructor.core.domain.enums.CustomerType;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerDto {
    private Long id;

    private String code;

    @NotBlank(message = "{ERROR_0001}")
    private String name;

    // --- 2.05: Phân loại cá nhân / doanh nghiệp ---
    private CustomerType customerType;

    private String taxCode;

    private String representativeName;

    private String representativePosition;

    private String idCardNumber;

    private String bankAccountNumber;

    private String bankName;

    private String bankBranch;

    private String bankAccountHolder;

    // --- Liên hệ ---
    private String phone;

    private String email;

    private String address;

    private ContactStatus contactStatus;

    // --- 2.08: Pipeline trạng thái khách hàng ---
    private CustomerPipelineStage pipelineStage;

    // --- 2.07: Phụ trách & tổng hợp lịch sử chăm sóc ---
    private Long assignedStaffId;

    private CodeNameResponse assignedStaff;

    private Boolean isPotential;

    private String description;

    private String note;

    // --- 2.06 & 2.07: Thông tin tổng hợp làm giàu khi xem danh sách / chi tiết ---
    private Integer projectCount;

    private BigDecimal totalContractValue;

    private Integer careLogCount;

    private LocalDateTime lastCareTime;

    private LocalDateTime nextActionDate;

    private String nextActionNote;

    private Boolean overdueFollowUp;

    private List<CustomerProjectItemDto> projects;

    public void trimData() {
        this.code = StringUtils.trimToNull(this.code);
        this.name = StringUtils.trim(this.name);
        this.taxCode = StringUtils.trimToNull(this.taxCode);
        this.representativeName = StringUtils.trimToNull(this.representativeName);
        this.representativePosition = StringUtils.trimToNull(this.representativePosition);
        this.idCardNumber = StringUtils.trimToNull(this.idCardNumber);
        this.bankAccountNumber = StringUtils.trimToNull(this.bankAccountNumber);
        this.bankName = StringUtils.trimToNull(this.bankName);
        this.bankBranch = StringUtils.trimToNull(this.bankBranch);
        this.bankAccountHolder = StringUtils.trimToNull(this.bankAccountHolder);
        this.phone = StringUtils.trimToNull(this.phone);
        this.email = StringUtils.trimToNull(this.email);
        this.address = StringUtils.trimToNull(this.address);
        this.description = StringUtils.trim(this.description);
        this.note = StringUtils.trim(this.note);
    }
}

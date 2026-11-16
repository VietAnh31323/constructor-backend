package com.backend.constructor.app.dto.project;

import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import com.backend.constructor.core.domain.enums.CustomerType;
import com.backend.constructor.core.domain.enums.PaymentStatus;
import com.backend.constructor.core.domain.enums.ProjectState;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectOutput {
    private Long id;

    private String code;

    private String name;

    private Long customerId;

    private CodeNameResponse customer;

    private CustomerType customerType;

    private String owner;

    private String address;

    private CodeNameResponse manager;

    private BigDecimal contractValue;

    private BigDecimal contractAdvance;

    private BigDecimal remainingAmount;

    private LocalDate signDate;

    private LocalDate deliveryDate;

    private LocalDate actualHandoverDate;

    private LocalDate warrantyEndDate;

    private ProjectState state;

    private ProjectState previousState;

    private String stateReason;

    private LocalDateTime stateChangedAt;

    private List<ProjectState> allowedNextStates;

    private PaymentStatus paymentStatus;

    private BigDecimal progressPercent;

    // Danh sách vắn tắt vai trò thành viên để hiển thị trên bảng
    private Integer memberCount;

    private Integer contractCount;
}

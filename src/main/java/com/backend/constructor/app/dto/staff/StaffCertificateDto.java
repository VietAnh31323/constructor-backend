package com.backend.constructor.app.dto.staff;

import com.backend.constructor.core.domain.enums.CertificateType;
import com.backend.constructor.core.domain.enums.CertificateValidityStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StaffCertificateDto {
    private Long id;

    private Long staffId;

    @NotNull(message = "ERROR_0001")
    private CertificateType certificateType;

    @NotBlank(message = "ERROR_0001")
    private String certificateNumber;

    private String certificateRank;

    private String issuedBy;

    @NotNull(message = "ERROR_0001")
    private LocalDate issueDate;

    @NotNull(message = "ERROR_0001")
    private LocalDate expiryDate;

    private String note;

    private CertificateValidityStatus validityStatus;

    private Long daysRemaining;

    public void trimData() {
        this.certificateNumber = StringUtils.trimToNull(this.certificateNumber);
        this.certificateRank = StringUtils.trimToNull(this.certificateRank);
        this.issuedBy = StringUtils.trimToNull(this.issuedBy);
        this.note = StringUtils.trimToNull(this.note);
    }

    public void computeValidity(LocalDate today) {
        if (this.expiryDate == null) {
            this.validityStatus = null;
            this.daysRemaining = null;
            return;
        }
        LocalDate ref = today != null ? today : LocalDate.now();
        long days = ChronoUnit.DAYS.between(ref, this.expiryDate);
        this.daysRemaining = days;
        if (days < 0) {
            this.validityStatus = CertificateValidityStatus.EXPIRED;
        } else if (days <= 30) {
            this.validityStatus = CertificateValidityStatus.EXPIRING_SOON;
        } else {
            this.validityStatus = CertificateValidityStatus.VALID;
        }
    }
}

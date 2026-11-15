package com.backend.constructor.app.dto.customer;

import com.backend.constructor.core.domain.enums.CustomerType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PublicCustomerLeadRequestDto {
    @NotBlank(message = "{ERROR_0001}")
    @Size(max = 255)
    private String name;

    @NotBlank(message = "{ERROR_0001}")
    @Size(max = 30)
    private String phone;

    private CustomerType customerType;

    @Size(max = 255)
    private String representativeName;

    @Size(max = 30)
    private String taxCode;

    @Email(message = "Email không đúng định dạng")
    @Size(max = 255)
    private String email;

    @Size(max = 500)
    private String address;

    @Size(max = 255)
    private String serviceInterest;

    @Size(max = 255)
    private String projectScale;

    @Size(max = 120)
    private String estimatedBudget;

    @Size(max = 2000)
    private String message;

    @Size(max = 255)
    private String source;

    public void trimData() {
        this.name = StringUtils.trim(this.name);
        this.phone = StringUtils.trimToNull(this.phone);
        this.representativeName = StringUtils.trimToNull(this.representativeName);
        this.taxCode = StringUtils.trimToNull(this.taxCode);
        this.email = StringUtils.trimToNull(this.email);
        this.address = StringUtils.trimToNull(this.address);
        this.serviceInterest = StringUtils.trimToNull(this.serviceInterest);
        this.projectScale = StringUtils.trimToNull(this.projectScale);
        this.estimatedBudget = StringUtils.trimToNull(this.estimatedBudget);
        this.message = StringUtils.trimToNull(this.message);
        this.source = StringUtils.trimToNull(this.source);
    }
}

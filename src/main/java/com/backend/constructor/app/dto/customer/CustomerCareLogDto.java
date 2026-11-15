package com.backend.constructor.app.dto.customer;

import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import com.backend.constructor.core.domain.enums.CareChannel;
import com.backend.constructor.core.domain.enums.CareOutcome;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerCareLogDto {
    private Long id;

    private Long customerId;

    private Long staffId;

    private CodeNameResponse staff;

    @NotNull(message = "{ERROR_0001}")
    private CareChannel careChannel;

    private LocalDateTime careTime;

    private String summary;

    @NotBlank(message = "{ERROR_0001}")
    private String content;

    private CareOutcome outcome;

    private LocalDateTime nextActionDate;

    private String nextActionNote;

    private boolean overdueFollowUp;

    private boolean upcomingFollowUp;

    private Instant createdAt;

    private String createdBy;

    public void trimData() {
        this.summary = StringUtils.trimToNull(this.summary);
        this.content = StringUtils.trim(this.content);
        this.nextActionNote = StringUtils.trimToNull(this.nextActionNote);
    }
}

package com.backend.constructor.app.dto.task;

import com.backend.constructor.app.dto.upload.UploadDto;
import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskCommentDto {
    private Long id;
    private Long taskId;
    private Long parentId;
    private String content;
    private List<Long> mentionedStaffIds;
    private List<CodeNameResponse> mentionedStaffs;
    private List<UploadDto> attachments;
    private Long authorStaffId;
    private String authorName;
    private Boolean edited;
    private Boolean canEdit;
    private Instant createdAt;
    private Instant updatedAt;
}

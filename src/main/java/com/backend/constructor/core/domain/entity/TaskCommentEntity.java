package com.backend.constructor.core.domain.entity;

import com.backend.constructor.app.dto.upload.UploadDto;
import com.backend.constructor.common.base.entity.BaseEntity;
import com.backend.constructor.core.domain.converter.UploadDtoListConverter;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "task_comment")
public class TaskCommentEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /** Danh sách staffId được @mention, lưu dạng "1,5,9" */
    @Column(name = "mentioned_staff_ids", columnDefinition = "TEXT")
    private String mentionedStaffIds;

    @Convert(converter = UploadDtoListConverter.class)
    @Column(name = "attachments", columnDefinition = "TEXT")
    private List<UploadDto> attachments;

    @Column(name = "author_staff_id")
    private Long authorStaffId;

    @Column(name = "author_name", length = 255)
    private String authorName;

    @Column(name = "edited", nullable = false)
    @Builder.Default
    private Boolean edited = Boolean.FALSE;
}

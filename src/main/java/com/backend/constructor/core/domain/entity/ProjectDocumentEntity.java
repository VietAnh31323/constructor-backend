package com.backend.constructor.core.domain.entity;

import com.backend.constructor.common.base.entity.BaseEntity;
import com.backend.constructor.core.domain.enums.ProjectDocumentFolder;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "project_document")
public class ProjectDocumentEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "folder", length = 50, nullable = false)
    @Builder.Default
    private ProjectDocumentFolder folder = ProjectDocumentFolder.DRAWING;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_url", length = 1000, nullable = false)
    private String fileUrl;

    @Column(name = "file_type", length = 100)
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "version", length = 30, nullable = false)
    @Builder.Default
    private String version = "v1.0";

    @Column(name = "uploaded_by_name", length = 255)
    private String uploadedByName;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}

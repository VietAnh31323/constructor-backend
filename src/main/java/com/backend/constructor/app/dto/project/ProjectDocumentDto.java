package com.backend.constructor.app.dto.project;

import com.backend.constructor.core.domain.enums.ProjectDocumentFolder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectDocumentDto {
    private Long id;

    private Long projectId;

    @NotNull(message = "{ERROR_0001}")
    private ProjectDocumentFolder folder;

    private String folderLabel;

    @NotBlank(message = "{ERROR_0001}")
    private String name;

    private String fileName;

    @NotBlank(message = "{ERROR_0001}")
    private String fileUrl;

    private String fileType;

    private Long fileSize;

    private String version;

    private String uploadedByName;

    private LocalDateTime uploadedAt;

    private String description;
}

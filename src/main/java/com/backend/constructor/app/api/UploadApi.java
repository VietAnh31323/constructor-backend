package com.backend.constructor.app.api;

import com.backend.constructor.app.dto.upload.UploadDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface UploadApi {
    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN', 'SCOPE_STAFF')")
    UploadDto uploadFile(MultipartFile file) throws IOException;
}

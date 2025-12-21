package com.backend.constructor.common.service;

import com.backend.constructor.app.dto.upload.UploadDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface CloudinaryService {
    UploadDto uploadFile(MultipartFile file) throws IOException;

    void deleteFile(String url);
}

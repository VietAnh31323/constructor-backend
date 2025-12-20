package com.backend.constructor.core.service;

import com.backend.constructor.app.api.UploadApi;
import com.backend.constructor.common.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UploadService implements UploadApi {
    private final CloudinaryService cloudinaryService;

    @Override
    public String upload(MultipartFile file) throws IOException {
        return cloudinaryService.uploadImage(file);
    }
}

package com.backend.constructor.app.api;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface UploadApi {
    String upload(MultipartFile file) throws IOException;
}

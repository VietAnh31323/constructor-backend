package com.backend.constructor.app.rest;

import com.backend.constructor.app.api.UploadApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
public class UploadController implements UploadApi {
    private final UploadApi uploadService;

    @Override
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String upload(MultipartFile file) throws IOException {
        return uploadService.upload(file);
    }
}

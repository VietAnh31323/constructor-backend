package com.backend.constructor.common.service.impl;

import com.backend.constructor.app.dto.upload.UploadDto;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.common.service.CloudinaryService;
import com.backend.constructor.core.domain.enums.CloudinaryResourceType;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryServiceImpl implements CloudinaryService {
    public static final String UPLOAD = "/upload/";
    private final Cloudinary cloudinary;

    @Override
    public UploadDto uploadFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("CST013");
        }
        String originalFilename = Objects.requireNonNull(file.getOriginalFilename());
        String[] fileName = getFileName(originalFilename);
        String safeName = sanitizeFileName(fileName[0]);
        CloudinaryResourceType resourceType = detectResourceType(file);
        String publicId = CloudinaryResourceType.RAW.equals(resourceType) ? "constructor/" + safeName + "." + fileName[1] : "constructor/" + safeName;
        @SuppressWarnings("unchecked")
        Map<String, Object> options = ObjectUtils.asMap(
                "resource_type", resourceType.getValue(),
                "public_id", publicId,
                "use_filename", true,
                "unique_filename", false
        );

        Map<?, ?> uploadResult = cloudinary.uploader()
                .upload(file.getBytes(), options);

        String secureUrl = (String) uploadResult.get("secure_url");

        String encodedFileName = URLEncoder.encode(safeName, StandardCharsets.UTF_8);
        String finalUrl = resourceType == CloudinaryResourceType.RAW ? secureUrl.replace(
                UPLOAD,
                "/upload/fl_attachment:" + encodedFileName + "/"
        ) : secureUrl;

        return UploadDto.builder()
                .name(originalFilename)
                .url(finalUrl)
                .type(resourceType.name())
                .build();
    }

    @Override
    public void deleteFile(String url) {
        try {
            // Tách public_id từ URL
            String publicId = extractPublicIdFromUrl(url);

            log.info("xóa publicId: {}", publicId);
            // Gọi Cloudinary để xóa file
            var result = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", "image"
            ));


            if (!"ok".equals(result.get("result"))) {
                throw new BusinessException("500", "Không thể xóa ảnh trên Cloudinary: " + result.get("result"));
            }

        } catch (IOException e) {
            throw new BusinessException("500", "Lỗi khi xóa ảnh: " + e.getMessage());
        }
    }

    private String extractPublicIdFromUrl(String url) {
        // Tìm vị trí bắt đầu từ "/upload/"
        int index = url.indexOf(UPLOAD);
        if (index == -1) {
            throw new BusinessException("400", "URL không hợp lệ: " + url);
        }
        String path = url.substring(index + UPLOAD.length());

        // Nếu có version v1/ → loại bỏ
        if (path.matches("v\\d+/.*")) {
            path = path.substring(path.indexOf("/") + 1); // bỏ "v1/"
        }

        // Xóa phần mở rộng .jpg, .png,...
        return path.replaceAll("\\.[a-zA-Z0-9]+$", "");
    }

    public String[] getFileName(String originalName) {
        return originalName.split("\\.");
    }

    public CloudinaryResourceType detectResourceType(MultipartFile file) {

        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();

        if (contentType != null) {
            if (contentType.startsWith("image/")) {
                return CloudinaryResourceType.IMAGE;
            }
            if (contentType.startsWith("video/")) {
                return CloudinaryResourceType.VIDEO;
            }
        }

        if (filename != null) {
            String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();

            if (Set.of("jpg", "jpeg", "png", "webp", "gif").contains(ext)) {
                return CloudinaryResourceType.IMAGE;
            }
            if (Set.of("mp4", "mov", "avi", "webm").contains(ext)) {
                return CloudinaryResourceType.VIDEO;
            }
        }

        return CloudinaryResourceType.RAW;
    }

    private String sanitizeFileName(String filename) {
        return filename
                .toLowerCase()
                .replaceAll("\\s+", "_")     // space → _
                .replaceAll("[^a-z0-9._-]", "");
    }
}

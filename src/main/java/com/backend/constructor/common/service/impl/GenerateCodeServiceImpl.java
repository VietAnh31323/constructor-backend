package com.backend.constructor.common.service.impl;

import com.backend.constructor.common.base.entity.BaseEntity;
import com.backend.constructor.common.service.GenerateCodeService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerateCodeServiceImpl implements GenerateCodeService {

    private final EntityManager entityManager;

    @Transactional
    public void generateCode(Object input, String featureCode, Class<? extends BaseEntity> entityClazz) {
        Field codeField = ReflectionUtils.findField(input.getClass(), "code");

        if (codeField == null) {
            log.warn("Field 'code' not found in class {}", input.getClass().getSimpleName());
            return;
        }

        ReflectionUtils.makeAccessible(codeField);
        Object currentValue = ReflectionUtils.getField(codeField, input);

        if (currentValue != null && StringUtils.hasText(currentValue.toString())) {
            return;
        }

        // 1. Query lấy mã lớn nhất, ưu tiên độ dài chuỗi trước để tránh lỗi sắp xếp String (NV10 < NV2)
        String jpql = String.format(
                "SELECT e.code FROM %s e " +
                        "WHERE e.code LIKE :prefix " +
                        "ORDER BY LENGTH(e.code) DESC, e.code DESC",
                entityClazz.getSimpleName()
        );

        TypedQuery<String> query = entityManager.createQuery(jpql, String.class);
        query.setParameter("prefix", featureCode + "%");
        query.setMaxResults(1);

        String lastCode = query.getResultList().stream().findFirst().orElse(null);

        // 2. Sinh mã mới
        String newCode = calculateNextCode(lastCode, featureCode);

        // 3. Gán giá trị vào object
        ReflectionUtils.setField(codeField, input, newCode);
    }

    private String calculateNextCode(String lastCode, String prefix) {
        // Nếu chưa có dữ liệu, bắt đầu từ 1 (Ví dụ: NV1)
        if (!StringUtils.hasText(lastCode)) {
            return prefix + "1";
        }

        try {
            // Tách phần số sau prefix
            String numberStr = lastCode.substring(prefix.length());

            // Chuyển sang kiểu Long để xử lý được số lớn và tăng đơn vị
            long nextNumber = Long.parseLong(numberStr) + 1;

            // Nếu mã cũ là "NV1", numberStr.length() = 1. Kết quả mới "NV2"
            // Nếu mã cũ là "NV01", numberStr.length() = 2. Kết quả mới "NV02"
            // Nếu mã cũ là "NV9", nextNumber = 10. formatLength sẽ là 2 -> "NV10"
            int formatLength = Math.max(numberStr.length(), String.valueOf(nextNumber).length());

            return prefix + String.format("%0" + formatLength + "d", nextNumber);

        } catch (Exception e) {
            log.error("Lỗi parse mã cũ: {}, Reset về 1", lastCode);
            return prefix + "1";
        }
    }
}
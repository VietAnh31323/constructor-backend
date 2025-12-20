package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.common.base.repository.filter.Filter;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.core.domain.entity.PasswordResetEntity;
import com.backend.constructor.core.domain.entity.PasswordResetEntity_;
import com.backend.constructor.core.port.repository.PasswordResetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PasswordResetRepositoryImpl extends JpaRepositoryAdapter<PasswordResetEntity> implements PasswordResetRepository {
    @Override
    public List<PasswordResetEntity> getListByAccountId(Long accountId) {
        Filter<PasswordResetEntity> filter = Filter.builder()
                .filter()
                .isEqual(PasswordResetEntity_.ACCOUNT_ID, accountId)
                .build(PasswordResetEntity.class);
        return filter.getList();
    }

    @Override
    public PasswordResetEntity getByAccountIdAndOtpValid(Long accountId, String otp) {
        Filter<PasswordResetEntity> filter = Filter.builder()
                .search()
                .isEqual(PasswordResetEntity_.USED, false)
                .isNull(PasswordResetEntity_.USED)
                .filter()
                .isEqual(PasswordResetEntity_.ACCOUNT_ID, accountId)
                .isEqual(PasswordResetEntity_.OTP_HASH, otp)
                .isGreaterThan(PasswordResetEntity_.EXPIRES_AT, Instant.now())
                .build(PasswordResetEntity.class);
        if (filter.getList().isEmpty()) {
            throw BusinessException.exception("CST011");
        }
        return filter.getList().getFirst();
    }
}

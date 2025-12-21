package com.backend.constructor.core.port.repository;

import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.PasswordResetEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PasswordResetRepository extends BaseRepository<PasswordResetEntity> {
    List<PasswordResetEntity> getListByAccountId(Long accountId);

    PasswordResetEntity getByAccountIdAndOtpValid(Long accountId);
}
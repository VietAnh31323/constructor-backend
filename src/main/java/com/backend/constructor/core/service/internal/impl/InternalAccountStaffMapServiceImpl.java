package com.backend.constructor.core.service.internal.impl;

import com.backend.constructor.core.domain.entity.AccountStaffMapEntity;
import com.backend.constructor.core.port.repository.AccountStaffMapRepository;
import com.backend.constructor.core.service.internal.InternalAccountStaffMapService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InternalAccountStaffMapServiceImpl implements InternalAccountStaffMapService {
    private final AccountStaffMapRepository accountStaffMapRepository;

    @Valid
    @Override
    @Transactional
    public void createAccountMap(@NotNull Long accountId,
                                 @NotNull Long staffId) {
        AccountStaffMapEntity accountStaffMapEntity = AccountStaffMapEntity.builder()
                .accountId(accountId)
                .staffId(staffId)
                .build();
        accountStaffMapRepository.save(accountStaffMapEntity);
    }
}

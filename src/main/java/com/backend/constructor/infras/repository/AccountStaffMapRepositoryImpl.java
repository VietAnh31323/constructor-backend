package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.core.domain.entity.AccountStaffMapEntity;
import com.backend.constructor.core.port.repository.AccountStaffMapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AccountStaffMapRepositoryImpl extends JpaRepositoryAdapter<AccountStaffMapEntity> implements AccountStaffMapRepository {
}

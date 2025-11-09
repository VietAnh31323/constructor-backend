package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.core.domain.entity.AccountRoleMapEntity;
import com.backend.constructor.core.port.repository.AccountRoleMapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AccountRoleMapRepositoryImpl extends JpaRepositoryAdapter<AccountRoleMapEntity> implements AccountRoleMapRepository {
}

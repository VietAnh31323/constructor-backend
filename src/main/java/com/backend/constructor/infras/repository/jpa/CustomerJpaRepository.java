package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.CustomerEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerJpaRepository extends BaseJpaRepository<CustomerEntity> {
    Optional<CustomerEntity> findFirstByPhoneOrderByIdDesc(String phone);
}

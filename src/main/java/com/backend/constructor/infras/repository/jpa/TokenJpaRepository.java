package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.common.enums.TokenType;
import com.backend.constructor.core.domain.entity.TokenEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenJpaRepository extends BaseJpaRepository<TokenEntity> {
    Optional<TokenEntity> findByTokenAndType(String token, TokenType type);
}

package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.common.enums.TokenType;
import com.backend.constructor.core.domain.entity.TokenEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TokenJpaRepository extends BaseJpaRepository<TokenEntity> {
    @Query("""
            SELECT t FROM TokenEntity t
            WHERE t.token = :token
              AND t.type = :type
              AND (t.revoked IS NULL OR t.revoked = false)
            """)
    Optional<TokenEntity> findActiveToken(String token, TokenType type);

    @Query("""
            SELECT t FROM TokenEntity t
            WHERE t.accountId = :accountId
              AND (t.revoked IS NULL OR t.revoked = false)
            """)
    List<TokenEntity> findAllActiveByAccountId(Long accountId);
}

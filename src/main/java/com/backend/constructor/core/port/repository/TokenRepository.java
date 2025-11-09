package com.backend.constructor.core.port.repository;

import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.TokenEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRepository extends BaseRepository<TokenEntity> {
    Optional<TokenEntity> findByRefreshToken(String refreshToken);

    List<TokenEntity> findByAccountAndRevoked(Long accountId, Boolean revoked);
}
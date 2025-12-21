package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.common.base.repository.filter.Filter;
import com.backend.constructor.common.enums.TokenType;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.core.domain.entity.TokenEntity;
import com.backend.constructor.core.domain.entity.TokenEntity_;
import com.backend.constructor.core.port.repository.TokenRepository;
import com.backend.constructor.infras.repository.jpa.TokenJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TokenRepositoryImpl extends JpaRepositoryAdapter<TokenEntity> implements TokenRepository {
    private final EntityManager entityManager;
    private final TokenJpaRepository tokenJpaRepository;

    @Override
    public Optional<TokenEntity> findByRefreshToken(String refreshToken) {
        return tokenJpaRepository.findByTokenAndTypeAndRevokedIsNullOrRevokedIsFalse(refreshToken, TokenType.REFRESH);
    }

    @Override
    public TokenEntity findByResetToken(String resetToken) {
        Filter<TokenEntity> filter = Filter.builder()
                .search()
                .isNull(TokenEntity_.REVOKED)
                .isEqual(TokenEntity_.REVOKED, false)
                .filter()
                .isEqual(TokenEntity_.TOKEN, resetToken)
                .withContext(entityManager)
                .build(TokenEntity.class);
        List<TokenEntity> tokenEntities = filter.getList();
        if (tokenEntities.isEmpty()) {
            throw BusinessException.exception("CST009");
        }
        return tokenEntities.getFirst();
    }

    @Override
    public List<TokenEntity> findByAccountAndRevoked(Long accountId, Boolean revoked) {
        Filter<TokenEntity> filter = Filter.builder()
                .filter()
                .isEqual(TokenEntity_.ACCOUNT_ID, accountId)
                .isEqual(TokenEntity_.REVOKED, revoked)
                .withContext(entityManager)
                .build(TokenEntity.class);
        return filter.getList();
    }
}

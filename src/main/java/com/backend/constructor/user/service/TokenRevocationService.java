package com.backend.constructor.user.service;

import com.backend.constructor.core.domain.entity.AccountEntity;
import com.backend.constructor.core.domain.entity.TokenEntity;
import com.backend.constructor.core.port.repository.AccountRepository;
import com.backend.constructor.core.port.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRevocationService {

    private final TokenRepository tokenRepository;
    private final AccountRepository accountRepository;

    private final Map<String, Instant> blacklistedAccessTokens = new ConcurrentHashMap<>();

    @Transactional
    public void revokeAllTokens(AccountEntity account) {
        if (account == null || account.getId() == null) {
            return;
        }
        List<TokenEntity> activeTokens = tokenRepository.findAllActiveByAccountId(account.getId());
        if (!activeTokens.isEmpty()) {
            Instant now = Instant.now();
            for (TokenEntity token : activeTokens) {
                token.setRevoked(true);
                token.setExpiryDate(now);
            }
            tokenRepository.saveAll(activeTokens);
        }
        account.setTokensValidAfter(Instant.now());
        accountRepository.save(account);
        log.info("Revoked all active tokens for account {}", account.getUsername());
    }

    public void blacklistAccessToken(String token, Instant expiresAt) {
        if (token == null || token.isBlank()) {
            return;
        }
        cleanupExpiredBlacklist();
        blacklistedAccessTokens.put(token, expiresAt != null ? expiresAt : Instant.now().plusSeconds(3600));
    }

    public boolean isAccessTokenBlacklisted(String token) {
        if (token == null) {
            return false;
        }
        Instant exp = blacklistedAccessTokens.get(token);
        if (exp == null) {
            return false;
        }
        if (exp.isBefore(Instant.now())) {
            blacklistedAccessTokens.remove(token);
            return false;
        }
        return true;
    }

    private void cleanupExpiredBlacklist() {
        if (blacklistedAccessTokens.size() < 500) {
            return;
        }
        Instant now = Instant.now();
        blacklistedAccessTokens.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }
}

package com.backend.constructor.user.service;

import com.backend.constructor.common.error.TooManyRequestsException;
import com.backend.constructor.core.domain.entity.AccountEntity;
import com.backend.constructor.core.service.AuditLogService;
import com.backend.constructor.infras.repository.jpa.AccountJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final AccountJpaRepository accountJpaRepository;
    private final AuditLogService auditLogService;

    @Value("${security.login.max-attempts:5}")
    private int maxAccountAttempts;

    @Value("${security.login.lock-duration-minutes:15}")
    private long lockDurationMinutes;

    @Value("${security.login.ip-max-attempts:20}")
    private int maxIpAttempts;

    @Value("${security.login.ip-window-minutes:5}")
    private long ipWindowMinutes;

    private final Map<String, Deque<Instant>> ipAttempts = new ConcurrentHashMap<>();

    public void checkAllowed(String username, String clientIp) {
        if (clientIp != null && !clientIp.isBlank()) {
            Deque<Instant> timestamps = ipAttempts.get(clientIp);
            if (timestamps != null) {
                pruneIpWindow(timestamps);
                if (timestamps.size() >= maxIpAttempts) {
                    throw new TooManyRequestsException(
                            "429",
                            "Quá nhiều lần thử đăng nhập từ địa chỉ mạng này. Vui lòng thử lại sau " + ipWindowMinutes + " phút!"
                    );
                }
            }
        }

        if (username != null && !username.isBlank()) {
            Optional<AccountEntity> opt = accountJpaRepository.findByUsername(username.trim());
            if (opt.isPresent()) {
                AccountEntity account = opt.get();
                Instant lockedUntil = account.getLockedUntil();
                if (lockedUntil != null && lockedUntil.isAfter(Instant.now())) {
                    long remainingMinutes = Math.max(1, Duration.between(Instant.now(), lockedUntil).toMinutes() + 1);
                    throw new TooManyRequestsException(
                            "429",
                            "Tài khoản tạm thời bị khóa do đăng nhập sai quá nhiều lần. Vui lòng thử lại sau " + remainingMinutes + " phút!"
                    );
                }
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedAttempt(String username, String clientIp) {
        if (clientIp != null && !clientIp.isBlank()) {
            Deque<Instant> deque = ipAttempts.computeIfAbsent(clientIp, k -> new ConcurrentLinkedDeque<>());
            pruneIpWindow(deque);
            deque.addLast(Instant.now());
        }

        if (username == null || username.isBlank()) {
            return;
        }

        Optional<AccountEntity> opt = accountJpaRepository.findByUsername(username.trim());
        if (opt.isEmpty()) {
            auditLogService.logExplicit(
                    "LOGIN_FAILED",
                    "ACCOUNT",
                    "AccountEntity",
                    null,
                    null,
                    Map.of("username", username.trim(), "reason", "UNKNOWN_USER"),
                    "Đăng nhập thất bại: " + username.trim(),
                    username.trim()
            );
            return;
        }

        AccountEntity account = opt.get();
        Instant now = Instant.now();
        int prevAttempts = account.getFailedLoginAttempts() != null ? account.getFailedLoginAttempts() : 0;
        if (account.getLockedUntil() != null && account.getLockedUntil().isBefore(now)) {
            prevAttempts = 0;
            account.setLockedUntil(null);
        }
        int newAttempts = prevAttempts + 1;
        account.setFailedLoginAttempts(newAttempts);

        if (newAttempts >= maxAccountAttempts) {
            Instant lockUntil = now.plus(lockDurationMinutes, ChronoUnit.MINUTES);
            account.setLockedUntil(lockUntil);
            accountJpaRepository.save(account);

            auditLogService.logExplicit(
                    "LOGIN_RATE_LIMITED",
                    "ACCOUNT",
                    "AccountEntity",
                    String.valueOf(account.getId()),
                    null,
                    Map.of("failedLoginAttempts", newAttempts, "lockedUntil", lockUntil.toString()),
                    "Tạm khóa đăng nhập tài khoản " + account.getUsername() + " do nhập sai " + newAttempts + " lần",
                    account.getUsername()
            );

            throw new TooManyRequestsException(
                    "429",
                    "Bạn đã đăng nhập sai " + newAttempts + " lần liên tiếp. Tài khoản tạm khóa trong " + lockDurationMinutes + " phút!"
            );
        }

        accountJpaRepository.save(account);
        auditLogService.logExplicit(
                "LOGIN_FAILED",
                "ACCOUNT",
                "AccountEntity",
                String.valueOf(account.getId()),
                null,
                Map.of("failedLoginAttempts", newAttempts),
                "Đăng nhập thất bại lần " + newAttempts + "/" + maxAccountAttempts + " cho tài khoản " + account.getUsername(),
                account.getUsername()
        );
    }

    @Transactional
    public void resetAttempts(AccountEntity account, String clientIp) {
        if (clientIp != null) {
            ipAttempts.remove(clientIp);
        }
        if (account != null && ((account.getFailedLoginAttempts() != null && account.getFailedLoginAttempts() > 0) || account.getLockedUntil() != null)) {
            account.setFailedLoginAttempts(0);
            account.setLockedUntil(null);
            accountJpaRepository.save(account);
        }
    }

    private void pruneIpWindow(Deque<Instant> timestamps) {
        Instant cutoff = Instant.now().minus(ipWindowMinutes, ChronoUnit.MINUTES);
        while (!timestamps.isEmpty()) {
            Instant head = timestamps.peekFirst();
            if (head != null && head.isBefore(cutoff)) {
                timestamps.pollFirst();
            } else {
                break;
            }
        }
    }
}

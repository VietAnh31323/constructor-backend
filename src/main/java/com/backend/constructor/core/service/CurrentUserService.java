package com.backend.constructor.core.service;

import com.backend.constructor.core.domain.entity.AccountEntity;
import com.backend.constructor.core.domain.entity.AccountStaffMapEntity;
import com.backend.constructor.core.domain.entity.StaffEntity;
import com.backend.constructor.core.port.repository.AccountRepository;
import com.backend.constructor.core.port.repository.AccountStaffMapRepository;
import com.backend.constructor.core.port.repository.StaffRepository;
import com.backend.constructor.user.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Tiện ích lấy thông tin người dùng đang đăng nhập (nhân sự gắn với tài khoản, quyền admin).
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {
    private final AccountRepository accountRepository;
    private final AccountStaffMapRepository accountStaffMapRepository;
    private final StaffRepository staffRepository;

    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        return auth.getAuthorities().stream().anyMatch(a -> {
            String val = a.getAuthority();
            return "ROLE_ADMIN".equalsIgnoreCase(val)
                    || "ADMIN".equalsIgnoreCase(val)
                    || "SCOPE_ADMIN".equalsIgnoreCase(val);
        });
    }

    public Long getCurrentStaffId() {
        Optional<String> loginOpt = SecurityUtils.getCurrentUserLogin();
        if (loginOpt.isEmpty() || StringUtils.isBlank(loginOpt.get())) {
            return null;
        }
        Optional<AccountEntity> accountOpt = accountRepository.findByUsername(loginOpt.get());
        if (accountOpt.isEmpty()) {
            return null;
        }
        return accountStaffMapRepository.findOptionalByAccountId(accountOpt.get().getId())
                .map(AccountStaffMapEntity::getStaffId)
                .orElse(null);
    }

    public String getCurrentDisplayName() {
        Long staffId = getCurrentStaffId();
        if (staffId != null) {
            Optional<String> name = staffRepository.findById(staffId)
                    .map(StaffEntity::getName)
                    .filter(StringUtils::isNotBlank);
            if (name.isPresent()) {
                return name.get();
            }
        }
        return SecurityUtils.getCurrentUserLogin().orElse("Hệ thống");
    }
}

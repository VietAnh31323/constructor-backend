package com.backend.constructor.user.security;

import com.backend.constructor.core.domain.entity.AccountEntity;
import com.backend.constructor.core.domain.entity.RoleEntity;
import com.backend.constructor.core.port.repository.AccountRepository;
import com.backend.constructor.core.port.repository.PermissionRepository;
import com.backend.constructor.core.port.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Authenticate a user from the database.
 */
@Slf4j
@Component("userDetailsService")
@RequiredArgsConstructor
public class DomainUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        log.debug("Authenticating {}", username);
        if (new EmailValidator().isValid(username, null)) {
            return accountRepository
                    .findByUsername(username)
                    .map(this::createSpringSecurityUser)
                    .orElseThrow(() -> new UsernameNotFoundException("User with email " + username + " was not found in the database"));
        }
        final var lowercaseLogin = username.toLowerCase(Locale.ENGLISH);
        return accountRepository
                    .findByUsername(lowercaseLogin)
                    .map(this::createSpringSecurityUser)
                    .orElseThrow(() -> new UsernameNotFoundException("User " + lowercaseLogin + " was not found in the database"));
    }

    private User createSpringSecurityUser(AccountEntity account) {
        List<RoleEntity> roleEntities = roleRepository.getListRoleByAccountId(account.getId());
        Set<String> permissionNames = permissionRepository.getPermissionNamesByAccountId(account.getId());

        List<SimpleGrantedAuthority> grantedAuthorities = new ArrayList<>();
        for (RoleEntity role : roleEntities) {
            if (role.getName() != null) {
                grantedAuthorities.add(new SimpleGrantedAuthority(role.getName().name()));
            }
        }
        for (String perm : permissionNames) {
            grantedAuthorities.add(new SimpleGrantedAuthority(perm));
        }

        return new User(account.getUsername(), account.getPassword(), grantedAuthorities);
    }
}

package com.backend.constructor.user.repository;

import com.backend.constructor.common.enums.AuthProvider;
import com.backend.constructor.user.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {
    Optional<AccountEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<AccountEntity> findByUsernameAndAuthProvider(String username, AuthProvider authProvider);
}
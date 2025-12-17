package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.common.enums.ERole;
import com.backend.constructor.core.domain.entity.AccountEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountJpaRepository extends BaseJpaRepository<AccountEntity> {
    @Query("""
                SELECT a
                FROM AccountEntity a
                JOIN AccountRoleMapEntity arm ON arm.accountId = a.id
                JOIN RoleEntity r ON r.id = arm.roleId
                WHERE a.username = :username
                  AND r.name = :roleName
            """)
    Optional<AccountEntity> findByUsernameAndRole(String username,
                                                  ERole roleName);


    boolean existsByUsername(String username);

    Optional<AccountEntity> findByUsername(String username);
}

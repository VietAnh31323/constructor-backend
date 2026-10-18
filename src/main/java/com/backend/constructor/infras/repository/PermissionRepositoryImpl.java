package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.core.domain.entity.PermissionEntity;
import com.backend.constructor.core.port.repository.PermissionRepository;
import com.backend.constructor.infras.repository.jpa.PermissionJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class PermissionRepositoryImpl extends JpaRepositoryAdapter<PermissionEntity> implements PermissionRepository {
    private final EntityManager entityManager;
    private final PermissionJpaRepository permissionJpaRepository;

    @Override
    public Optional<PermissionEntity> getByName(String name) {
        return permissionJpaRepository.findByName(name);
    }

    @Override
    public List<PermissionEntity> getAllPermissions() {
        return permissionJpaRepository.findAll();
    }

    @Override
    public List<PermissionEntity> getPermissionsByRoleId(Long roleId) {
        String jpql = "SELECT p FROM PermissionEntity p, RolePermissionMapEntity rpm " +
                      "WHERE p.id = rpm.permissionId AND rpm.roleId = :roleId";
        return entityManager.createQuery(jpql, PermissionEntity.class)
                .setParameter("roleId", roleId)
                .getResultList();
    }

    @Override
    public Set<String> getPermissionNamesByRoleIds(Collection<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptySet();
        }
        String jpql = "SELECT DISTINCT p.name FROM PermissionEntity p, RolePermissionMapEntity rpm " +
                      "WHERE p.id = rpm.permissionId AND rpm.roleId IN :roleIds";
        List<String> list = entityManager.createQuery(jpql, String.class)
                .setParameter("roleIds", roleIds)
                .getResultList();
        return new HashSet<>(list);
    }

    @Override
    public Set<String> getPermissionNamesByAccountId(Long accountId) {
        if (accountId == null) {
            return Collections.emptySet();
        }
        String jpql = "SELECT DISTINCT p.name FROM PermissionEntity p, RolePermissionMapEntity rpm, AccountRoleMapEntity arm " +
                      "WHERE p.id = rpm.permissionId AND rpm.roleId = arm.roleId AND arm.accountId = :accountId";
        List<String> list = entityManager.createQuery(jpql, String.class)
                .setParameter("accountId", accountId)
                .getResultList();
        return new HashSet<>(list);
    }

    @Override
    public List<PermissionEntity> getPermissionsByAccountId(Long accountId) {
        if (accountId == null) {
            return Collections.emptyList();
        }
        String jpql = "SELECT DISTINCT p FROM PermissionEntity p, RolePermissionMapEntity rpm, AccountRoleMapEntity arm " +
                      "WHERE p.id = rpm.permissionId AND rpm.roleId = arm.roleId AND arm.accountId = :accountId";
        return entityManager.createQuery(jpql, PermissionEntity.class)
                .setParameter("accountId", accountId)
                .getResultList();
    }
}

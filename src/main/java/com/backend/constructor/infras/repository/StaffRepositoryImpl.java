package com.backend.constructor.infras.repository;

import com.backend.constructor.app.dto.staff.StaffFilterParam;
import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.common.base.repository.filter.Filter;
import com.backend.constructor.common.base.repository.filter.FilterFlag;
import com.backend.constructor.common.base.repository.filter.FilterJoiner;
import com.backend.constructor.common.enums.AccountStatus;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.core.domain.entity.*;
import com.backend.constructor.core.port.repository.StaffRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class StaffRepositoryImpl extends JpaRepositoryAdapter<StaffEntity> implements StaffRepository {
    private final EntityManager entityManager;

    @Override
    public StaffEntity getStaffById(Long id) {
        return findById(id).orElseThrow(() -> BusinessException.exception("CST002"));
    }

    @Override
    public Page<StaffEntity> getPageStaff(StaffFilterParam param, Pageable pageable) {
        String trimSearch = StringUtils.trimToNull(param.getSearch());
        Filter<StaffEntity> filter = Filter.builder()
                .search()
                .isContains(StaffEntity_.NAME, trimSearch, FilterFlag.UNACCENT_CASE_SENSITIVE)
                .pageable(pageable)
                .withContext(entityManager)
                .build(StaffEntity.class);
        return filter.getPage();
    }

    @Override
    public StaffEntity getStaffByUsername(String usernameLogin) {
        Filter<StaffEntity> filter = Filter.builder()
                .leftJoin(StaffEntity_.ID, new FilterJoiner(AccountStaffMapEntity.class, AccountStaffMapEntity_.STAFF_ID))
                .thenLeftJoin(AccountStaffMapEntity_.ACCOUNT_ID, new FilterJoiner(AccountEntity.class, AccountEntity_.ID))
                .filter()
                .isEqual(AccountEntity_.USERNAME, usernameLogin)
                .isEqual(AccountEntity_.STATUS, AccountStatus.ACTIVE)
                .withContext(entityManager)
                .build(StaffEntity.class);
        List<StaffEntity> staffEntities = filter.getList();
        if (staffEntities.isEmpty()) {
            throw BusinessException.exception("CST002");
        }
        return staffEntities.getFirst();
    }

    @Override
    public List<StaffEntity> getListStaffByAccountIds(Collection<Long> accountIds) {
        Filter<StaffEntity> filter = Filter.builder()
                .select(StaffEntity_.ID)
                .select(StaffEntity_.CODE)
                .select(StaffEntity_.NAME)
                .select(StaffEntity_.BIRTH_DATE)
                .select(StaffEntity_.PHONE)
                .select(StaffEntity_.POSITION)
                .select(StaffEntity_.AVATAR)
                .leftJoin(StaffEntity_.ID, new FilterJoiner(AccountStaffMapEntity.class, AccountStaffMapEntity_.STAFF_ID))
                .select(AccountStaffMapEntity_.ACCOUNT_ID)
                .filter()
                .isIn(AccountStaffMapEntity_.ACCOUNT_ID, accountIds)
                .withContext(entityManager)
                .build(StaffEntity.class, StaffEntity.class);
        return filter.getList();
    }

    @Override
    public Map<Long, CodeNameResponse> getMapSimpleStaffByIds(Set<Long> staffIds) {
        return findAllByIds(staffIds).stream()
                .collect(Collectors.toMap(StaffEntity::getId, entity -> CodeNameResponse.builder()
                        .id(entity.getId())
                        .code(entity.getCode())
                        .name(entity.getName())
                        .build()));
    }

    @Override
    public StaffEntity getStaffByAccountId(Long accountId) {
        Filter<StaffEntity> filter = Filter.builder()
                .leftJoin(StaffEntity_.ID, new FilterJoiner(AccountStaffMapEntity.class, AccountStaffMapEntity_.STAFF_ID))
                .thenLeftJoin(AccountStaffMapEntity_.ACCOUNT_ID, new FilterJoiner(AccountEntity.class, AccountEntity_.ID))
                .filter()
                .isEqual(AccountEntity_.ID, accountId)
                .isEqual(AccountEntity_.STATUS, AccountStatus.ACTIVE)
                .withContext(entityManager)
                .build(StaffEntity.class);
        List<StaffEntity> staffEntities = filter.getList();
        if (staffEntities.isEmpty()) {
            throw BusinessException.exception("CST002");
        }
        return staffEntities.getFirst();
    }

    @Override
    public Map<Long, StaffEntity> getMapStaffEntityByIds(Set<Long> staffIds) {
        return findAllByIds(staffIds).stream()
                .collect(Collectors.toMap(StaffEntity::getId, Function.identity()));
    }
}

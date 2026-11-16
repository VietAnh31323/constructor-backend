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
import com.backend.constructor.core.port.repository.AccountStaffMapRepository;
import com.backend.constructor.core.port.repository.StaffRepository;
import com.backend.constructor.infras.repository.jpa.StaffJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class StaffRepositoryImpl extends JpaRepositoryAdapter<StaffEntity> implements StaffRepository {
    private final EntityManager entityManager;
    private final StaffJpaRepository staffJpaRepository;
    private final AccountStaffMapRepository accountStaffMapRepository;

    @Override
    public StaffEntity getStaffById(Long id) {
        return findById(id).orElseThrow(() -> BusinessException.exception("CST002"));
    }

    @Override
    public Page<StaffEntity> getPageStaff(StaffFilterParam param, Pageable pageable) {
        String trimSearch = param != null ? StringUtils.trimToNull(param.getSearch()) : null;
        Pageable effectivePageable = (pageable != null && pageable.getSort().isUnsorted())
                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "id"))
                : pageable;

        Filter<StaffEntity> filter = Filter.builder()
                .search()
                .isContains(StaffEntity_.NAME, trimSearch, FilterFlag.UNACCENT_CASE_SENSITIVE)
                .isContains(StaffEntity_.CODE, trimSearch, FilterFlag.CASE_SENSITIVE)
                .isContains(StaffEntity_.EMAIL, trimSearch, FilterFlag.CASE_SENSITIVE)
                .isContains(StaffEntity_.PHONE, trimSearch, FilterFlag.CASE_SENSITIVE)
                .isContains(StaffEntity_.ID_CARD_NUMBER, trimSearch, FilterFlag.CASE_SENSITIVE)
                .filter()
                .isEqual(StaffEntity_.POSITION, param != null ? param.getPosition() : null)
                .isEqual(StaffEntity_.DEPARTMENT, param != null ? param.getDepartment() : null)
                .isEqual(StaffEntity_.STATUS, param != null ? param.getStatus() : null)
                .pageable(effectivePageable)
                .withContext(entityManager)
                .build(StaffEntity.class);
        return filter.getPage();
    }

    @Override
    public StaffEntity getStaffByUsername(String usernameLogin) {
        return findOptionalStaffByUsername(usernameLogin)
                .orElseThrow(() -> BusinessException.exception("CST020"));
    }

    @Override
    public Optional<StaffEntity> findOptionalStaffByUsername(String usernameLogin) {
        Filter<StaffEntity> filter = Filter.builder()
                .leftJoin(StaffEntity_.ID, new FilterJoiner(AccountStaffMapEntity.class, AccountStaffMapEntity_.STAFF_ID))
                .thenLeftJoin(AccountStaffMapEntity_.ACCOUNT_ID, new FilterJoiner(AccountEntity.class, AccountEntity_.ID))
                .filter()
                .isEqual(AccountEntity_.USERNAME, usernameLogin)
                .isEqual(AccountEntity_.STATUS, AccountStatus.ACTIVE)
                .withContext(entityManager)
                .build(StaffEntity.class);
        List<StaffEntity> staffEntities = filter.getList();
        return staffEntities.isEmpty() ? Optional.empty() : Optional.of(staffEntities.getFirst());
    }

    @Override
    public List<StaffEntity> getListStaffByAccountIds(Collection<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<AccountStaffMapEntity> maps = accountStaffMapRepository.getListByAccountIds(accountIds);
        if (maps.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Long> staffIdToAccountId = new HashMap<>();
        for (AccountStaffMapEntity m : maps) {
            if (m.getStaffId() != null && m.getAccountId() != null) {
                staffIdToAccountId.putIfAbsent(m.getStaffId(), m.getAccountId());
            }
        }
        List<StaffEntity> staffs = findAllByIds(new ArrayList<>(staffIdToAccountId.keySet()));
        for (StaffEntity staff : staffs) {
            staff.setAccountId(staffIdToAccountId.get(staff.getId()));
        }
        return staffs;
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
        return findOptionalStaffByAccountId(accountId)
                .orElseThrow(() -> BusinessException.exception("CST002"));
    }

    @Override
    public Optional<StaffEntity> findOptionalStaffByAccountId(Long accountId) {
        if (accountId == null) {
            return Optional.empty();
        }
        Optional<AccountStaffMapEntity> mapOpt = accountStaffMapRepository.findOptionalByAccountId(accountId);
        if (mapOpt.isEmpty() || mapOpt.get().getStaffId() == null) {
            return Optional.empty();
        }
        Optional<StaffEntity> staffOpt = findById(mapOpt.get().getStaffId());
        staffOpt.ifPresent(s -> s.setAccountId(accountId));
        return staffOpt;
    }

    @Override
    public Map<Long, StaffEntity> getMapStaffEntityByIds(Set<Long> staffIds) {
        if (staffIds == null || staffIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return findAllByIds(staffIds).stream()
                .collect(Collectors.toMap(StaffEntity::getId, Function.identity()));
    }
}

package com.backend.constructor.infras.repository;

import com.backend.constructor.app.dto.customer.CustomerFilterParam;
import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.common.base.repository.filter.Filter;
import com.backend.constructor.common.base.repository.filter.FilterFlag;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.core.domain.entity.CustomerEntity;
import com.backend.constructor.core.domain.entity.CustomerEntity_;
import com.backend.constructor.core.port.repository.CustomerRepository;
import com.backend.constructor.infras.repository.jpa.CustomerJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerRepositoryImpl extends JpaRepositoryAdapter<CustomerEntity> implements CustomerRepository {
    private final EntityManager entityManager;
    private final CustomerJpaRepository customerJpaRepository;

    @Override
    public CustomerEntity getCustomerById(Long id) {
        return findById(id).orElseThrow(() -> BusinessException.exception("CST003"));
    }

    @Override
    public Optional<CustomerEntity> findFirstByPhone(String phone) {
        if (StringUtils.isBlank(phone)) {
            return Optional.empty();
        }
        return customerJpaRepository.findFirstByPhoneOrderByIdDesc(phone.trim());
    }

    @Override
    public Page<CustomerEntity> getPageCustomer(CustomerFilterParam param, Pageable pageable) {
        String trimSearch = param != null ? StringUtils.trimToNull(param.getSearch()) : null;
        Pageable effectivePageable = (pageable != null && pageable.getSort().isUnsorted())
                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "id"))
                : pageable;

        Filter<CustomerEntity> filter = Filter.builder()
                .search()
                .isContains(CustomerEntity_.NAME, trimSearch, FilterFlag.UNACCENT_CASE_SENSITIVE)
                .isContains(CustomerEntity_.CODE, trimSearch, FilterFlag.CASE_SENSITIVE)
                .isContains(CustomerEntity_.PHONE, trimSearch, FilterFlag.CASE_SENSITIVE)
                .isContains(CustomerEntity_.EMAIL, trimSearch, FilterFlag.CASE_SENSITIVE)
                .isContains(CustomerEntity_.TAX_CODE, trimSearch, FilterFlag.CASE_SENSITIVE)
                .isContains(CustomerEntity_.REPRESENTATIVE_NAME, trimSearch, FilterFlag.UNACCENT_CASE_SENSITIVE)
                .filter()
                .isEqual(CustomerEntity_.CUSTOMER_TYPE, param != null ? param.getCustomerType() : null)
                .isEqual(CustomerEntity_.PIPELINE_STAGE, param != null ? param.getPipelineStage() : null)
                .isEqual(CustomerEntity_.CONTACT_STATUS, param != null ? param.getContactStatus() : null)
                .isEqual(CustomerEntity_.ASSIGNED_STAFF_ID, param != null ? param.getAssignedStaffId() : null)
                .isEqual(CustomerEntity_.IS_POTENTIAL, param != null ? param.getIsPotential() : null)
                .pageable(effectivePageable)
                .withContext(entityManager)
                .build(CustomerEntity.class);
        return filter.getPage();
    }

    @Override
    public Map<Long, CodeNameResponse> getMapSimpleCustomerByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return findAllByIds(ids).stream()
                .collect(Collectors.toMap(CustomerEntity::getId, entity -> CodeNameResponse.builder()
                        .id(entity.getId())
                        .code(entity.getCode())
                        .name(entity.getName())
                        .build()));
    }

    @Override
    public Map<Long, CustomerEntity> getMapCustomerEntityByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return findAllByIds(ids).stream()
                .collect(Collectors.toMap(CustomerEntity::getId, Function.identity()));
    }
}

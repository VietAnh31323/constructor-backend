package com.backend.constructor.infras.repository;

import com.backend.constructor.app.dto.customer.CustomerFilterParam;
import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.common.base.repository.filter.Filter;
import com.backend.constructor.common.base.repository.filter.FilterFlag;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.config.languages.Translator;
import com.backend.constructor.core.domain.entity.CustomerEntity;
import com.backend.constructor.core.domain.entity.CustomerEntity_;
import com.backend.constructor.core.port.repository.CustomerRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerRepositoryImpl extends JpaRepositoryAdapter<CustomerEntity> implements CustomerRepository {
    private final EntityManager entityManager;
    private final Translator translator;

    @Override
    public CustomerEntity getCustomerById(Long id) {
        return findById(id).orElseThrow(() -> BusinessException.exception(translator.toLocale("CST003")));

    }

    @Override
    public Page<CustomerEntity> getPageCustomer(CustomerFilterParam param, Pageable pageable) {
        String trimSearch = StringUtils.trimToNull(param.getSearch());
        Filter<CustomerEntity> filter = Filter.builder()
                .search()
                .isContains(CustomerEntity_.NAME, trimSearch, FilterFlag.UNACCENT_CASE_SENSITIVE)
                .filter()
                .isEqual(CustomerEntity_.CONTACT_STATUS, param.getContactStatus())
                .isEqual(CustomerEntity_.IS_POTENTIAL, param.getContactStatus())
                .pageable(pageable)
                .withContext(entityManager)
                .build(CustomerEntity.class);
        return filter.getPage();
    }
}

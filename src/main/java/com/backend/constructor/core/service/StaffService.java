package com.backend.constructor.core.service;

import com.backend.constructor.app.api.StaffApi;
import com.backend.constructor.app.dto.staff.StaffDto;
import com.backend.constructor.app.dto.staff.StaffFilterParam;
import com.backend.constructor.app.dto.staff.StaffOutput;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.core.domain.entity.AccountEntity;
import com.backend.constructor.core.domain.entity.AccountStaffMapEntity;
import com.backend.constructor.core.domain.entity.StaffEntity;
import com.backend.constructor.core.port.mapper.StaffMapper;
import com.backend.constructor.core.port.repository.AccountRepository;
import com.backend.constructor.core.port.repository.AccountStaffMapRepository;
import com.backend.constructor.core.port.repository.StaffRepository;
import com.backend.constructor.core.service.internal.InternalAccountStaffMapService;
import com.backend.constructor.user.dto.request.SignUpRequest;
import com.backend.constructor.user.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.backend.constructor.core.service.HelperService.addIfNotNull;
import static com.backend.constructor.core.service.HelperService.joinName;
import static com.backend.constructor.core.service.PasswordGenerator.generateDefaultPassword;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffService implements StaffApi {
    private final StaffRepository staffRepository;
    private final StaffMapper staffMapper;
    private final AuthenticationService authenticationService;
    private final InternalAccountStaffMapService internalAccountStaffMapService;
    private final AccountStaffMapRepository accountStaffMapRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public IdResponse create(StaffDto input) {
        input.trimData();
        input.setId(null);
        StaffEntity staffEntity = staffMapper.toEntity(input);
        staffEntity.setName(joinName(input.getFirstName(), input.getLastName()));
        staffRepository.save(staffEntity);
        createAccount(staffEntity);
        return IdResponse.builder().id(staffEntity.getId()).build();
    }

    private void createAccount(StaffEntity staffEntity) {
        String password = generateDefaultPassword(8);
        staffEntity.setGenPassword(password);
        SignUpRequest request = SignUpRequest.builder()
                .username(staffEntity.getEmail())
                .password(password)
                .build();
        IdResponse idResponse = authenticationService.signUp(request);
        internalAccountStaffMapService.createAccountMap(staffEntity.getId(), idResponse.getId());
    }

    @Override
    @Transactional
    public IdResponse update(StaffDto input) {
        input.trimData();
        StaffEntity staffEntity = staffRepository.getStaffById(input.getId());
        staffMapper.update(input, staffEntity);
        staffEntity.setName(joinName(input.getFirstName(), input.getLastName()));
        staffRepository.save(staffEntity);
        return IdResponse.builder().id(staffEntity.getId()).build();
    }

    @Override
    @Transactional
    public IdResponse delete(Long id) {
        StaffEntity staffEntity = staffRepository.getStaffById(id);
        staffRepository.delete(staffEntity);
        deleteAccountOfStaff(id);
        return IdResponse.builder().id(staffEntity.getId()).build();
    }

    @Override
    public StaffDto getDetail(Long id) {
        StaffEntity staffEntity = staffRepository.getStaffById(id);
        return staffMapper.toDto(staffEntity);
    }

    @Override
    public Page<StaffOutput> getListStaff(StaffFilterParam param,
                                          Pageable pageable) {
        Page<StaffEntity> staffEntities = staffRepository.getPageStaff(param, pageable);
        return staffEntities.map(staffMapper::toOutput);
    }

    private void deleteAccountOfStaff(Long id) {
        List<AccountStaffMapEntity> accountStaffMapEntities = accountStaffMapRepository.getListByStaffId(id);
        Set<Long> accountIds = new HashSet<>();
        for (AccountStaffMapEntity accountStaffMapEntity : accountStaffMapEntities) {
            addIfNotNull(accountIds, accountStaffMapEntity.getAccountId());
        }
        List<AccountEntity> accountEntities = accountRepository.findAllByIds(accountIds);
        accountStaffMapRepository.deleteAllInBatch(accountStaffMapEntities);
        accountRepository.deleteAll(accountEntities);
    }
}

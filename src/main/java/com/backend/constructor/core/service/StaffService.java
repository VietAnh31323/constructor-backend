package com.backend.constructor.core.service;

import com.backend.constructor.app.api.StaffApi;
import com.backend.constructor.app.dto.StaffDto;
import com.backend.constructor.app.dto.StaffFilterParam;
import com.backend.constructor.app.dto.StaffOutput;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.core.domain.entity.StaffEntity;
import com.backend.constructor.core.port.mapper.StaffMapper;
import com.backend.constructor.core.port.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.backend.constructor.core.service.HelperService.joinName;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffService implements StaffApi {
    private final StaffRepository staffRepository;
    private final StaffMapper staffMapper;

    @Override
    @Transactional
    public IdResponse create(StaffDto input) {
        input.trimData();
        StaffEntity staffEntity = staffMapper.toEntity(input);
        staffEntity.setName(joinName(input.getFirstName(), input.getLastName()));
        staffRepository.save(staffEntity);
        return IdResponse.builder().id(staffEntity.getId()).build();
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
}

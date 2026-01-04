package com.backend.constructor.core.service.dto;

import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import com.backend.constructor.core.domain.entity.StaffEntity;
import com.backend.constructor.core.domain.entity.TaskEntity;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DataCollect {
    private Map<Long, TaskEntity> taskEntityMap;
    private Map<Long, StaffEntity> staffEntityMap;
    private Map<Long, List<CodeNameResponse>> staffSimpleListMap;
}

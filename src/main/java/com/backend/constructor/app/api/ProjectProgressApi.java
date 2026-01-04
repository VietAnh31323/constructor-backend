package com.backend.constructor.app.api;

import com.backend.constructor.app.dto.project_progress.ProjectInfoDto;
import com.backend.constructor.common.base.dto.response.IdResponse;

public interface ProjectProgressApi {
    IdResponse create(ProjectInfoDto input);

    ProjectInfoDto getDetail(Long id);
}

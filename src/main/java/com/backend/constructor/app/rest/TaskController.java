package com.backend.constructor.app.rest;

import com.backend.constructor.app.api.TaskApi;
import com.backend.constructor.app.dto.task.TaskDto;
import com.backend.constructor.app.dto.task.TaskOutput;
import com.backend.constructor.common.base.dto.response.IdResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/task")
public class TaskController implements TaskApi {
    private final TaskApi taskService;

    @Override
    @PostMapping
    public IdResponse create(@RequestBody @Valid TaskDto input) {
        return taskService.create(input);
    }

    @Override
    @PutMapping
    public IdResponse update(@RequestBody @Valid TaskDto input) {
        return taskService.update(input);
    }

    @Override
    @DeleteMapping
    public void delete(@RequestParam Long id) {
        taskService.delete(id);
    }

    @Override
    @GetMapping
    public TaskDto getDetail(@RequestParam Long id) {
        return taskService.getDetail(id);
    }

    @Operation(summary = "Lấy danh sách công việc con của công việc cha")
    @Override
    @GetMapping("/task-sub/list")
    public List<TaskOutput> getListTaskSub(@RequestParam Long parentId) {
        return taskService.getListTaskSub(parentId);
    }
}

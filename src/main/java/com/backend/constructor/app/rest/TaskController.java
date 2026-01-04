package com.backend.constructor.app.rest;

import com.backend.constructor.app.api.TaskApi;
import com.backend.constructor.app.dto.task.TaskDto;
import com.backend.constructor.common.base.dto.response.IdResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
}

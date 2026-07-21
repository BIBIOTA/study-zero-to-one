package com.example.taskcrud.controller;

import com.example.taskcrud.dto.TaskRequest;
import com.example.taskcrud.dto.TaskResponse;
import com.example.taskcrud.entity.Task;
import com.example.taskcrud.mapper.TaskMapper;
import com.example.taskcrud.service.TaskService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;
    private final TaskMapper taskMapper;

    public TaskController(TaskService taskService, TaskMapper taskMapper) {
        this.taskService = taskService;
        this.taskMapper = taskMapper;
    }

    @GetMapping
    public List<TaskResponse> getAll() {
        return taskService.getAll()
                .stream()
                .map(taskMapper::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public TaskResponse getById(@PathVariable Long id) {
        return taskMapper.toResponse(taskService.getById(id));
    }

    @PostMapping
    public TaskResponse create(@RequestBody TaskRequest request) {
        Task task = taskMapper.toEntity(request);
        return taskMapper.toResponse(taskService.create(task));
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id, @RequestBody TaskRequest request) {
        Task updated = taskMapper.toEntity(request);
        return taskMapper.toResponse(taskService.update(id, updated));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        taskService.delete(id);
    }
}

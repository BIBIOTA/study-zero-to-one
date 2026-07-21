package com.example.taskcrud.mapper;

import com.example.taskcrud.dto.TaskRequest;
import com.example.taskcrud.dto.TaskResponse;
import com.example.taskcrud.entity.Task;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    public TaskResponse toResponse(Task task) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setTitle(task.getTitle());
        response.setCompleted(task.isCompleted());
        return response;
    }

    public Task toEntity(TaskRequest request) {
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setCompleted(request.isCompleted());
        return task;
    }
}

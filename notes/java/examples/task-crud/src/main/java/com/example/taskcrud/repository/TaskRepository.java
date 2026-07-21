package com.example.taskcrud.repository;

import com.example.taskcrud.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    // findAll、findById、save、deleteById 已由 JpaRepository 內建
}

package com.example.taskcrud.dto;

public class TaskRequest {
    private String title;
    private boolean completed;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}

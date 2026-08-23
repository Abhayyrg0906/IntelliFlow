package com.intelliflow.model;

import com.intelliflow.enums.ProjectStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Project {
    private int id;
    private String name;
    private String description;
    private Integer managerId; // Can be null if manager is deleted
    private LocalDate startDate;
    private LocalDate deadline;
    private ProjectStatus status;
    private LocalDateTime createdAt;

    // Constructors
    public Project() {}

    public Project(int id, String name, String description, Integer managerId, LocalDate startDate, LocalDate deadline, ProjectStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.managerId = managerId;
        this.startDate = startDate;
        this.deadline = deadline;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getManagerId() {
        return managerId;
    }

    public void setManagerId(Integer managerId) {
        this.managerId = managerId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return name;
    }
}

package com.intelliflow.model;

import java.time.LocalDateTime;

public class ActivityLog {
    private int id;
    private Integer userId; // Can be null if user was deleted
    private String action;
    private String description;
    private LocalDateTime timestamp;

    // Constructors
    public ActivityLog() {}

    public ActivityLog(int id, Integer userId, String action, String description, LocalDateTime timestamp) {
        this.id = id;
        this.userId = userId;
        this.action = action;
        this.description = description;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

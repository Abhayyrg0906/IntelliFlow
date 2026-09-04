package com.intelliflow.model;

import com.intelliflow.enums.ProjectHealth;

public class ProjectProgressReport {
    private int projectId;
    private String projectName;
    private int totalTasks;
    private int completedTasks;
    private int pendingTasks; // TO_DO, IN_PROGRESS, TESTING
    private int blockedTasks;
    private int overdueTasks;
    private double completionPercentage;
    private ProjectHealth health = ProjectHealth.ON_TRACK;

    public ProjectProgressReport() {}

    public ProjectHealth getHealth() {
        return health;
    }

    public void setHealth(ProjectHealth health) {
        this.health = health;
    }

    // Getters and Setters
    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(int totalTasks) {
        this.totalTasks = totalTasks;
    }

    public int getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(int completedTasks) {
        this.completedTasks = completedTasks;
    }

    public int getPendingTasks() {
        return pendingTasks;
    }

    public void setPendingTasks(int pendingTasks) {
        this.pendingTasks = pendingTasks;
    }

    public int getBlockedTasks() {
        return blockedTasks;
    }

    public void setBlockedTasks(int blockedTasks) {
        this.blockedTasks = blockedTasks;
    }

    public int getOverdueTasks() {
        return overdueTasks;
    }

    public void setOverdueTasks(int overdueTasks) {
        this.overdueTasks = overdueTasks;
    }

    public double getCompletionPercentage() {
        return completionPercentage;
    }

    public void setCompletionPercentage(double completionPercentage) {
        this.completionPercentage = completionPercentage;
    }
}

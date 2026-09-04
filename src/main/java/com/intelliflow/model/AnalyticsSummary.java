package com.intelliflow.model;

import com.intelliflow.enums.ProjectHealth;
import com.intelliflow.enums.TaskPriority;
import com.intelliflow.enums.TaskStatus;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class AnalyticsSummary {
    private int totalTasks;
    private int completedTasks;
    private double taskCompletionRate; // percentage 0.0 to 100.0
    private int overdueTaskCount;
    private int dueSoonTaskCount;

    private Map<TaskPriority, Integer> priorityDistribution = new EnumMap<>(TaskPriority.class);
    private Map<TaskStatus, Integer> statusDistribution = new EnumMap<>(TaskStatus.class);
    private Map<ProjectHealth, Integer> projectHealthDistribution = new EnumMap<>(ProjectHealth.class);

    private List<ProjectProgressReport> projectProgressList = new ArrayList<>();
    private List<EmployeePerformanceReport> employeeWorkloads = new ArrayList<>();

    public AnalyticsSummary() {
        for (TaskPriority p : TaskPriority.values()) {
            priorityDistribution.put(p, 0);
        }
        for (TaskStatus s : TaskStatus.values()) {
            statusDistribution.put(s, 0);
        }
        for (ProjectHealth h : ProjectHealth.values()) {
            projectHealthDistribution.put(h, 0);
        }
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

    public double getTaskCompletionRate() {
        return taskCompletionRate;
    }

    public void setTaskCompletionRate(double taskCompletionRate) {
        this.taskCompletionRate = taskCompletionRate;
    }

    public int getOverdueTaskCount() {
        return overdueTaskCount;
    }

    public void setOverdueTaskCount(int overdueTaskCount) {
        this.overdueTaskCount = overdueTaskCount;
    }

    public int getDueSoonTaskCount() {
        return dueSoonTaskCount;
    }

    public void setDueSoonTaskCount(int dueSoonTaskCount) {
        this.dueSoonTaskCount = dueSoonTaskCount;
    }

    public Map<TaskPriority, Integer> getPriorityDistribution() {
        return priorityDistribution;
    }

    public void setPriorityDistribution(Map<TaskPriority, Integer> priorityDistribution) {
        this.priorityDistribution = priorityDistribution;
    }

    public Map<TaskStatus, Integer> getStatusDistribution() {
        return statusDistribution;
    }

    public void setStatusDistribution(Map<TaskStatus, Integer> statusDistribution) {
        this.statusDistribution = statusDistribution;
    }

    public Map<ProjectHealth, Integer> getProjectHealthDistribution() {
        return projectHealthDistribution;
    }

    public void setProjectHealthDistribution(Map<ProjectHealth, Integer> projectHealthDistribution) {
        this.projectHealthDistribution = projectHealthDistribution;
    }

    public List<ProjectProgressReport> getProjectProgressList() {
        return projectProgressList;
    }

    public void setProjectProgressList(List<ProjectProgressReport> projectProgressList) {
        this.projectProgressList = projectProgressList;
    }

    public List<EmployeePerformanceReport> getEmployeeWorkloads() {
        return employeeWorkloads;
    }

    public void setEmployeeWorkloads(List<EmployeePerformanceReport> employeeWorkloads) {
        this.employeeWorkloads = employeeWorkloads;
    }
}

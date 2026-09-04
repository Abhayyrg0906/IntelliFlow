package com.intelliflow.model;

public class TeamMemberWorkload {
    private int employeeId;
    private String employeeName;
    private String email;
    private int assignedTasks;
    private int completedTasks;
    private int inProgressTasks;
    private int overdueTasks;
    private double completionPercentage;
    private String workloadIndicator;

    public TeamMemberWorkload() {}

    public TeamMemberWorkload(int employeeId, String employeeName, String email, int assignedTasks, int completedTasks, int inProgressTasks, int overdueTasks, double completionPercentage, String workloadIndicator) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.email = email;
        this.assignedTasks = assignedTasks;
        this.completedTasks = completedTasks;
        this.inProgressTasks = inProgressTasks;
        this.overdueTasks = overdueTasks;
        this.completionPercentage = completionPercentage;
        this.workloadIndicator = workloadIndicator;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getAssignedTasks() {
        return assignedTasks;
    }

    public void setAssignedTasks(int assignedTasks) {
        this.assignedTasks = assignedTasks;
    }

    public int getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(int completedTasks) {
        this.completedTasks = completedTasks;
    }

    public int getInProgressTasks() {
        return inProgressTasks;
    }

    public void setInProgressTasks(int inProgressTasks) {
        this.inProgressTasks = inProgressTasks;
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

    public String getWorkloadIndicator() {
        return workloadIndicator;
    }

    public void setWorkloadIndicator(String workloadIndicator) {
        this.workloadIndicator = workloadIndicator;
    }
}

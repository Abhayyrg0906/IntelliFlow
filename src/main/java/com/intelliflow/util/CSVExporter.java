package com.intelliflow.util;

import com.intelliflow.enums.ProjectHealth;
import com.intelliflow.enums.TaskPriority;
import com.intelliflow.enums.TaskStatus;
import com.intelliflow.model.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CSVExporter {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private CSVExporter() {}

    /**
     * Generates a safe filename by removing illegal characters and appending timestamp.
     */
    public static String getSafeFilename(String baseName, String extension) {
        String sanitized = baseName.replaceAll("[^a-zA-Z0-9_-]", "_").replaceAll("_+", "_");
        if (sanitized.startsWith("_")) sanitized = sanitized.substring(1);
        if (sanitized.endsWith("_")) sanitized = sanitized.substring(0, sanitized.length() - 1);
        if (sanitized.isEmpty()) sanitized = "report";

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String ext = extension.startsWith(".") ? extension : "." + extension;
        return sanitized + "_" + timestamp + ext;
    }

    /**
     * 1. Project Summary & Tasks Report
     */
    public static void exportProjectReport(ProjectProgressReport report, List<Task> tasks, List<User> allUsers, File file) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("INTELLIFLOW PROJECT STATUS REPORT");
            writer.newLine();
            writer.write("Project Name," + escapeCSV(report.getProjectName()));
            writer.newLine();
            writer.write("Project ID," + report.getProjectId());
            writer.newLine();
            writer.write("Health Status," + report.getHealth().name());
            writer.newLine();
            writer.write("Completion Rate," + report.getCompletionPercentage() + "%");
            writer.newLine();
            writer.write("Total Tasks," + report.getTotalTasks());
            writer.newLine();
            writer.write("Completed Tasks," + report.getCompletedTasks());
            writer.newLine();
            writer.write("Pending Tasks," + report.getPendingTasks());
            writer.newLine();
            writer.write("Blocked Tasks," + report.getBlockedTasks());
            writer.newLine();
            writer.write("Overdue Tasks," + report.getOverdueTasks());
            writer.newLine();
            writer.newLine();

            writer.write("Task ID,Task Name,Description,Assigned Employee,Priority,Deadline,Status");
            writer.newLine();

            for (Task t : tasks) {
                String empName = resolveUserName(t.getAssignedEmployeeId(), allUsers);
                StringBuilder sb = new StringBuilder();
                sb.append(t.getId()).append(",");
                sb.append(escapeCSV(t.getName())).append(",");
                sb.append(escapeCSV(t.getDescription() != null ? t.getDescription() : "")).append(",");
                sb.append(escapeCSV(empName)).append(",");
                sb.append(t.getPriority().name()).append(",");
                sb.append(t.getDeadline() != null ? t.getDeadline().format(DATE_FMT) : "").append(",");
                sb.append(t.getStatus().name());

                writer.write(sb.toString());
                writer.newLine();
            }
        }
    }

    /**
     * 2. Comprehensive Task Directory Report
     */
    public static void exportTaskReport(List<Task> tasks, List<Project> projects, List<User> allUsers, File file) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("INTELLIFLOW COMPREHENSIVE TASK DIRECTORY");
            writer.newLine();
            writer.write("Generated Date," + LocalDateTime.now().format(TIME_FMT));
            writer.newLine();
            writer.write("Total Tasks," + tasks.size());
            writer.newLine();
            writer.newLine();

            writer.write("Task ID,Task Name,Project Name,Assigned Employee,Priority,Deadline,Status,Created Date");
            writer.newLine();

            for (Task t : tasks) {
                String projName = resolveProjectName(t.getProjectId(), projects);
                String empName = resolveUserName(t.getAssignedEmployeeId(), allUsers);
                String deadline = t.getDeadline() != null ? t.getDeadline().format(DATE_FMT) : "No Deadline";
                String created = t.getCreatedAt() != null ? t.getCreatedAt().format(DATE_FMT) : "";

                StringBuilder sb = new StringBuilder();
                sb.append(t.getId()).append(",");
                sb.append(escapeCSV(t.getName())).append(",");
                sb.append(escapeCSV(projName)).append(",");
                sb.append(escapeCSV(empName)).append(",");
                sb.append(t.getPriority().name()).append(",");
                sb.append(escapeCSV(deadline)).append(",");
                sb.append(t.getStatus().name()).append(",");
                sb.append(escapeCSV(created));

                writer.write(sb.toString());
                writer.newLine();
            }
        }
    }

    /**
     * 3. User & Role Directory Report (Never exposes password hashes or tokens)
     */
    public static void exportUserReport(List<User> users, File file) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("INTELLIFLOW USER AND ROLE DIRECTORY");
            writer.newLine();
            writer.write("Generated Date," + LocalDateTime.now().format(TIME_FMT));
            writer.newLine();
            writer.write("Total Accounts," + users.size());
            writer.newLine();
            writer.newLine();

            writer.write("User ID,Username,Full Name,Email Address,Security Role,Account Status,Created Date");
            writer.newLine();

            for (User u : users) {
                String status = u.isActive() ? "ACTIVE" : "INACTIVE";
                String created = u.getCreatedAt() != null ? u.getCreatedAt().format(DATE_FMT) : "";

                StringBuilder sb = new StringBuilder();
                sb.append(u.getId()).append(",");
                sb.append(escapeCSV(u.getUsername())).append(",");
                sb.append(escapeCSV(u.getFullName())).append(",");
                sb.append(escapeCSV(u.getEmail())).append(",");
                sb.append(u.getRole().name()).append(",");
                sb.append(status).append(",");
                sb.append(escapeCSV(created));

                writer.write(sb.toString());
                writer.newLine();
            }
        }
    }

    /**
     * 4. Priority Breakdown Report
     */
    public static void exportPriorityReport(List<Task> tasks, List<Project> projects, List<User> allUsers, File file) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("INTELLIFLOW TASK PRIORITY AUDIT REPORT");
            writer.newLine();
            writer.write("Generated Date," + LocalDateTime.now().format(TIME_FMT));
            writer.newLine();
            writer.newLine();

            // Priority Distribution Summary
            long crit = tasks.stream().filter(t -> t.getPriority() == TaskPriority.CRITICAL).count();
            long high = tasks.stream().filter(t -> t.getPriority() == TaskPriority.HIGH).count();
            long med = tasks.stream().filter(t -> t.getPriority() == TaskPriority.MEDIUM).count();
            long low = tasks.stream().filter(t -> t.getPriority() == TaskPriority.LOW).count();

            writer.write("Priority Tier,Task Count");
            writer.newLine();
            writer.write("CRITICAL," + crit);
            writer.newLine();
            writer.write("HIGH," + high);
            writer.newLine();
            writer.write("MEDIUM," + med);
            writer.newLine();
            writer.write("LOW," + low);
            writer.newLine();
            writer.newLine();

            writer.write("Task ID,Priority,Task Name,Project Name,Assigned Employee,Deadline,Status");
            writer.newLine();

            // Sort by priority descending
            tasks.stream()
                    .sorted(Comparator.comparing(Task::getPriority).reversed())
                    .forEach(t -> {
                        try {
                            String projName = resolveProjectName(t.getProjectId(), projects);
                            String empName = resolveUserName(t.getAssignedEmployeeId(), allUsers);
                            String deadline = t.getDeadline() != null ? t.getDeadline().format(DATE_FMT) : "No Deadline";

                            StringBuilder sb = new StringBuilder();
                            sb.append(t.getId()).append(",");
                            sb.append(t.getPriority().name()).append(",");
                            sb.append(escapeCSV(t.getName())).append(",");
                            sb.append(escapeCSV(projName)).append(",");
                            sb.append(escapeCSV(empName)).append(",");
                            sb.append(escapeCSV(deadline)).append(",");
                            sb.append(t.getStatus().name());

                            writer.write(sb.toString());
                            writer.newLine();
                        } catch (IOException ignored) {}
                    });
        }
    }

    /**
     * 5. Deadline & Overdue Schedule Report
     */
    public static void exportDeadlineReport(List<Task> tasks, List<Project> projects, List<User> allUsers, File file) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("INTELLIFLOW DEADLINE AND SCHEDULE DIRECTORY");
            writer.newLine();
            writer.write("Generated Date," + LocalDateTime.now().format(TIME_FMT));
            writer.newLine();
            writer.newLine();

            writer.write("Task ID,Task Name,Project Name,Assigned Employee,Priority,Deadline Date,Days Remaining,Schedule Status,Current State");
            writer.newLine();

            LocalDate today = LocalDate.now();

            tasks.stream()
                    .sorted(Comparator.comparing(Task::getDeadline, Comparator.nullsLast(Comparator.naturalOrder())))
                    .forEach(t -> {
                        try {
                            String projName = resolveProjectName(t.getProjectId(), projects);
                            String empName = resolveUserName(t.getAssignedEmployeeId(), allUsers);
                            String deadlineStr = t.getDeadline() != null ? t.getDeadline().format(DATE_FMT) : "No Deadline";
                            String scheduleStatus;
                            String daysRem;

                            if (t.getStatus() == TaskStatus.COMPLETED) {
                                scheduleStatus = "COMPLETED";
                                daysRem = "0";
                            } else if (t.getDeadline() == null) {
                                scheduleStatus = "NO DEADLINE";
                                daysRem = "N/A";
                            } else {
                                long diff = ChronoUnit.DAYS.between(today, t.getDeadline());
                                if (diff < 0) {
                                    scheduleStatus = "OVERDUE";
                                    daysRem = String.valueOf(diff);
                                } else if (diff == 0) {
                                    scheduleStatus = "DUE TODAY";
                                    daysRem = "0";
                                } else {
                                    scheduleStatus = diff <= 2 ? "DUE SOON" : "ON SCHEDULE";
                                    daysRem = String.valueOf(diff);
                                }
                            }

                            StringBuilder sb = new StringBuilder();
                            sb.append(t.getId()).append(",");
                            sb.append(escapeCSV(t.getName())).append(",");
                            sb.append(escapeCSV(projName)).append(",");
                            sb.append(escapeCSV(empName)).append(",");
                            sb.append(t.getPriority().name()).append(",");
                            sb.append(escapeCSV(deadlineStr)).append(",");
                            sb.append(escapeCSV(daysRem)).append(",");
                            sb.append(scheduleStatus).append(",");
                            sb.append(t.getStatus().name());

                            writer.write(sb.toString());
                            writer.newLine();
                        } catch (IOException ignored) {}
                    });
        }
    }

    /**
     * 6. Activity & Audit Logs Report
     */
    public static void exportActivityReport(List<ActivityLog> logs, List<User> allUsers, File file) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("INTELLIFLOW ACTIVITY AND AUDIT TRAIL REPORT");
            writer.newLine();
            writer.write("Generated Date," + LocalDateTime.now().format(TIME_FMT));
            writer.newLine();
            writer.write("Total Log Entries," + logs.size());
            writer.newLine();
            writer.newLine();

            writer.write("Log ID,Timestamp,Actor / User,Action,Description");
            writer.newLine();

            for (ActivityLog l : logs) {
                String actor = "System Action";
                if (l.getUserId() != null) {
                    actor = resolveUserName(l.getUserId(), allUsers);
                }
                String time = l.getTimestamp() != null ? l.getTimestamp().format(TIME_FMT) : "";

                StringBuilder sb = new StringBuilder();
                sb.append(l.getId()).append(",");
                sb.append(escapeCSV(time)).append(",");
                sb.append(escapeCSV(actor)).append(",");
                sb.append(escapeCSV(l.getAction())).append(",");
                sb.append(escapeCSV(l.getDescription() != null ? l.getDescription() : ""));

                writer.write(sb.toString());
                writer.newLine();
            }
        }
    }

    /**
     * 7. Executive Analytics Summary Report
     */
    public static void exportAnalyticsReport(AnalyticsSummary summary, List<Project> projects, File file) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("INTELLIFLOW EXECUTIVE ANALYTICS SUMMARY");
            writer.newLine();
            writer.write("Generated Date," + LocalDateTime.now().format(TIME_FMT));
            writer.newLine();
            writer.newLine();

            // Core KPI Summary
            writer.write("KEY PERFORMANCE METRICS");
            writer.newLine();
            writer.write("Total Tasks," + summary.getTotalTasks());
            writer.newLine();
            writer.write("Completed Tasks," + summary.getCompletedTasks());
            writer.newLine();
            writer.write("Overdue Tasks," + summary.getOverdueTaskCount());
            writer.newLine();
            writer.write("Due Soon Tasks (1-2d)," + summary.getDueSoonTaskCount());
            writer.newLine();
            writer.write("Overall Task Completion Rate," + summary.getTaskCompletionRate() + "%");
            writer.newLine();
            writer.newLine();

            // Priority Distribution
            writer.write("PRIORITY DISTRIBUTION");
            writer.newLine();
            writer.write("Priority,Count");
            writer.newLine();
            for (Map.Entry<TaskPriority, Integer> entry : summary.getPriorityDistribution().entrySet()) {
                writer.write(entry.getKey().name() + "," + entry.getValue());
                writer.newLine();
            }
            writer.newLine();

            // Status Distribution
            writer.write("STATUS DISTRIBUTION");
            writer.newLine();
            writer.write("Status,Count");
            writer.newLine();
            for (Map.Entry<TaskStatus, Integer> entry : summary.getStatusDistribution().entrySet()) {
                writer.write(entry.getKey().name() + "," + entry.getValue());
                writer.newLine();
            }
            writer.newLine();

            // Health Distribution
            writer.write("PROJECT HEALTH DISTRIBUTION");
            writer.newLine();
            writer.write("Health State,Count");
            writer.newLine();
            for (Map.Entry<ProjectHealth, Integer> entry : summary.getProjectHealthDistribution().entrySet()) {
                writer.write(entry.getKey().name() + "," + entry.getValue());
                writer.newLine();
            }
            writer.newLine();

            // Project Progress Details
            writer.write("PROJECT PROGRESS BREAKDOWN");
            writer.newLine();
            writer.write("Project Name,Health State,Completed Tasks,Total Tasks,Completion Percentage");
            writer.newLine();
            for (ProjectProgressReport p : summary.getProjectProgressList()) {
                writer.write(escapeCSV(p.getProjectName()) + "," +
                        p.getHealth().name() + "," +
                        p.getCompletedTasks() + "," +
                        p.getTotalTasks() + "," +
                        p.getCompletionPercentage() + "%");
                writer.newLine();
            }
            writer.newLine();

            // Employee Workloads
            writer.write("EMPLOYEE WORKLOAD BREAKDOWN");
            writer.newLine();
            writer.write("Employee Name,Assigned Tasks,Pending Tasks,Completed Tasks,Overdue Tasks,Completion Rate");
            writer.newLine();
            for (EmployeePerformanceReport emp : summary.getEmployeeWorkloads()) {
                writer.write(escapeCSV(emp.getEmployeeName()) + "," +
                        emp.getTotalTasks() + "," +
                        emp.getPendingTasks() + "," +
                        emp.getCompletedTasks() + "," +
                        emp.getOverdueTasks() + "," +
                        emp.getCompletionRate() + "%");
                writer.newLine();
            }
        }
    }

    private static String resolveUserName(Integer userId, List<User> allUsers) {
        if (userId == null) return "Unassigned";
        return allUsers.stream()
                .filter(u -> u.getId() == userId)
                .map(User::getFullName)
                .findFirst()
                .orElse("User #" + userId);
    }

    private static String resolveProjectName(int projectId, List<Project> projects) {
        return projects.stream()
                .filter(p -> p.getId() == projectId)
                .map(Project::getName)
                .findFirst()
                .orElse("Project #" + projectId);
    }

    private static String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}

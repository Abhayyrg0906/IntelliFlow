package com.intelliflow.util;

import com.intelliflow.enums.DeadlineState;
import com.intelliflow.enums.ProjectHealth;
import com.intelliflow.enums.Role;
import com.intelliflow.enums.TaskPriority;
import com.intelliflow.enums.TaskStatus;
import com.intelliflow.model.Project;
import com.intelliflow.model.Task;
import com.intelliflow.model.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AlertUtil {

    public static class SmartAlert {
        private final String icon;
        private final String message;
        private final String severity; // DANGER, WARNING, INFO

        public SmartAlert(String icon, String message, String severity) {
            this.icon = icon;
            this.message = message;
            this.severity = severity;
        }

        public String getIcon() { return icon; }
        public String getMessage() { return message; }
        public String getSeverity() { return severity; }

        @Override
        public String toString() {
            return icon + " " + message;
        }
    }

    public static List<SmartAlert> generateAlertsForUser(User user, List<Project> allProjects, List<Task> allTasks, LocalDate referenceDate) {
        List<SmartAlert> alerts = new ArrayList<>();
        if (user == null) return alerts;
        if (referenceDate == null) referenceDate = LocalDate.now();

        Role role = user.getRole();

        // 1. Filter visible projects and tasks based on role to ensure privacy & security
        List<Project> visibleProjects;
        List<Task> visibleTasks;

        if (role == Role.ADMIN) {
            visibleProjects = allProjects != null ? allProjects : List.of();
            visibleTasks = allTasks != null ? allTasks : List.of();
        } else if (role == Role.MANAGER) {
            visibleProjects = allProjects != null ? allProjects.stream()
                    .filter(p -> p.getManagerId() != null && p.getManagerId() == user.getId())
                    .collect(Collectors.toList()) : List.of();
            List<Integer> managedProjectIds = visibleProjects.stream().map(Project::getId).collect(Collectors.toList());
            visibleTasks = allTasks != null ? allTasks.stream()
                    .filter(t -> managedProjectIds.contains(t.getProjectId()) || (t.getAssignedEmployeeId() != null && t.getAssignedEmployeeId() == user.getId()))
                    .collect(Collectors.toList()) : List.of();
        } else {
            // EMPLOYEE role: only assigned tasks and projects associated with assigned tasks
            visibleTasks = allTasks != null ? allTasks.stream()
                    .filter(t -> t.getAssignedEmployeeId() != null && t.getAssignedEmployeeId() == user.getId())
                    .collect(Collectors.toList()) : List.of();
            List<Integer> empProjectIds = visibleTasks.stream().map(Task::getProjectId).distinct().collect(Collectors.toList());
            visibleProjects = allProjects != null ? allProjects.stream()
                    .filter(p -> empProjectIds.contains(p.getId()))
                    .collect(Collectors.toList()) : List.of();
        }

        // 2. Compute Task Alerts
        int overdueTasks = 0;
        int criticalDueToday = 0;
        int tasksDueSoon = 0;

        for (Task t : visibleTasks) {
            if (t.getStatus() != TaskStatus.COMPLETED) {
                DeadlineState state = DeadlineUtil.calculateDeadlineState(t, referenceDate);
                if (state == DeadlineState.OVERDUE) {
                    overdueTasks++;
                } else if (state == DeadlineState.DUE_TODAY) {
                    if (t.getPriority() == TaskPriority.CRITICAL || t.getPriority() == TaskPriority.HIGH) {
                        criticalDueToday++;
                    } else {
                        tasksDueSoon++;
                    }
                } else if (state == DeadlineState.DUE_SOON) {
                    tasksDueSoon++;
                }
            }
        }

        if (overdueTasks > 0) {
            alerts.add(new SmartAlert("⛔", overdueTasks + (overdueTasks == 1 ? " overdue task" : " overdue tasks"), "DANGER"));
        }

        if (criticalDueToday > 0) {
            alerts.add(new SmartAlert("🔥", criticalDueToday + (criticalDueToday == 1 ? " critical/high task due today" : " critical/high tasks due today"), "DANGER"));
        }

        if (tasksDueSoon > 0) {
            alerts.add(new SmartAlert("⚠️", tasksDueSoon + (tasksDueSoon == 1 ? " task due soon" : " tasks due soon"), "WARNING"));
        }

        // 3. Compute Project Health Alerts (Admin and Manager only)
        if (role == Role.ADMIN || role == Role.MANAGER) {
            Map<Integer, List<Task>> tasksByProject = (allTasks != null ? allTasks : List.<Task>of()).stream()
                    .collect(Collectors.groupingBy(Task::getProjectId));

            int atRiskProjects = 0;
            int delayedProjects = 0;

            for (Project p : visibleProjects) {
                List<Task> pTasks = tasksByProject.getOrDefault(p.getId(), List.of());
                ProjectHealth health = ProjectHealthUtil.calculateProjectHealth(p, pTasks, referenceDate);
                if (health == ProjectHealth.DELAYED) {
                    delayedProjects++;
                } else if (health == ProjectHealth.AT_RISK) {
                    atRiskProjects++;
                }
            }

            if (delayedProjects > 0) {
                alerts.add(new SmartAlert("🔴", delayedProjects + (delayedProjects == 1 ? " project delayed" : " projects delayed"), "DANGER"));
            }

            if (atRiskProjects > 0) {
                alerts.add(new SmartAlert("🟡", atRiskProjects + (atRiskProjects == 1 ? " project at risk" : " projects at risk"), "WARNING"));
            }
        }

        return alerts;
    }
}

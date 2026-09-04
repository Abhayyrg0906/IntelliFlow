package com.intelliflow.util;

import com.intelliflow.enums.ProjectHealth;
import com.intelliflow.enums.ProjectStatus;
import com.intelliflow.enums.TaskPriority;
import com.intelliflow.enums.TaskStatus;
import com.intelliflow.model.Project;
import com.intelliflow.model.Task;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ProjectHealthUtil {

    public static ProjectHealth calculateProjectHealth(Project project, List<Task> tasks, LocalDate referenceDate) {
        if (project == null) {
            return ProjectHealth.ON_TRACK;
        }

        if (referenceDate == null) {
            referenceDate = LocalDate.now();
        }

        // 1. If project is completed, it's ON_TRACK
        if (project.getStatus() == ProjectStatus.COMPLETED) {
            return ProjectHealth.ON_TRACK;
        }

        // 2. If project deadline itself has passed and project is not completed -> DELAYED
        if (project.getDeadline() != null && project.getDeadline().isBefore(referenceDate)) {
            return ProjectHealth.DELAYED;
        }

        if (tasks == null || tasks.isEmpty()) {
            return ProjectHealth.ON_TRACK;
        }

        int overdueCount = 0;
        int criticalIncomplete = 0;
        int highIncomplete = 0;
        int dueSoonCount = 0;
        int completedCount = 0;
        int totalTasks = tasks.size();

        for (Task t : tasks) {
            if (t.getStatus() == TaskStatus.COMPLETED) {
                completedCount++;
            } else {
                if (t.getDeadline() != null && t.getDeadline().isBefore(referenceDate)) {
                    overdueCount++;
                }
                if (t.getPriority() == TaskPriority.CRITICAL) {
                    criticalIncomplete++;
                }
                if (t.getPriority() == TaskPriority.HIGH) {
                    highIncomplete++;
                }
                if (t.getDeadline() != null && !t.getDeadline().isBefore(referenceDate)) {
                    long days = ChronoUnit.DAYS.between(referenceDate, t.getDeadline());
                    if (days <= 2) {
                        dueSoonCount++;
                    }
                }
            }
        }

        // 3. DELAYED rule: Any overdue incomplete tasks
        if (overdueCount > 0) {
            return ProjectHealth.DELAYED;
        }

        // 4. AT_RISK rule: Critical incomplete tasks OR multiple high priority incomplete OR multiple due soon OR near project deadline with low completion
        double completionPercent = (double) completedCount / totalTasks;
        boolean nearProjectDeadline = project.getDeadline() != null && ChronoUnit.DAYS.between(referenceDate, project.getDeadline()) <= 7;

        if (criticalIncomplete > 0 || highIncomplete >= 2 || dueSoonCount >= 2 || (nearProjectDeadline && completionPercent < 0.50)) {
            return ProjectHealth.AT_RISK;
        }

        // 5. Otherwise ON_TRACK
        return ProjectHealth.ON_TRACK;
    }

    public static ProjectHealth calculateProjectHealth(Project project, List<Task> tasks) {
        return calculateProjectHealth(project, tasks, LocalDate.now());
    }
}

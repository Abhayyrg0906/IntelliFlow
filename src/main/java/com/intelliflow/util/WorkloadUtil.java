package com.intelliflow.util;

import com.intelliflow.enums.TaskStatus;
import com.intelliflow.model.Task;
import com.intelliflow.model.TeamMemberWorkload;
import com.intelliflow.model.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WorkloadUtil {

    public static String calculateWorkloadIndicator(int activeTasks, int overdueTasks) {
        if (overdueTasks >= 2 || activeTasks >= 9) {
            return "🔴 OVERLOADED";
        } else if (overdueTasks == 1 || activeTasks >= 6) {
            return "🟠 HEAVY";
        } else if (activeTasks >= 3) {
            return "🟡 BALANCED";
        } else {
            return "🟢 LIGHT";
        }
    }

    public static TeamMemberWorkload calculateMemberWorkload(User employee, List<Task> tasks, LocalDate referenceDate) {
        if (referenceDate == null) {
            referenceDate = LocalDate.now();
        }

        TeamMemberWorkload workload = new TeamMemberWorkload();
        workload.setEmployeeId(employee != null ? employee.getId() : 0);
        workload.setEmployeeName(employee != null && employee.getFullName() != null && !employee.getFullName().isBlank() 
                ? employee.getFullName() 
                : (employee != null ? employee.getUsername() : "Unassigned"));
        workload.setEmail(employee != null ? employee.getEmail() : "");

        if (tasks == null || tasks.isEmpty()) {
            workload.setAssignedTasks(0);
            workload.setCompletedTasks(0);
            workload.setInProgressTasks(0);
            workload.setOverdueTasks(0);
            workload.setCompletionPercentage(0.0);
            workload.setWorkloadIndicator("🟢 LIGHT");
            return workload;
        }

        int total = tasks.size();
        int completed = 0;
        int inProgress = 0;
        int overdue = 0;

        for (Task t : tasks) {
            if (t.getStatus() == TaskStatus.COMPLETED) {
                completed++;
            } else {
                if (t.getStatus() == TaskStatus.IN_PROGRESS || t.getStatus() == TaskStatus.TESTING || t.getStatus() == TaskStatus.TO_DO || t.getStatus() == TaskStatus.BLOCKED) {
                    inProgress++;
                }
                if (t.getDeadline() != null && t.getDeadline().isBefore(referenceDate)) {
                    overdue++;
                }
            }
        }

        workload.setAssignedTasks(total);
        workload.setCompletedTasks(completed);
        workload.setInProgressTasks(inProgress);
        workload.setOverdueTasks(overdue);

        double compPct = total > 0 ? Math.round(((double) completed / total) * 1000.0) / 10.0 : 0.0;
        workload.setCompletionPercentage(compPct);
        workload.setWorkloadIndicator(calculateWorkloadIndicator(inProgress, overdue));

        return workload;
    }

    public static List<TeamMemberWorkload> calculateTeamWorkloadForProject(List<User> employees, List<Task> projectTasks, LocalDate referenceDate) {
        if (referenceDate == null) {
            referenceDate = LocalDate.now();
        }

        if (projectTasks == null) {
            projectTasks = new ArrayList<>();
        }

        Map<Integer, List<Task>> tasksByEmp = projectTasks.stream()
                .filter(t -> t.getAssignedEmployeeId() != null && t.getAssignedEmployeeId() > 0)
                .collect(Collectors.groupingBy(Task::getAssignedEmployeeId));

        List<TeamMemberWorkload> result = new ArrayList<>();
        if (employees != null) {
            for (User emp : employees) {
                if (tasksByEmp.containsKey(emp.getId())) {
                    result.add(calculateMemberWorkload(emp, tasksByEmp.get(emp.getId()), referenceDate));
                }
            }
        }
        return result;
    }
}

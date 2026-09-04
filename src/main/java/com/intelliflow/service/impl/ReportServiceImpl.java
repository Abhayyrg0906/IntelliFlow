package com.intelliflow.service.impl;

import com.intelliflow.dao.impl.*;
import com.intelliflow.dao.interfaces.*;
import com.intelliflow.enums.Role;
import com.intelliflow.enums.TaskStatus;
import com.intelliflow.exception.DatabaseException;
import com.intelliflow.exception.ValidationException;
import com.intelliflow.model.*;
import com.intelliflow.service.interfaces.ReportService;

import com.intelliflow.enums.ProjectHealth;
import com.intelliflow.enums.TaskPriority;
import com.intelliflow.util.ProjectHealthUtil;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ReportServiceImpl implements ReportService {

    private final ProjectDAO projectDAO;
    private final TaskDAO taskDAO;
    private final UserDAO userDAO;

    public ReportServiceImpl() {
        this.projectDAO = new ProjectDAOImpl();
        this.taskDAO = new TaskDAOImpl();
        this.userDAO = new UserDAOImpl();
    }

    public ReportServiceImpl(ProjectDAO projectDAO, TaskDAO taskDAO, UserDAO userDAO) {
        this.projectDAO = projectDAO;
        this.taskDAO = taskDAO;
        this.userDAO = userDAO;
    }

    @Override
    public ProjectProgressReport getProjectProgressReport(int projectId) throws DatabaseException, ValidationException {
        Optional<Project> projectOpt = projectDAO.findById(projectId);
        if (projectOpt.isEmpty()) {
            throw new ValidationException("Project not found with ID: " + projectId);
        }
        Project project = projectOpt.get();
        List<Task> tasks = taskDAO.findByProjectId(projectId);

        ProjectProgressReport report = new ProjectProgressReport();
        report.setProjectId(projectId);
        report.setProjectName(project.getName());
        report.setTotalTasks(tasks.size());

        LocalDate today = LocalDate.now();
        report.setHealth(ProjectHealthUtil.calculateProjectHealth(project, tasks, today));

        if (tasks.isEmpty()) {
            report.setCompletedTasks(0);
            report.setPendingTasks(0);
            report.setBlockedTasks(0);
            report.setOverdueTasks(0);
            report.setCompletionPercentage(0.0);
            return report;
        }

        int completed = 0;
        int blocked = 0;
        int overdue = 0;

        for (Task t : tasks) {
            if (t.getStatus() == TaskStatus.COMPLETED) {
                completed++;
            } else {
                if (t.getStatus() == TaskStatus.BLOCKED) {
                    blocked++;
                }
                if (t.getDeadline() != null && t.getDeadline().isBefore(today)) {
                    overdue++;
                }
            }
        }

        report.setCompletedTasks(completed);
        report.setBlockedTasks(blocked);
        report.setOverdueTasks(overdue);
        report.setPendingTasks(tasks.size() - completed);
        report.setCompletionPercentage(Math.round(((double) completed / tasks.size()) * 1000.0) / 10.0);

        return report;
    }

    @Override
    public List<EmployeePerformanceReport> getEmployeePerformanceReports() throws DatabaseException {
        List<EmployeePerformanceReport> reports = new ArrayList<>();
        List<User> employees = userDAO.findAll().stream()
                .filter(u -> u.getRole() == Role.EMPLOYEE)
                .collect(Collectors.toList());

        LocalDate today = LocalDate.now();

        for (User emp : employees) {
            List<Task> tasks = taskDAO.findByEmployeeId(emp.getId());
            EmployeePerformanceReport report = new EmployeePerformanceReport();
            report.setEmployeeId(emp.getId());
            report.setEmployeeName(emp.getFullName());
            report.setTotalTasks(tasks.size());

            if (tasks.isEmpty()) {
                report.setCompletedTasks(0);
                report.setPendingTasks(0);
                report.setOverdueTasks(0);
                report.setCompletionRate(0.0);
            } else {
                int completed = 0;
                int overdue = 0;

                for (Task t : tasks) {
                    if (t.getStatus() == TaskStatus.COMPLETED) {
                        completed++;
                    } else if (t.getDeadline() != null && t.getDeadline().isBefore(today)) {
                        overdue++;
                    }
                }

                report.setCompletedTasks(completed);
                report.setPendingTasks(tasks.size() - completed);
                report.setOverdueTasks(overdue);
                report.setCompletionRate(Math.round(((double) completed / tasks.size()) * 1000.0) / 10.0);
            }
            reports.add(report);
        }

        return reports;
    }

    @Override
    public List<Task> getOverdueTasks() throws DatabaseException {
        LocalDate today = LocalDate.now();
        return taskDAO.findAll().stream()
                .filter(t -> t.getStatus() != TaskStatus.COMPLETED)
                .filter(t -> t.getDeadline() != null && t.getDeadline().isBefore(today))
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> getCompletedTasks() throws DatabaseException {
        return taskDAO.findAll().stream()
                .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> getPendingTasks() throws DatabaseException {
        return taskDAO.findAll().stream()
                .filter(t -> t.getStatus() != TaskStatus.COMPLETED)
                .collect(Collectors.toList());
    }

    @Override
    public AnalyticsSummary getAnalyticsSummary(User user) throws DatabaseException {
        return getAnalyticsSummary(user, LocalDate.now());
    }

    @Override
    public AnalyticsSummary getAnalyticsSummary(User user, LocalDate referenceDate) throws DatabaseException {
        if (referenceDate == null) {
            referenceDate = LocalDate.now();
        }

        AnalyticsSummary summary = new AnalyticsSummary();
        List<Project> scopedProjects;
        List<Task> scopedTasks;
        List<User> scopedEmployees;

        if (user == null || user.getRole() == Role.ADMIN) {
            scopedProjects = projectDAO.findAll();
            scopedTasks = taskDAO.findAll();
            scopedEmployees = userDAO.findAll().stream()
                    .filter(u -> u.getRole() == Role.EMPLOYEE)
                    .collect(Collectors.toList());
        } else if (user.getRole() == Role.MANAGER) {
            scopedProjects = projectDAO.findByManagerId(user.getId());
            Set<Integer> managedProjectIds = scopedProjects.stream().map(Project::getId).collect(Collectors.toSet());
            scopedTasks = taskDAO.findAll().stream()
                    .filter(t -> managedProjectIds.contains(t.getProjectId()))
                    .collect(Collectors.toList());
            Set<Integer> assignedEmpIds = scopedTasks.stream()
                    .map(Task::getAssignedEmployeeId)
                    .filter(id -> id != null && id > 0)
                    .collect(Collectors.toSet());
            scopedEmployees = userDAO.findAll().stream()
                    .filter(u -> u.getRole() == Role.EMPLOYEE && (assignedEmpIds.isEmpty() || assignedEmpIds.contains(u.getId())))
                    .collect(Collectors.toList());
        } else {
            scopedTasks = taskDAO.findByEmployeeId(user.getId());
            Set<Integer> assignedProjectIds = scopedTasks.stream().map(Task::getProjectId).collect(Collectors.toSet());
            scopedProjects = projectDAO.findAll().stream()
                    .filter(p -> assignedProjectIds.contains(p.getId()))
                    .collect(Collectors.toList());
            scopedEmployees = Collections.singletonList(user);
        }

        // 1. Task Completion & Counts
        summary.setTotalTasks(scopedTasks.size());
        int completedCount = 0;
        int overdueCount = 0;
        int dueSoonCount = 0;

        Map<TaskPriority, Integer> priorityMap = new EnumMap<>(TaskPriority.class);
        for (TaskPriority p : TaskPriority.values()) priorityMap.put(p, 0);

        Map<TaskStatus, Integer> statusMap = new EnumMap<>(TaskStatus.class);
        for (TaskStatus s : TaskStatus.values()) statusMap.put(s, 0);

        for (Task t : scopedTasks) {
            if (t.getPriority() != null) {
                priorityMap.put(t.getPriority(), priorityMap.getOrDefault(t.getPriority(), 0) + 1);
            }
            if (t.getStatus() != null) {
                statusMap.put(t.getStatus(), statusMap.getOrDefault(t.getStatus(), 0) + 1);
            }

            if (t.getStatus() == TaskStatus.COMPLETED) {
                completedCount++;
            } else {
                if (t.getDeadline() != null) {
                    if (t.getDeadline().isBefore(referenceDate)) {
                        overdueCount++;
                    } else {
                        long daysRemaining = ChronoUnit.DAYS.between(referenceDate, t.getDeadline());
                        if (daysRemaining <= 2) {
                            dueSoonCount++;
                        }
                    }
                }
            }
        }

        summary.setCompletedTasks(completedCount);
        summary.setOverdueTaskCount(overdueCount);
        summary.setDueSoonTaskCount(dueSoonCount);
        summary.setPriorityDistribution(priorityMap);
        summary.setStatusDistribution(statusMap);

        double completionRate = scopedTasks.isEmpty() ? 0.0 : Math.round(((double) completedCount / scopedTasks.size()) * 1000.0) / 10.0;
        summary.setTaskCompletionRate(completionRate);

        // 2. Project Health & Project Progress
        Map<ProjectHealth, Integer> healthMap = new EnumMap<>(ProjectHealth.class);
        for (ProjectHealth h : ProjectHealth.values()) healthMap.put(h, 0);

        List<ProjectProgressReport> projectProgressList = new ArrayList<>();
        for (Project p : scopedProjects) {
            List<Task> prjTasks = taskDAO.findByProjectId(p.getId());
            ProjectHealth health = ProjectHealthUtil.calculateProjectHealth(p, prjTasks, referenceDate);
            healthMap.put(health, healthMap.getOrDefault(health, 0) + 1);

            ProjectProgressReport pReport = new ProjectProgressReport();
            pReport.setProjectId(p.getId());
            pReport.setProjectName(p.getName());
            pReport.setTotalTasks(prjTasks.size());
            pReport.setHealth(health);

            if (prjTasks.isEmpty()) {
                pReport.setCompletedTasks(0);
                pReport.setPendingTasks(0);
                pReport.setBlockedTasks(0);
                pReport.setOverdueTasks(0);
                pReport.setCompletionPercentage(0.0);
            } else {
                int pComp = 0;
                int pBlocked = 0;
                int pOverdue = 0;
                for (Task pt : prjTasks) {
                    if (pt.getStatus() == TaskStatus.COMPLETED) {
                        pComp++;
                    } else {
                        if (pt.getStatus() == TaskStatus.BLOCKED) pBlocked++;
                        if (pt.getDeadline() != null && pt.getDeadline().isBefore(referenceDate)) pOverdue++;
                    }
                }
                pReport.setCompletedTasks(pComp);
                pReport.setBlockedTasks(pBlocked);
                pReport.setOverdueTasks(pOverdue);
                pReport.setPendingTasks(prjTasks.size() - pComp);
                pReport.setCompletionPercentage(Math.round(((double) pComp / prjTasks.size()) * 1000.0) / 10.0);
            }
            projectProgressList.add(pReport);
        }

        summary.setProjectHealthDistribution(healthMap);
        summary.setProjectProgressList(projectProgressList);

        // 3. Employee Workloads
        List<EmployeePerformanceReport> employeeReports = new ArrayList<>();
        for (User emp : scopedEmployees) {
            List<Task> empTasks;
            if (user != null && user.getRole() == Role.MANAGER) {
                Set<Integer> managedProjectIds = scopedProjects.stream().map(Project::getId).collect(Collectors.toSet());
                empTasks = taskDAO.findByEmployeeId(emp.getId()).stream()
                        .filter(t -> managedProjectIds.contains(t.getProjectId()))
                        .collect(Collectors.toList());
            } else {
                empTasks = taskDAO.findByEmployeeId(emp.getId());
            }

            EmployeePerformanceReport eReport = new EmployeePerformanceReport();
            eReport.setEmployeeId(emp.getId());
            eReport.setEmployeeName(emp.getFullName() != null && !emp.getFullName().isBlank() ? emp.getFullName() : emp.getUsername());
            eReport.setTotalTasks(empTasks.size());

            if (empTasks.isEmpty()) {
                eReport.setCompletedTasks(0);
                eReport.setPendingTasks(0);
                eReport.setOverdueTasks(0);
                eReport.setCompletionRate(0.0);
            } else {
                int eComp = 0;
                int eOverdue = 0;
                for (Task et : empTasks) {
                    if (et.getStatus() == TaskStatus.COMPLETED) {
                        eComp++;
                    } else if (et.getDeadline() != null && et.getDeadline().isBefore(referenceDate)) {
                        eOverdue++;
                    }
                }
                eReport.setCompletedTasks(eComp);
                eReport.setPendingTasks(empTasks.size() - eComp);
                eReport.setOverdueTasks(eOverdue);
                eReport.setCompletionRate(Math.round(((double) eComp / empTasks.size()) * 1000.0) / 10.0);
            }
            employeeReports.add(eReport);
        }
        summary.setEmployeeWorkloads(employeeReports);

        return summary;
    }
}

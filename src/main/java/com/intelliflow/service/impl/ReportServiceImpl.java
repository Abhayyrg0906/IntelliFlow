package com.intelliflow.service.impl;

import com.intelliflow.dao.impl.*;
import com.intelliflow.dao.interfaces.*;
import com.intelliflow.enums.Role;
import com.intelliflow.enums.TaskStatus;
import com.intelliflow.exception.DatabaseException;
import com.intelliflow.exception.ValidationException;
import com.intelliflow.model.*;
import com.intelliflow.service.interfaces.ReportService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
        LocalDate today = LocalDate.now();

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
}

package com.intelliflow.service.interfaces;

import com.intelliflow.exception.DatabaseException;
import com.intelliflow.exception.ValidationException;
import com.intelliflow.model.AnalyticsSummary;
import com.intelliflow.model.EmployeePerformanceReport;
import com.intelliflow.model.ProjectProgressReport;
import com.intelliflow.model.Task;
import com.intelliflow.model.User;
import java.time.LocalDate;
import java.util.List;

public interface ReportService {
    ProjectProgressReport getProjectProgressReport(int projectId) throws DatabaseException, ValidationException;
    List<EmployeePerformanceReport> getEmployeePerformanceReports() throws DatabaseException;
    List<Task> getOverdueTasks() throws DatabaseException;
    List<Task> getCompletedTasks() throws DatabaseException;
    List<Task> getPendingTasks() throws DatabaseException;
    AnalyticsSummary getAnalyticsSummary(User user) throws DatabaseException;
    AnalyticsSummary getAnalyticsSummary(User user, LocalDate referenceDate) throws DatabaseException;
}

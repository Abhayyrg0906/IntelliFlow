package com.intelliflow.service.impl;

import com.intelliflow.context.UserSession;
import com.intelliflow.dao.impl.*;
import com.intelliflow.dao.interfaces.*;
import com.intelliflow.enums.Role;
import com.intelliflow.enums.TaskStatus;
import com.intelliflow.exception.DatabaseException;
import com.intelliflow.exception.UnauthorizedException;
import com.intelliflow.exception.ValidationException;
import com.intelliflow.model.*;
import com.intelliflow.service.interfaces.TaskService;
import com.intelliflow.util.ValidationUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class TaskServiceImpl implements TaskService {

    private final TaskDAO taskDAO;
    private final ProjectDAO projectDAO;
    private final UserDAO userDAO;
    private final NotificationDAO notificationDAO;
    private final ActivityLogDAO logDAO;

    public TaskServiceImpl() {
        this.taskDAO = new TaskDAOImpl();
        this.projectDAO = new ProjectDAOImpl();
        this.userDAO = new UserDAOImpl();
        this.notificationDAO = new NotificationDAOImpl();
        this.logDAO = new ActivityLogDAOImpl();
    }

    public TaskServiceImpl(TaskDAO taskDAO, ProjectDAO projectDAO, UserDAO userDAO, NotificationDAO notificationDAO, ActivityLogDAO logDAO) {
        this.taskDAO = taskDAO;
        this.projectDAO = projectDAO;
        this.userDAO = userDAO;
        this.notificationDAO = notificationDAO;
        this.logDAO = logDAO;
    }

    private void checkManagerOrAdmin() throws UnauthorizedException {
        User user = UserSession.getInstance().getCurrentUser();
        if (user == null || (user.getRole() != Role.ADMIN && user.getRole() != Role.MANAGER)) {
            throw new UnauthorizedException("Access Denied: Only Managers and Administrators can perform this operation.");
        }
    }

    @Override
    public Task createTask(Task task) throws ValidationException, DatabaseException, UnauthorizedException {
        checkManagerOrAdmin();
        validateTaskDetails(task);

        Task created = taskDAO.create(task);

        // Audit Log
        User currentUser = UserSession.getInstance().getCurrentUser();
        ActivityLog audit = new ActivityLog();
        audit.setUserId(currentUser != null ? currentUser.getId() : null);
        audit.setAction("TASK_CREATE");
        audit.setDescription("Created task: " + created.getName() + " (ID: " + created.getId() + ")");
        logDAO.create(audit);

        // Assign Notification
        if (created.getAssignedEmployeeId() != null) {
            sendNotification(created.getAssignedEmployeeId(), "New task assigned: " + created.getName());
        }

        return created;
    }

    @Override
    public void updateTask(Task task) throws ValidationException, DatabaseException, UnauthorizedException {
        checkManagerOrAdmin();
        validateTaskDetails(task);

        // Fetch original to check for assignment change
        Optional<Task> originalOpt = taskDAO.findById(task.getId());
        if (originalOpt.isEmpty()) {
            throw new ValidationException("Task not found with ID: " + task.getId());
        }
        Task original = originalOpt.get();

        taskDAO.update(task);

        // Audit Log
        User currentUser = UserSession.getInstance().getCurrentUser();
        ActivityLog audit = new ActivityLog();
        audit.setUserId(currentUser != null ? currentUser.getId() : null);
        audit.setAction("TASK_UPDATE");
        audit.setDescription("Updated task: " + task.getName() + " (ID: " + task.getId() + ")");
        logDAO.create(audit);

        // If assignment changed, notify the new assignee
        if (task.getAssignedEmployeeId() != null && 
            !task.getAssignedEmployeeId().equals(original.getAssignedEmployeeId())) {
            sendNotification(task.getAssignedEmployeeId(), "You have been assigned to task: " + task.getName());
        }
    }

    @Override
    public void updateTaskStatus(int taskId, TaskStatus newStatus) throws ValidationException, DatabaseException, UnauthorizedException {
        Optional<Task> taskOpt = taskDAO.findById(taskId);
        if (taskOpt.isEmpty()) {
            throw new ValidationException("Task not found with ID: " + taskId);
        }
        Task task = taskOpt.get();
        User currentUser = UserSession.getInstance().getCurrentUser();

        if (currentUser == null) {
            throw new UnauthorizedException("Session is not active.");
        }

        // Authorization checks:
        // Employee can only update tasks assigned to them
        if (currentUser.getRole() == Role.EMPLOYEE) {
            if (task.getAssignedEmployeeId() == null || task.getAssignedEmployeeId() != currentUser.getId()) {
                throw new UnauthorizedException("You can only update status for tasks assigned to you.");
            }
        }

        // Workflow Transition Checks
        validateTransition(task.getStatus(), newStatus, currentUser.getRole());

        TaskStatus oldStatus = task.getStatus();
        task.setStatus(newStatus);
        taskDAO.update(task);

        // Log operation
        ActivityLog audit = new ActivityLog();
        audit.setUserId(currentUser.getId());
        audit.setAction("TASK_STATUS_CHANGE");
        audit.setDescription("Task '" + task.getName() + "' status changed from " + oldStatus + " to " + newStatus);
        logDAO.create(audit);

        // Notify Project Manager and/or Admin of change
        Optional<Project> projectOpt = projectDAO.findById(task.getProjectId());
        if (projectOpt.isPresent() && projectOpt.get().getManagerId() != null) {
            sendNotification(projectOpt.get().getManagerId(), 
                "Task '" + task.getName() + "' status updated to " + newStatus + " by " + currentUser.getFullName());
        }
    }

    @Override
    public void deleteTask(int taskId) throws DatabaseException, UnauthorizedException {
        checkManagerOrAdmin();
        Optional<Task> taskOpt = taskDAO.findById(taskId);
        if (taskOpt.isPresent()) {
            taskDAO.delete(taskId);

            User currentUser = UserSession.getInstance().getCurrentUser();
            ActivityLog audit = new ActivityLog();
            audit.setUserId(currentUser != null ? currentUser.getId() : null);
            audit.setAction("TASK_DELETE");
            audit.setDescription("Deleted task: " + taskOpt.get().getName() + " (ID: " + taskId + ")");
            logDAO.create(audit);
        }
    }

    @Override
    public Optional<Task> getTaskById(int id) throws DatabaseException {
        return taskDAO.findById(id);
    }

    @Override
    public List<Task> getTasksByProject(int projectId) throws DatabaseException {
        return taskDAO.findByProjectId(projectId);
    }

    @Override
    public List<Task> getTasksByEmployee(int employeeId) throws DatabaseException {
        return taskDAO.findByEmployeeId(employeeId);
    }

    @Override
    public List<Task> getAllTasks() throws DatabaseException {
        return taskDAO.findAll();
    }

    // Helper: Validates task details and relationships
    private void validateTaskDetails(Task task) throws ValidationException {
        if (task == null) {
            throw new ValidationException("Task details cannot be null.");
        }
        if (!ValidationUtil.isNotEmpty(task.getName())) {
            throw new ValidationException("Task name is required.");
        }
        // Validate association with project
        Optional<Project> projectOpt = projectDAO.findById(task.getProjectId());
        if (projectOpt.isEmpty()) {
            throw new ValidationException("Associated project does not exist (ID: " + task.getProjectId() + ").");
        }
        Project project = projectOpt.get();

        if (task.getDeadline() != null) {
            // Constraint: Task deadline cannot exceed the project's deadline
            if (project.getDeadline() != null && task.getDeadline().isAfter(project.getDeadline())) {
                throw new ValidationException("Task deadline (" + task.getDeadline() + ") cannot be after project deadline (" + project.getDeadline() + ").");
            }

            // Constraint: Task deadline cannot be before project start date
            if (project.getStartDate() != null && task.getDeadline().isBefore(project.getStartDate())) {
                throw new ValidationException("Task deadline cannot be before project start date (" + project.getStartDate() + ").");
            }
        }

        // Validate assigned employee
        if (task.getAssignedEmployeeId() != null) {
            Optional<User> userOpt = userDAO.findById(task.getAssignedEmployeeId());
            if (userOpt.isEmpty()) {
                throw new ValidationException("Assigned employee does not exist.");
            }
            User user = userOpt.get();
            if (user.getRole() != Role.EMPLOYEE) {
                throw new ValidationException("Invalid assignment: Tasks can only be assigned to users with the EMPLOYEE role.");
            }
        }
    }

    // Helper: Validates standard status transition sequence
    private void validateTransition(TaskStatus current, TaskStatus next, Role role) throws ValidationException {
        if (current == next) return;

        // Block status can resolve back to anything or block can be set from anything
        if (next == TaskStatus.BLOCKED) return; 

        switch (current) {
            case TO_DO:
                if (next != TaskStatus.IN_PROGRESS) {
                    throw new ValidationException("Invalid transition: From TO_DO, a task must move to IN_PROGRESS.");
                }
                break;
            case IN_PROGRESS:
                if (next != TaskStatus.TESTING) {
                    throw new ValidationException("Invalid transition: From IN_PROGRESS, a task must move to TESTING.");
                }
                break;
            case TESTING:
                if (next != TaskStatus.COMPLETED && next != TaskStatus.IN_PROGRESS) {
                    throw new ValidationException("Invalid transition: From TESTING, a task must move to COMPLETED (pass) or IN_PROGRESS (fail).");
                }
                break;
            case COMPLETED:
                // Only Manager/Admin can override completed tasks
                if (role == Role.EMPLOYEE) {
                    throw new ValidationException("Access Denied: Employees cannot reopen completed tasks.");
                }
                break;
            case BLOCKED:
                // From blocked state, it can go to any valid next state when resolved.
                break;
        }
    }

    private void sendNotification(int userId, String message) {
        try {
            Notification notif = new Notification();
            notif.setUserId(userId);
            notif.setMessage(message);
            notif.setRead(false);
            notif.setCreatedAt(LocalDateTime.now());
            notificationDAO.create(notif);
        } catch (Exception e) {
            // Suppress notification exceptions to avoid blocking main transaction
            System.err.println("Failed to write notification to database: " + e.getMessage());
        }
    }
}

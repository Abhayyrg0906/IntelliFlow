package com.intelliflow.service.impl;

import com.intelliflow.context.UserSession;
import com.intelliflow.dao.impl.ActivityLogDAOImpl;
import com.intelliflow.dao.impl.ProjectDAOImpl;
import com.intelliflow.dao.interfaces.ActivityLogDAO;
import com.intelliflow.dao.interfaces.ProjectDAO;
import com.intelliflow.enums.Role;
import com.intelliflow.exception.DatabaseException;
import com.intelliflow.exception.UnauthorizedException;
import com.intelliflow.exception.ValidationException;
import com.intelliflow.model.ActivityLog;
import com.intelliflow.model.Project;
import com.intelliflow.model.User;
import com.intelliflow.service.interfaces.ProjectService;
import com.intelliflow.util.ValidationUtil;

import java.util.List;
import java.util.Optional;

public class ProjectServiceImpl implements ProjectService {

    private final ProjectDAO projectDAO;
    private final ActivityLogDAO logDAO;
    private final com.intelliflow.dao.interfaces.NotificationDAO notificationDAO;

    public ProjectServiceImpl() {
        this.projectDAO = new ProjectDAOImpl();
        this.logDAO = new ActivityLogDAOImpl();
        this.notificationDAO = new com.intelliflow.dao.impl.NotificationDAOImpl();
    }

    public ProjectServiceImpl(ProjectDAO projectDAO, ActivityLogDAO logDAO) {
        this.projectDAO = projectDAO;
        this.logDAO = logDAO;
        this.notificationDAO = new com.intelliflow.dao.impl.NotificationDAOImpl();
    }

    public ProjectServiceImpl(ProjectDAO projectDAO, ActivityLogDAO logDAO, com.intelliflow.dao.interfaces.NotificationDAO notificationDAO) {
        this.projectDAO = projectDAO;
        this.logDAO = logDAO;
        this.notificationDAO = notificationDAO;
    }

    private void checkWritePermission() throws UnauthorizedException {
        User user = UserSession.getInstance().getCurrentUser();
        if (user == null || (user.getRole() != Role.ADMIN && user.getRole() != Role.MANAGER)) {
            throw new UnauthorizedException("Access Denied: Only Managers and Administrators can perform this action.");
        }
    }

    @Override
    public Project createProject(Project project) throws ValidationException, DatabaseException, UnauthorizedException {
        checkWritePermission();

        if (project == null) {
            throw new ValidationException("Project details cannot be empty.");
        }
        if (!ValidationUtil.isNotEmpty(project.getName())) {
            throw new ValidationException("Project name is required.");
        }
        ValidationUtil.validateDateRange(project.getStartDate(), project.getDeadline());

        Project created = projectDAO.create(project);

        User currentUser = UserSession.getInstance().getCurrentUser();
        ActivityLog audit = new ActivityLog();
        audit.setUserId(currentUser != null ? currentUser.getId() : null);
        audit.setAction("PROJECT_CREATE");
        audit.setDescription("Created project: " + created.getName() + " (ID: " + created.getId() + ")");
        logDAO.create(audit);

        if (created.getManagerId() != null) {
            sendNotification(created.getManagerId(), "You have been assigned as Manager for project: " + created.getName());
        }

        return created;
    }

    @Override
    public void updateProject(Project project) throws ValidationException, DatabaseException, UnauthorizedException {
        checkWritePermission();

        if (project == null) {
            throw new ValidationException("Project details cannot be empty.");
        }
        if (!ValidationUtil.isNotEmpty(project.getName())) {
            throw new ValidationException("Project name is required.");
        }
        ValidationUtil.validateDateRange(project.getStartDate(), project.getDeadline());

        Optional<Project> originalOpt = projectDAO.findById(project.getId());
        Project original = originalOpt.orElse(null);

        projectDAO.update(project);

        User currentUser = UserSession.getInstance().getCurrentUser();
        ActivityLog audit = new ActivityLog();
        audit.setUserId(currentUser != null ? currentUser.getId() : null);
        audit.setAction("PROJECT_UPDATE");
        audit.setDescription("Updated project: " + project.getName() + " (ID: " + project.getId() + ")");
        logDAO.create(audit);

        if (original != null) {
            // Manager assigned/changed
            if (project.getManagerId() != null && !project.getManagerId().equals(original.getManagerId())) {
                sendNotification(project.getManagerId(), "You have been assigned as Manager for project: " + project.getName());
            }
            // Status changed
            if (project.getStatus() != original.getStatus() && project.getManagerId() != null) {
                sendNotification(project.getManagerId(), "Project '" + project.getName() + "' status changed from " + original.getStatus() + " to " + project.getStatus());
            }
            // Deadline changed
            if (project.getDeadline() != null && !project.getDeadline().equals(original.getDeadline()) && project.getManagerId() != null) {
                sendNotification(project.getManagerId(), "Project '" + project.getName() + "' deadline changed to " + project.getDeadline());
            }
        }
    }

    private void sendNotification(int userId, String message) {
        try {
            com.intelliflow.model.Notification notif = new com.intelliflow.model.Notification();
            notif.setUserId(userId);
            notif.setMessage(message);
            notif.setRead(false);
            notif.setCreatedAt(java.time.LocalDateTime.now());
            notificationDAO.create(notif);
        } catch (Exception e) {
            System.err.println("Failed to write project notification: " + e.getMessage());
        }
    }

    @Override
    public void deleteProject(int projectId) throws DatabaseException, UnauthorizedException {
        checkWritePermission();

        // Check if project exists
        Optional<Project> projOpt = projectDAO.findById(projectId);
        if (projOpt.isPresent()) {
            projectDAO.delete(projectId);

            User currentUser = UserSession.getInstance().getCurrentUser();
            ActivityLog audit = new ActivityLog();
            audit.setUserId(currentUser != null ? currentUser.getId() : null);
            audit.setAction("PROJECT_DELETE");
            audit.setDescription("Deleted project: " + projOpt.get().getName() + " (ID: " + projectId + ")");
            logDAO.create(audit);
        }
    }

    @Override
    public Optional<Project> getProjectById(int id) throws DatabaseException {
        return projectDAO.findById(id);
    }

    @Override
    public List<Project> getAllProjects() throws DatabaseException {
        return projectDAO.findAll();
    }

    @Override
    public List<Project> getProjectsManagedBy(int managerId) throws DatabaseException {
        return projectDAO.findByManagerId(managerId);
    }
}

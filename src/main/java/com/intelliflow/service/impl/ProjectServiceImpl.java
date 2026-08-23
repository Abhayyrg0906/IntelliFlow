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

    public ProjectServiceImpl() {
        this.projectDAO = new ProjectDAOImpl();
        this.logDAO = new ActivityLogDAOImpl();
    }

    public ProjectServiceImpl(ProjectDAO projectDAO, ActivityLogDAO logDAO) {
        this.projectDAO = projectDAO;
        this.logDAO = logDAO;
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

        projectDAO.update(project);

        User currentUser = UserSession.getInstance().getCurrentUser();
        ActivityLog audit = new ActivityLog();
        audit.setUserId(currentUser != null ? currentUser.getId() : null);
        audit.setAction("PROJECT_UPDATE");
        audit.setDescription("Updated project: " + project.getName() + " (ID: " + project.getId() + ")");
        logDAO.create(audit);
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

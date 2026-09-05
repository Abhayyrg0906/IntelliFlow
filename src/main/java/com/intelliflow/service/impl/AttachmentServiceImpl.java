package com.intelliflow.service.impl;

import com.intelliflow.context.UserSession;
import com.intelliflow.dao.impl.*;
import com.intelliflow.dao.interfaces.*;
import com.intelliflow.enums.Role;
import com.intelliflow.exception.DatabaseException;
import com.intelliflow.exception.UnauthorizedException;
import com.intelliflow.exception.ValidationException;
import com.intelliflow.model.*;
import com.intelliflow.service.interfaces.AttachmentService;
import com.intelliflow.util.FileStorageUtil;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentDAO attachmentDAO;
    private final TaskDAO taskDAO;
    private final ProjectDAO projectDAO;
    private final UserDAO userDAO;
    private final NotificationDAO notificationDAO;
    private final ActivityLogDAO logDAO;

    public AttachmentServiceImpl() {
        this.attachmentDAO = new AttachmentDAOImpl();
        this.taskDAO = new TaskDAOImpl();
        this.projectDAO = new ProjectDAOImpl();
        this.userDAO = new UserDAOImpl();
        this.notificationDAO = new NotificationDAOImpl();
        this.logDAO = new ActivityLogDAOImpl();
    }

    public AttachmentServiceImpl(AttachmentDAO attachmentDAO, TaskDAO taskDAO, ProjectDAO projectDAO, UserDAO userDAO, NotificationDAO notificationDAO, ActivityLogDAO logDAO) {
        this.attachmentDAO = attachmentDAO;
        this.taskDAO = taskDAO;
        this.projectDAO = projectDAO;
        this.userDAO = userDAO;
        this.notificationDAO = notificationDAO;
        this.logDAO = logDAO;
    }

    @Override
    public Attachment uploadAttachment(int taskId, File sourceFile) throws ValidationException, DatabaseException, UnauthorizedException {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Session is not active. Please log in.");
        }

        if (sourceFile == null) {
            throw new ValidationException("Source file must not be null.");
        }

        try {
            FileStorageUtil.validateFile(sourceFile);
        } catch (IllegalArgumentException | SecurityException e) {
            throw new ValidationException(e.getMessage(), e);
        }

        Optional<Task> taskOpt = taskDAO.findById(taskId);
        if (taskOpt.isEmpty()) {
            throw new ValidationException("Task not found with ID: " + taskId);
        }
        Task task = taskOpt.get();

        Optional<Project> projectOpt = projectDAO.findById(task.getProjectId());
        Project project = projectOpt.orElse(null);

        // Check task authorization
        checkTaskAccess(currentUser, task, project, "upload attachments to");

        String originalFilename = sourceFile.getName();
        String storedFilename = FileStorageUtil.generateStoredFilename(originalFilename);
        String ext = FileStorageUtil.getFileExtension(originalFilename);

        try {
            FileStorageUtil.saveFile(sourceFile, storedFilename);
        } catch (IOException e) {
            throw new DatabaseException("Failed to save attachment file to disk: " + e.getMessage(), e);
        }

        Attachment attachment = new Attachment();
        attachment.setTaskId(taskId);
        attachment.setUserId(currentUser.getId());
        attachment.setFilename(originalFilename);
        attachment.setStoredFilename(storedFilename);
        attachment.setFileSize(sourceFile.length());
        attachment.setFileType(ext);
        attachment.setCreatedAt(LocalDateTime.now());

        Attachment created = attachmentDAO.create(attachment);
        created.setUploaderName(currentUser.getFullName());
        created.setUploaderRole(currentUser.getRole());

        // Audit Log
        ActivityLog audit = new ActivityLog();
        audit.setUserId(currentUser.getId());
        audit.setAction("TASK_ATTACHMENT_UPLOAD");
        audit.setDescription(currentUser.getFullName() + " attached '" + originalFilename + "' to task '" + task.getName() + "'");
        logDAO.create(audit);

        // Notifications
        // 1. Notify Assignee if not the uploader
        if (task.getAssignedEmployeeId() != null && task.getAssignedEmployeeId() != currentUser.getId()) {
            sendNotification(task.getAssignedEmployeeId(),
                    currentUser.getFullName() + " attached a file '" + originalFilename + "' to your task '" + task.getName() + "'");
        }

        // 2. Notify Manager if not the uploader
        if (project != null && project.getManagerId() != null && project.getManagerId() != currentUser.getId()) {
            sendNotification(project.getManagerId(),
                    currentUser.getFullName() + " attached a file '" + originalFilename + "' to task '" + task.getName() + "'");
        }

        return created;
    }

    @Override
    public List<Attachment> getAttachmentsByTaskId(int taskId) throws ValidationException, DatabaseException, UnauthorizedException {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Session is not active. Please log in.");
        }

        Optional<Task> taskOpt = taskDAO.findById(taskId);
        if (taskOpt.isEmpty()) {
            throw new ValidationException("Task not found with ID: " + taskId);
        }
        Task task = taskOpt.get();

        Optional<Project> projectOpt = projectDAO.findById(task.getProjectId());
        Project project = projectOpt.orElse(null);

        // Check task authorization
        checkTaskAccess(currentUser, task, project, "view attachments for");

        List<Attachment> attachments = attachmentDAO.findByTaskId(taskId);
        for (Attachment a : attachments) {
            Optional<User> uploaderOpt = userDAO.findById(a.getUserId());
            if (uploaderOpt.isPresent()) {
                a.setUploaderName(uploaderOpt.get().getFullName());
                a.setUploaderRole(uploaderOpt.get().getRole());
            } else {
                a.setUploaderName("User #" + a.getUserId());
                a.setUploaderRole(Role.EMPLOYEE);
            }
        }
        return attachments;
    }

    @Override
    public File getAttachmentFile(int attachmentId) throws ValidationException, DatabaseException, UnauthorizedException {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Session is not active. Please log in.");
        }

        Optional<Attachment> attachmentOpt = attachmentDAO.findById(attachmentId);
        if (attachmentOpt.isEmpty()) {
            throw new ValidationException("Attachment not found with ID: " + attachmentId);
        }
        Attachment attachment = attachmentOpt.get();

        Optional<Task> taskOpt = taskDAO.findById(attachment.getTaskId());
        if (taskOpt.isEmpty()) {
            throw new ValidationException("Task not found with ID: " + attachment.getTaskId());
        }
        Task task = taskOpt.get();

        Optional<Project> projectOpt = projectDAO.findById(task.getProjectId());
        Project project = projectOpt.orElse(null);

        // Check access
        checkTaskAccess(currentUser, task, project, "download attachments for");

        try {
            return FileStorageUtil.getStoredFile(attachment.getStoredFilename());
        } catch (Exception e) {
            throw new ValidationException("Unable to retrieve attachment file: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteAttachment(int attachmentId) throws ValidationException, DatabaseException, UnauthorizedException {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Session is not active. Please log in.");
        }

        Optional<Attachment> attachmentOpt = attachmentDAO.findById(attachmentId);
        if (attachmentOpt.isEmpty()) {
            throw new ValidationException("Attachment not found with ID: " + attachmentId);
        }
        Attachment attachment = attachmentOpt.get();

        Optional<Task> taskOpt = taskDAO.findById(attachment.getTaskId());
        Task task = taskOpt.orElse(null);
        String taskName = task != null ? task.getName() : "Task #" + attachment.getTaskId();

        // Authorization check for deletion
        if (currentUser.getRole() != Role.ADMIN && attachment.getUserId() != currentUser.getId()) {
            if (task != null) {
                Optional<Project> projectOpt = projectDAO.findById(task.getProjectId());
                if (projectOpt.isEmpty() || projectOpt.get().getManagerId() == null || !projectOpt.get().getManagerId().equals(currentUser.getId())) {
                    throw new UnauthorizedException("Access Denied: You do not have permission to delete this attachment.");
                }
            } else {
                throw new UnauthorizedException("Access Denied: You do not have permission to delete this attachment.");
            }
        }

        // Delete from disk
        FileStorageUtil.deleteStoredFile(attachment.getStoredFilename());

        // Delete from database
        attachmentDAO.delete(attachmentId);

        // Audit Log
        ActivityLog audit = new ActivityLog();
        audit.setUserId(currentUser.getId());
        audit.setAction("TASK_ATTACHMENT_DELETE");
        audit.setDescription(currentUser.getFullName() + " deleted attachment '" + attachment.getFilename() + "' from task '" + taskName + "'");
        logDAO.create(audit);
    }

    private void checkTaskAccess(User currentUser, Task task, Project project, String actionVerb) throws UnauthorizedException {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }

        if (currentUser.getRole() == Role.MANAGER) {
            if (project != null && project.getManagerId() != null && project.getManagerId().equals(currentUser.getId())) {
                return;
            }
            throw new UnauthorizedException("Access Denied: Managers can only " + actionVerb + " within their managed projects.");
        }

        if (currentUser.getRole() == Role.EMPLOYEE) {
            if (task.getAssignedEmployeeId() != null && task.getAssignedEmployeeId().equals(currentUser.getId())) {
                return;
            }
            throw new UnauthorizedException("Access Denied: Employees can only " + actionVerb + " tasks assigned to them.");
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
            System.err.println("Failed to create attachment notification: " + e.getMessage());
        }
    }
}

package com.intelliflow.service.impl;

import com.intelliflow.context.UserSession;
import com.intelliflow.dao.impl.*;
import com.intelliflow.dao.interfaces.*;
import com.intelliflow.enums.Role;
import com.intelliflow.exception.DatabaseException;
import com.intelliflow.exception.UnauthorizedException;
import com.intelliflow.exception.ValidationException;
import com.intelliflow.model.*;
import com.intelliflow.service.interfaces.CommentService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class CommentServiceImpl implements CommentService {

    private final CommentDAO commentDAO;
    private final TaskDAO taskDAO;
    private final ProjectDAO projectDAO;
    private final UserDAO userDAO;
    private final NotificationDAO notificationDAO;
    private final ActivityLogDAO logDAO;

    public CommentServiceImpl() {
        this.commentDAO = new CommentDAOImpl();
        this.taskDAO = new TaskDAOImpl();
        this.projectDAO = new ProjectDAOImpl();
        this.userDAO = new UserDAOImpl();
        this.notificationDAO = new NotificationDAOImpl();
        this.logDAO = new ActivityLogDAOImpl();
    }

    public CommentServiceImpl(CommentDAO commentDAO, TaskDAO taskDAO, ProjectDAO projectDAO, UserDAO userDAO, NotificationDAO notificationDAO, ActivityLogDAO logDAO) {
        this.commentDAO = commentDAO;
        this.taskDAO = taskDAO;
        this.projectDAO = projectDAO;
        this.userDAO = userDAO;
        this.notificationDAO = notificationDAO;
        this.logDAO = logDAO;
    }

    @Override
    public Comment addComment(int taskId, String content) throws ValidationException, DatabaseException, UnauthorizedException {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Session is not active. Please log in.");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new ValidationException("Comment cannot be empty.");
        }

        Optional<Task> taskOpt = taskDAO.findById(taskId);
        if (taskOpt.isEmpty()) {
            throw new ValidationException("Task not found with ID: " + taskId);
        }
        Task task = taskOpt.get();

        Optional<Project> projectOpt = projectDAO.findById(task.getProjectId());
        Project project = projectOpt.orElse(null);

        // Authorization check
        checkTaskAccess(currentUser, task, project, "add comments to");

        Comment comment = new Comment();
        comment.setTaskId(taskId);
        comment.setUserId(currentUser.getId());
        comment.setContent(content.trim());
        comment.setCreatedAt(LocalDateTime.now());

        Comment created = commentDAO.create(comment);
        created.setAuthorName(currentUser.getFullName());
        created.setAuthorRole(currentUser.getRole());

        // Audit Log
        ActivityLog audit = new ActivityLog();
        audit.setUserId(currentUser.getId());
        audit.setAction("TASK_COMMENT");
        audit.setDescription(currentUser.getFullName() + " commented on task '" + task.getName() + "'");
        logDAO.create(audit);

        // Notifications
        String preview = content.trim();
        if (preview.length() > 45) {
            preview = preview.substring(0, 42) + "...";
        }

        // 1. Notify Assignee if author is not the assignee
        if (task.getAssignedEmployeeId() != null && task.getAssignedEmployeeId() != currentUser.getId()) {
            sendNotification(task.getAssignedEmployeeId(),
                    currentUser.getFullName() + " commented on your task '" + task.getName() + "': \"" + preview + "\"");
        }

        // 2. Notify Manager if author is not the manager
        if (project != null && project.getManagerId() != null && project.getManagerId() != currentUser.getId()) {
            sendNotification(project.getManagerId(),
                    currentUser.getFullName() + " commented on task '" + task.getName() + "': \"" + preview + "\"");
        }

        return created;
    }

    @Override
    public List<Comment> getCommentsByTaskId(int taskId) throws ValidationException, DatabaseException, UnauthorizedException {
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

        // Authorization check
        checkTaskAccess(currentUser, task, project, "view comments for");

        List<Comment> comments = commentDAO.findByTaskId(taskId);
        for (Comment c : comments) {
            Optional<User> authorOpt = userDAO.findById(c.getUserId());
            if (authorOpt.isPresent()) {
                c.setAuthorName(authorOpt.get().getFullName());
                c.setAuthorRole(authorOpt.get().getRole());
            } else {
                c.setAuthorName("User #" + c.getUserId());
                c.setAuthorRole(Role.EMPLOYEE);
            }
        }
        return comments;
    }

    @Override
    public void deleteComment(int commentId) throws ValidationException, DatabaseException, UnauthorizedException {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Session is not active. Please log in.");
        }

        Optional<Comment> commentOpt = commentDAO.findById(commentId);
        if (commentOpt.isEmpty()) {
            throw new ValidationException("Comment not found with ID: " + commentId);
        }
        Comment comment = commentOpt.get();

        // Only comment author, project manager, or admin can delete comment
        if (currentUser.getRole() != Role.ADMIN && comment.getUserId() != currentUser.getId()) {
            Optional<Task> taskOpt = taskDAO.findById(comment.getTaskId());
            if (taskOpt.isPresent()) {
                Optional<Project> projectOpt = projectDAO.findById(taskOpt.get().getProjectId());
                if (projectOpt.isEmpty() || projectOpt.get().getManagerId() == null || !projectOpt.get().getManagerId().equals(currentUser.getId())) {
                    throw new UnauthorizedException("Access Denied: You do not have permission to delete this comment.");
                }
            } else {
                throw new UnauthorizedException("Access Denied: You do not have permission to delete this comment.");
            }
        }

        commentDAO.delete(commentId);
    }

    private void checkTaskAccess(User currentUser, Task task, Project project, String actionVerb) throws UnauthorizedException {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }

        if (currentUser.getRole() == Role.MANAGER) {
            if (project != null && project.getManagerId() != null && project.getManagerId().equals(currentUser.getId())) {
                return;
            }
            throw new UnauthorizedException("Access Denied: Managers can only " + actionVerb + " tasks within their managed projects.");
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
            System.err.println("Failed to create comment notification: " + e.getMessage());
        }
    }
}

package com.intelliflow.service.impl;

import com.intelliflow.dao.impl.NotificationDAOImpl;
import com.intelliflow.dao.impl.ProjectDAOImpl;
import com.intelliflow.dao.impl.TaskDAOImpl;
import com.intelliflow.dao.interfaces.NotificationDAO;
import com.intelliflow.dao.interfaces.ProjectDAO;
import com.intelliflow.dao.interfaces.TaskDAO;
import com.intelliflow.enums.DeadlineState;
import com.intelliflow.enums.TaskStatus;
import com.intelliflow.exception.DatabaseException;
import com.intelliflow.model.Notification;
import com.intelliflow.model.Project;
import com.intelliflow.model.Task;
import com.intelliflow.service.interfaces.NotificationService;
import com.intelliflow.util.DeadlineUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NotificationServiceImpl implements NotificationService {

    private final NotificationDAO notificationDAO;
    private final TaskDAO taskDAO;
    private final ProjectDAO projectDAO;

    public NotificationServiceImpl() {
        this.notificationDAO = new NotificationDAOImpl();
        this.taskDAO = new TaskDAOImpl();
        this.projectDAO = new ProjectDAOImpl();
    }

    public NotificationServiceImpl(NotificationDAO notificationDAO) {
        this.notificationDAO = notificationDAO;
        this.taskDAO = new TaskDAOImpl();
        this.projectDAO = new ProjectDAOImpl();
    }

    public NotificationServiceImpl(NotificationDAO notificationDAO, TaskDAO taskDAO, ProjectDAO projectDAO) {
        this.notificationDAO = notificationDAO;
        this.taskDAO = taskDAO;
        this.projectDAO = projectDAO;
    }

    @Override
    public List<Notification> getNotificationsForUser(int userId) throws DatabaseException {
        return notificationDAO.findByUserId(userId);
    }

    @Override
    public List<Notification> getUnreadNotificationsForUser(int userId) throws DatabaseException {
        return notificationDAO.findUnreadByUserId(userId);
    }

    @Override
    public Notification createNotification(int userId, String message) throws DatabaseException {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setMessage(message);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        return notificationDAO.create(notification);
    }

    @Override
    public Notification createNotificationIfNotExists(int userId, String message) throws DatabaseException {
        if (notificationDAO.existsUnread(userId, message)) {
            return null; // Prevent duplicate unread notifications
        }
        return createNotification(userId, message);
    }

    @Override
    public void markAsRead(int notificationId) throws DatabaseException {
        notificationDAO.markAsRead(notificationId);
    }

    @Override
    public void markAllAsRead(int userId) throws DatabaseException {
        notificationDAO.markAllAsRead(userId);
    }

    @Override
    public void deleteNotification(int notificationId) throws DatabaseException {
        notificationDAO.delete(notificationId);
    }

    @Override
    public void deleteAllNotificationsForUser(int userId) throws DatabaseException {
        notificationDAO.deleteAllByUserId(userId);
    }

    @Override
    public int checkAndGenerateDeadlineNotifications(LocalDate referenceDate) throws DatabaseException {
        if (referenceDate == null) referenceDate = LocalDate.now();
        List<Task> allTasks = taskDAO.findAll();
        List<Project> allProjects = projectDAO.findAll();
        Map<Integer, Project> projectMap = allProjects.stream().collect(Collectors.toMap(Project::getId, p -> p, (p1, p2) -> p1));

        int createdCount = 0;
        for (Task t : allTasks) {
            if (t.getStatus() == TaskStatus.COMPLETED) continue;

            DeadlineState state = DeadlineUtil.calculateDeadlineState(t, referenceDate);
            Project p = projectMap.get(t.getProjectId());
            Integer managerId = (p != null) ? p.getManagerId() : null;
            Integer assigneeId = t.getAssignedEmployeeId();

            if (state == DeadlineState.OVERDUE) {
                if (assigneeId != null) {
                    Notification n = createNotificationIfNotExists(assigneeId, "⛔ Task overdue: '" + t.getName() + "' passed its deadline on " + t.getDeadline());
                    if (n != null) createdCount++;
                }
                if (managerId != null && !managerId.equals(assigneeId)) {
                    Notification n = createNotificationIfNotExists(managerId, "⛔ Task overdue in your project: '" + t.getName() + "' passed its deadline on " + t.getDeadline());
                    if (n != null) createdCount++;
                }
            } else if (state == DeadlineState.DUE_TODAY) {
                if (assigneeId != null) {
                    Notification n = createNotificationIfNotExists(assigneeId, "🔥 Task due today: '" + t.getName() + "' is due today!");
                    if (n != null) createdCount++;
                }
            } else if (state == DeadlineState.DUE_SOON) {
                if (assigneeId != null) {
                    Notification n = createNotificationIfNotExists(assigneeId, "⚠️ Task approaching deadline: '" + t.getName() + "' is due on " + t.getDeadline());
                    if (n != null) createdCount++;
                }
            }
        }
        return createdCount;
    }
}

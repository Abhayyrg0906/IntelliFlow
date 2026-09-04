package com.intelliflow.service.interfaces;

import com.intelliflow.exception.DatabaseException;
import com.intelliflow.model.Notification;
import java.time.LocalDate;
import java.util.List;

public interface NotificationService {
    List<Notification> getNotificationsForUser(int userId) throws DatabaseException;
    List<Notification> getUnreadNotificationsForUser(int userId) throws DatabaseException;
    Notification createNotification(int userId, String message) throws DatabaseException;
    Notification createNotificationIfNotExists(int userId, String message) throws DatabaseException;
    void markAsRead(int notificationId) throws DatabaseException;
    void markAllAsRead(int userId) throws DatabaseException;
    void deleteNotification(int notificationId) throws DatabaseException;
    void deleteAllNotificationsForUser(int userId) throws DatabaseException;
    int checkAndGenerateDeadlineNotifications(LocalDate referenceDate) throws DatabaseException;
}

package com.intelliflow.service.interfaces;

import com.intelliflow.exception.DatabaseException;
import com.intelliflow.model.Notification;
import java.util.List;

public interface NotificationService {
    List<Notification> getNotificationsForUser(int userId) throws DatabaseException;
    List<Notification> getUnreadNotificationsForUser(int userId) throws DatabaseException;
    void markAsRead(int notificationId) throws DatabaseException;
    void markAllAsRead(int userId) throws DatabaseException;
    void deleteNotification(int notificationId) throws DatabaseException;
}

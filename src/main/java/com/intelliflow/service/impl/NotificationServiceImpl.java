package com.intelliflow.service.impl;

import com.intelliflow.dao.impl.NotificationDAOImpl;
import com.intelliflow.dao.interfaces.NotificationDAO;
import com.intelliflow.exception.DatabaseException;
import com.intelliflow.model.Notification;
import com.intelliflow.service.interfaces.NotificationService;

import java.util.List;

public class NotificationServiceImpl implements NotificationService {

    private final NotificationDAO notificationDAO;

    public NotificationServiceImpl() {
        this.notificationDAO = new NotificationDAOImpl();
    }

    public NotificationServiceImpl(NotificationDAO notificationDAO) {
        this.notificationDAO = notificationDAO;
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
}

package com.intelliflow.dao.interfaces;

import com.intelliflow.exception.DatabaseException;
import com.intelliflow.model.Notification;
import java.util.List;
import java.util.Optional;

public interface NotificationDAO {
    Optional<Notification> findById(int id) throws DatabaseException;
    List<Notification> findByUserId(int userId) throws DatabaseException;
    List<Notification> findUnreadByUserId(int userId) throws DatabaseException;
    Notification create(Notification notification) throws DatabaseException;
    void markAsRead(int id) throws DatabaseException;
    void markAllAsRead(int userId) throws DatabaseException;
    void delete(int id) throws DatabaseException;
}

package com.intelliflow.dao.impl;

import com.intelliflow.dao.interfaces.NotificationDAO;
import com.intelliflow.exception.DatabaseException;
import com.intelliflow.model.Notification;
import com.intelliflow.util.DBUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NotificationDAOImpl implements NotificationDAO {

    private Notification mapRowToNotification(ResultSet rs) throws SQLException {
        Notification notification = new Notification();
        notification.setId(rs.getInt("id"));
        notification.setUserId(rs.getInt("user_id"));
        notification.setMessage(rs.getString("message"));
        notification.setRead(rs.getBoolean("is_read"));
        notification.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return notification;
    }

    @Override
    public Optional<Notification> findById(int id) throws DatabaseException {
        String sql = "SELECT * FROM notifications WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToNotification(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding notification by ID: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Notification> findByUserId(int userId) throws DatabaseException {
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapRowToNotification(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding notifications for user ID: " + userId, e);
        }
        return notifications;
    }

    @Override
    public List<Notification> findUnreadByUserId(int userId) throws DatabaseException {
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? AND is_read = FALSE ORDER BY created_at DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapRowToNotification(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding unread notifications for user ID: " + userId, e);
        }
        return notifications;
    }

    @Override
    public Notification create(Notification notification) throws DatabaseException {
        String sql = "INSERT INTO notifications (user_id, message, is_read, created_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, notification.getUserId());
            pstmt.setString(2, notification.getMessage());
            pstmt.setBoolean(3, notification.isRead());
            
            LocalDateTime now = notification.getCreatedAt() != null ? notification.getCreatedAt() : LocalDateTime.now();
            pstmt.setObject(4, now);
            notification.setCreatedAt(now);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseException("Creating notification failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    notification.setId(generatedKeys.getInt(1));
                } else {
                    throw new DatabaseException("Creating notification failed, no ID obtained.");
                }
            }
            return notification;
        } catch (SQLException e) {
            throw new DatabaseException("Error creating notification for user ID: " + notification.getUserId(), e);
        }
    }

    @Override
    public void markAsRead(int id) throws DatabaseException {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error marking notification as read for ID: " + id, e);
        }
    }

    @Override
    public void markAllAsRead(int userId) throws DatabaseException {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error marking all notifications as read for user ID: " + userId, e);
        }
    }

    @Override
    public void delete(int id) throws DatabaseException {
        String sql = "DELETE FROM notifications WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting notification with ID: " + id, e);
        }
    }
}

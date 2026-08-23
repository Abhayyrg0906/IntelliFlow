package com.intelliflow.dao.impl;

import com.intelliflow.dao.interfaces.ActivityLogDAO;
import com.intelliflow.exception.DatabaseException;
import com.intelliflow.model.ActivityLog;
import com.intelliflow.util.DBUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDAOImpl implements ActivityLogDAO {

    private ActivityLog mapRowToActivityLog(ResultSet rs) throws SQLException {
        ActivityLog log = new ActivityLog();
        log.setId(rs.getInt("id"));
        
        int userIdVal = rs.getInt("user_id");
        log.setUserId(rs.wasNull() ? null : userIdVal);
        
        log.setAction(rs.getString("action"));
        log.setDescription(rs.getString("description"));
        log.setTimestamp(rs.getObject("timestamp", LocalDateTime.class));
        return log;
    }

    @Override
    public List<ActivityLog> findAll() throws DatabaseException {
        List<ActivityLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM activity_logs ORDER BY timestamp DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                logs.add(mapRowToActivityLog(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving all activity logs", e);
        }
        return logs;
    }

    @Override
    public List<ActivityLog> findByUserId(int userId) throws DatabaseException {
        List<ActivityLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM activity_logs WHERE user_id = ? ORDER BY timestamp DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRowToActivityLog(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding activity logs for user ID: " + userId, e);
        }
        return logs;
    }

    @Override
    public ActivityLog create(ActivityLog log) throws DatabaseException {
        String sql = "INSERT INTO activity_logs (user_id, action, description, timestamp) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            if (log.getUserId() != null) {
                pstmt.setInt(1, log.getUserId());
            } else {
                pstmt.setNull(1, Types.INTEGER);
            }
            
            pstmt.setString(2, log.getAction());
            pstmt.setString(3, log.getDescription());
            
            LocalDateTime now = log.getTimestamp() != null ? log.getTimestamp() : LocalDateTime.now();
            pstmt.setObject(4, now);
            log.setTimestamp(now);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseException("Creating activity log failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    log.setId(generatedKeys.getInt(1));
                } else {
                    throw new DatabaseException("Creating activity log failed, no ID obtained.");
                }
            }
            return log;
        } catch (SQLException e) {
            throw new DatabaseException("Error creating activity log", e);
        }
    }
}

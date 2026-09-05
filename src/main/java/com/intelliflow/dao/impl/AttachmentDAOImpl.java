package com.intelliflow.dao.impl;

import com.intelliflow.dao.interfaces.AttachmentDAO;
import com.intelliflow.exception.DatabaseException;
import com.intelliflow.model.Attachment;
import com.intelliflow.util.DBUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AttachmentDAOImpl implements AttachmentDAO {

    private Attachment mapRowToAttachment(ResultSet rs) throws SQLException {
        Attachment attachment = new Attachment();
        attachment.setId(rs.getInt("id"));
        attachment.setTaskId(rs.getInt("task_id"));
        attachment.setUserId(rs.getInt("user_id"));
        attachment.setFilename(rs.getString("filename"));
        attachment.setStoredFilename(rs.getString("stored_filename"));
        attachment.setFileSize(rs.getLong("file_size"));
        attachment.setFileType(rs.getString("file_type"));
        attachment.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return attachment;
    }

    @Override
    public Optional<Attachment> findById(int id) throws DatabaseException {
        String sql = "SELECT * FROM task_attachments WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToAttachment(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding attachment by ID: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Attachment> findByTaskId(int taskId) throws DatabaseException {
        List<Attachment> attachments = new ArrayList<>();
        String sql = "SELECT * FROM task_attachments WHERE task_id = ? ORDER BY created_at ASC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, taskId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    attachments.add(mapRowToAttachment(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving attachments for task ID: " + taskId, e);
        }
        return attachments;
    }

    @Override
    public List<Attachment> findAll() throws DatabaseException {
        List<Attachment> attachments = new ArrayList<>();
        String sql = "SELECT * FROM task_attachments ORDER BY created_at ASC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                attachments.add(mapRowToAttachment(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving all task attachments", e);
        }
        return attachments;
    }

    @Override
    public Attachment create(Attachment attachment) throws DatabaseException {
        String sql = "INSERT INTO task_attachments (task_id, user_id, filename, stored_filename, file_size, file_type, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, attachment.getTaskId());
            pstmt.setInt(2, attachment.getUserId());
            pstmt.setString(3, attachment.getFilename());
            pstmt.setString(4, attachment.getStoredFilename());
            pstmt.setLong(5, attachment.getFileSize());
            pstmt.setString(6, attachment.getFileType());

            LocalDateTime now = attachment.getCreatedAt() != null ? attachment.getCreatedAt() : LocalDateTime.now();
            pstmt.setObject(7, now);
            attachment.setCreatedAt(now);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseException("Creating attachment failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    attachment.setId(generatedKeys.getInt(1));
                } else {
                    throw new DatabaseException("Creating attachment failed, no ID obtained.");
                }
            }
            return attachment;
        } catch (SQLException e) {
            throw new DatabaseException("Error creating task attachment", e);
        }
    }

    @Override
    public void delete(int id) throws DatabaseException {
        String sql = "DELETE FROM task_attachments WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting attachment with ID: " + id, e);
        }
    }
}

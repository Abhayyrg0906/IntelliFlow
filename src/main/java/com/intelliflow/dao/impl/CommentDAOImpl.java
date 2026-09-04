package com.intelliflow.dao.impl;

import com.intelliflow.dao.interfaces.CommentDAO;
import com.intelliflow.exception.DatabaseException;
import com.intelliflow.model.Comment;
import com.intelliflow.util.DBUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CommentDAOImpl implements CommentDAO {

    private Comment mapRowToComment(ResultSet rs) throws SQLException {
        Comment comment = new Comment();
        comment.setId(rs.getInt("id"));
        comment.setTaskId(rs.getInt("task_id"));
        comment.setUserId(rs.getInt("user_id"));
        comment.setContent(rs.getString("content"));
        comment.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return comment;
    }

    @Override
    public Optional<Comment> findById(int id) throws DatabaseException {
        String sql = "SELECT * FROM task_comments WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToComment(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding comment by ID: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Comment> findByTaskId(int taskId) throws DatabaseException {
        List<Comment> comments = new ArrayList<>();
        String sql = "SELECT * FROM task_comments WHERE task_id = ? ORDER BY created_at ASC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, taskId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    comments.add(mapRowToComment(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving comments for task ID: " + taskId, e);
        }
        return comments;
    }

    @Override
    public List<Comment> findAll() throws DatabaseException {
        List<Comment> comments = new ArrayList<>();
        String sql = "SELECT * FROM task_comments ORDER BY created_at ASC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                comments.add(mapRowToComment(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving all task comments", e);
        }
        return comments;
    }

    @Override
    public Comment create(Comment comment) throws DatabaseException {
        String sql = "INSERT INTO task_comments (task_id, user_id, content, created_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, comment.getTaskId());
            pstmt.setInt(2, comment.getUserId());
            pstmt.setString(3, comment.getContent());

            LocalDateTime now = comment.getCreatedAt() != null ? comment.getCreatedAt() : LocalDateTime.now();
            pstmt.setObject(4, now);
            comment.setCreatedAt(now);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseException("Creating comment failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    comment.setId(generatedKeys.getInt(1));
                } else {
                    throw new DatabaseException("Creating comment failed, no ID obtained.");
                }
            }
            return comment;
        } catch (SQLException e) {
            throw new DatabaseException("Error creating comment", e);
        }
    }

    @Override
    public void delete(int id) throws DatabaseException {
        String sql = "DELETE FROM task_comments WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting comment with ID: " + id, e);
        }
    }
}

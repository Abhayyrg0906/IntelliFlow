package com.intelliflow.dao.impl;

import com.intelliflow.dao.interfaces.UserDAO;
import com.intelliflow.enums.Role;
import com.intelliflow.exception.DatabaseException;
import com.intelliflow.model.User;
import com.intelliflow.util.DBUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAOImpl implements UserDAO {

    private User mapRowToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(Role.fromString(rs.getString("role")));
        user.setFullName(rs.getString("full_name"));
        user.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return user;
    }

    @Override
    public Optional<User> findById(int id) throws DatabaseException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding user by ID: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByUsername(String username) throws DatabaseException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding user by username: " + username, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByEmail(String email) throws DatabaseException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding user by email: " + email, e);
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() throws DatabaseException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY full_name ASC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                users.add(mapRowToUser(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving all users", e);
        }
        return users;
    }

    @Override
    public User create(User user) throws DatabaseException {
        String sql = "INSERT INTO users (username, email, password_hash, role, full_name, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getPasswordHash());
            pstmt.setString(4, user.getRole().name());
            pstmt.setString(5, user.getFullName());
            
            LocalDateTime now = user.getCreatedAt() != null ? user.getCreatedAt() : LocalDateTime.now();
            pstmt.setObject(6, now);
            user.setCreatedAt(now);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseException("Creating user failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getInt(1));
                } else {
                    throw new DatabaseException("Creating user failed, no ID obtained.");
                }
            }
            return user;
        } catch (SQLException e) {
            throw new DatabaseException("Error creating user: " + user.getUsername(), e);
        }
    }

    @Override
    public void update(User user) throws DatabaseException {
        String sql = "UPDATE users SET username = ?, email = ?, password_hash = ?, role = ?, full_name = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getPasswordHash());
            pstmt.setString(4, user.getRole().name());
            pstmt.setString(5, user.getFullName());
            pstmt.setInt(6, user.getId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseException("Updating user failed, user not found with ID: " + user.getId());
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error updating user: " + user.getUsername(), e);
        }
    }

    @Override
    public void delete(int id) throws DatabaseException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting user with ID: " + id, e);
        }
    }
}

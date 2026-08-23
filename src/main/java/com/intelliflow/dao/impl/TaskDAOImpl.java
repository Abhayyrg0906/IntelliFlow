package com.intelliflow.dao.impl;

import com.intelliflow.dao.interfaces.TaskDAO;
import com.intelliflow.enums.TaskPriority;
import com.intelliflow.enums.TaskStatus;
import com.intelliflow.exception.DatabaseException;
import com.intelliflow.model.Task;
import com.intelliflow.util.DBUtil;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TaskDAOImpl implements TaskDAO {

    private Task mapRowToTask(ResultSet rs) throws SQLException {
        Task task = new Task();
        task.setId(rs.getInt("id"));
        task.setProjectId(rs.getInt("project_id"));
        task.setName(rs.getString("name"));
        task.setDescription(rs.getString("description"));
        
        int empIdVal = rs.getInt("assigned_employee_id");
        task.setAssignedEmployeeId(rs.wasNull() ? null : empIdVal);
        
        task.setPriority(TaskPriority.fromString(rs.getString("priority")));
        
        Date deadlineSql = rs.getDate("deadline");
        task.setDeadline(deadlineSql != null ? deadlineSql.toLocalDate() : null);
        
        task.setStatus(TaskStatus.fromString(rs.getString("status")));
        task.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        task.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        return task;
    }

    @Override
    public Optional<Task> findById(int id) throws DatabaseException {
        String sql = "SELECT * FROM tasks WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToTask(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding task by ID: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Task> findByProjectId(int projectId) throws DatabaseException {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks WHERE project_id = ? ORDER BY deadline ASC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, projectId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tasks.add(mapRowToTask(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding tasks for project ID: " + projectId, e);
        }
        return tasks;
    }

    @Override
    public List<Task> findByEmployeeId(int employeeId) throws DatabaseException {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks WHERE assigned_employee_id = ? ORDER BY deadline ASC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, employeeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tasks.add(mapRowToTask(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding tasks for employee ID: " + employeeId, e);
        }
        return tasks;
    }

    @Override
    public List<Task> findAll() throws DatabaseException {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks ORDER BY deadline ASC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                tasks.add(mapRowToTask(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving all tasks", e);
        }
        return tasks;
    }

    @Override
    public Task create(Task task) throws DatabaseException {
        String sql = "INSERT INTO tasks (project_id, name, description, assigned_employee_id, priority, deadline, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, task.getProjectId());
            pstmt.setString(2, task.getName());
            pstmt.setString(3, task.getDescription());
            
            if (task.getAssignedEmployeeId() != null) {
                pstmt.setInt(4, task.getAssignedEmployeeId());
            } else {
                pstmt.setNull(4, Types.INTEGER);
            }
            
            pstmt.setString(5, task.getPriority().name());
            pstmt.setDate(6, task.getDeadline() != null ? Date.valueOf(task.getDeadline()) : null);
            pstmt.setString(7, task.getStatus().name());
            
            LocalDateTime now = task.getCreatedAt() != null ? task.getCreatedAt() : LocalDateTime.now();
            pstmt.setObject(8, now);
            pstmt.setObject(9, now);
            task.setCreatedAt(now);
            task.setUpdatedAt(now);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseException("Creating task failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    task.setId(generatedKeys.getInt(1));
                } else {
                    throw new DatabaseException("Creating task failed, no ID obtained.");
                }
            }
            return task;
        } catch (SQLException e) {
            throw new DatabaseException("Error creating task: " + task.getName(), e);
        }
    }

    @Override
    public void update(Task task) throws DatabaseException {
        String sql = "UPDATE tasks SET project_id = ?, name = ?, description = ?, assigned_employee_id = ?, priority = ?, deadline = ?, status = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, task.getProjectId());
            pstmt.setString(2, task.getName());
            pstmt.setString(3, task.getDescription());
            
            if (task.getAssignedEmployeeId() != null) {
                pstmt.setInt(4, task.getAssignedEmployeeId());
            } else {
                pstmt.setNull(4, Types.INTEGER);
            }
            
            pstmt.setString(5, task.getPriority().name());
            pstmt.setDate(6, task.getDeadline() != null ? Date.valueOf(task.getDeadline()) : null);
            pstmt.setString(7, task.getStatus().name());
            
            LocalDateTime now = LocalDateTime.now();
            pstmt.setObject(8, now);
            task.setUpdatedAt(now);
            
            pstmt.setInt(9, task.getId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseException("Updating task failed, task not found with ID: " + task.getId());
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error updating task: " + task.getName(), e);
        }
    }

    @Override
    public void delete(int id) throws DatabaseException {
        String sql = "DELETE FROM tasks WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting task with ID: " + id, e);
        }
    }
}

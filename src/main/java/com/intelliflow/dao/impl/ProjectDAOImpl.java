package com.intelliflow.dao.impl;

import com.intelliflow.dao.interfaces.ProjectDAO;
import com.intelliflow.enums.ProjectStatus;
import com.intelliflow.exception.DatabaseException;
import com.intelliflow.model.Project;
import com.intelliflow.util.DBUtil;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProjectDAOImpl implements ProjectDAO {

    private Project mapRowToProject(ResultSet rs) throws SQLException {
        Project project = new Project();
        project.setId(rs.getInt("id"));
        project.setName(rs.getString("name"));
        project.setDescription(rs.getString("description"));
        
        int managerIdVal = rs.getInt("manager_id");
        project.setManagerId(rs.wasNull() ? null : managerIdVal);
        
        Date startDateSql = rs.getDate("start_date");
        project.setStartDate(startDateSql != null ? startDateSql.toLocalDate() : null);
        
        Date deadlineSql = rs.getDate("deadline");
        project.setDeadline(deadlineSql != null ? deadlineSql.toLocalDate() : null);
        
        project.setStatus(ProjectStatus.fromString(rs.getString("status")));
        project.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return project;
    }

    @Override
    public Optional<Project> findById(int id) throws DatabaseException {
        String sql = "SELECT * FROM projects WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToProject(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding project by ID: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Project> findAll() throws DatabaseException {
        List<Project> projects = new ArrayList<>();
        String sql = "SELECT * FROM projects ORDER BY created_at DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                projects.add(mapRowToProject(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving all projects", e);
        }
        return projects;
    }

    @Override
    public List<Project> findByManagerId(int managerId) throws DatabaseException {
        List<Project> projects = new ArrayList<>();
        String sql = "SELECT * FROM projects WHERE manager_id = ? ORDER BY created_at DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, managerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    projects.add(mapRowToProject(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding projects for manager ID: " + managerId, e);
        }
        return projects;
    }

    @Override
    public Project create(Project project) throws DatabaseException {
        String sql = "INSERT INTO projects (name, description, manager_id, start_date, deadline, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, project.getName());
            pstmt.setString(2, project.getDescription());
            
            if (project.getManagerId() != null) {
                pstmt.setInt(3, project.getManagerId());
            } else {
                pstmt.setNull(3, Types.INTEGER);
            }
            
            pstmt.setDate(4, project.getStartDate() != null ? Date.valueOf(project.getStartDate()) : null);
            pstmt.setDate(5, project.getDeadline() != null ? Date.valueOf(project.getDeadline()) : null);
            pstmt.setString(6, project.getStatus().name());
            
            LocalDateTime now = project.getCreatedAt() != null ? project.getCreatedAt() : LocalDateTime.now();
            pstmt.setObject(7, now);
            project.setCreatedAt(now);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseException("Creating project failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    project.setId(generatedKeys.getInt(1));
                } else {
                    throw new DatabaseException("Creating project failed, no ID obtained.");
                }
            }
            return project;
        } catch (SQLException e) {
            throw new DatabaseException("Error creating project: " + project.getName(), e);
        }
    }

    @Override
    public void update(Project project) throws DatabaseException {
        String sql = "UPDATE projects SET name = ?, description = ?, manager_id = ?, start_date = ?, deadline = ?, status = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, project.getName());
            pstmt.setString(2, project.getDescription());
            
            if (project.getManagerId() != null) {
                pstmt.setInt(3, project.getManagerId());
            } else {
                pstmt.setNull(3, Types.INTEGER);
            }
            
            pstmt.setDate(4, project.getStartDate() != null ? Date.valueOf(project.getStartDate()) : null);
            pstmt.setDate(5, project.getDeadline() != null ? Date.valueOf(project.getDeadline()) : null);
            pstmt.setString(6, project.getStatus().name());
            pstmt.setInt(7, project.getId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseException("Updating project failed, project not found with ID: " + project.getId());
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error updating project: " + project.getName(), e);
        }
    }

    @Override
    public void delete(int id) throws DatabaseException {
        String sql = "DELETE FROM projects WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting project with ID: " + id, e);
        }
    }
}

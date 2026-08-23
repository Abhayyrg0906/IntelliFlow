package com.intelliflow.service.interfaces;

import com.intelliflow.exception.DatabaseException;
import com.intelliflow.exception.ValidationException;
import com.intelliflow.exception.UnauthorizedException;
import com.intelliflow.model.Project;
import java.util.List;
import java.util.Optional;

public interface ProjectService {
    Project createProject(Project project) throws ValidationException, DatabaseException, UnauthorizedException;
    void updateProject(Project project) throws ValidationException, DatabaseException, UnauthorizedException;
    void deleteProject(int projectId) throws DatabaseException, UnauthorizedException;
    Optional<Project> getProjectById(int id) throws DatabaseException;
    List<Project> getAllProjects() throws DatabaseException;
    List<Project> getProjectsManagedBy(int managerId) throws DatabaseException;
}

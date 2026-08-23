package com.intelliflow.dao.interfaces;

import com.intelliflow.exception.DatabaseException;
import com.intelliflow.model.Project;
import java.util.List;
import java.util.Optional;

public interface ProjectDAO {
    Optional<Project> findById(int id) throws DatabaseException;
    List<Project> findAll() throws DatabaseException;
    List<Project> findByManagerId(int managerId) throws DatabaseException;
    Project create(Project project) throws DatabaseException;
    void update(Project project) throws DatabaseException;
    void delete(int id) throws DatabaseException;
}

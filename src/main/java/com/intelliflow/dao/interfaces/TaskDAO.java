package com.intelliflow.dao.interfaces;

import com.intelliflow.exception.DatabaseException;
import com.intelliflow.model.Task;
import java.util.List;
import java.util.Optional;

public interface TaskDAO {
    Optional<Task> findById(int id) throws DatabaseException;
    List<Task> findByProjectId(int projectId) throws DatabaseException;
    List<Task> findByEmployeeId(int employeeId) throws DatabaseException;
    List<Task> findAll() throws DatabaseException;
    Task create(Task task) throws DatabaseException;
    void update(Task task) throws DatabaseException;
    void delete(int id) throws DatabaseException;
}

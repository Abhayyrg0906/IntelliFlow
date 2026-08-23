package com.intelliflow.service.interfaces;

import com.intelliflow.enums.TaskStatus;
import com.intelliflow.exception.DatabaseException;
import com.intelliflow.exception.ValidationException;
import com.intelliflow.exception.UnauthorizedException;
import com.intelliflow.model.Task;
import java.util.List;
import java.util.Optional;

public interface TaskService {
    Task createTask(Task task) throws ValidationException, DatabaseException, UnauthorizedException;
    void updateTask(Task task) throws ValidationException, DatabaseException, UnauthorizedException;
    void updateTaskStatus(int taskId, TaskStatus status) throws ValidationException, DatabaseException, UnauthorizedException;
    void deleteTask(int taskId) throws DatabaseException, UnauthorizedException;
    Optional<Task> getTaskById(int id) throws DatabaseException;
    List<Task> getTasksByProject(int projectId) throws DatabaseException;
    List<Task> getTasksByEmployee(int employeeId) throws DatabaseException;
    List<Task> getAllTasks() throws DatabaseException;
}

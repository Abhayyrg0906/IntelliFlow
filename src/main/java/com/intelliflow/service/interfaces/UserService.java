package com.intelliflow.service.interfaces;

import com.intelliflow.exception.AuthenticationException;
import com.intelliflow.exception.DatabaseException;
import com.intelliflow.exception.ValidationException;
import com.intelliflow.model.ActivityLog;
import com.intelliflow.model.User;
import java.util.List;

public interface UserService {
    User authenticate(String username, String password) throws AuthenticationException, DatabaseException;
    User register(User user, String plainTextPassword) throws ValidationException, DatabaseException;
    List<User> getAllUsers() throws DatabaseException;
    List<User> searchUsers(String query, com.intelliflow.enums.Role roleFilter, Boolean activeStatus) throws DatabaseException;
    void deleteUser(int id) throws DatabaseException, ValidationException;
    void updateUser(User user) throws DatabaseException, ValidationException;
    void setUserActiveStatus(int userId, boolean active) throws DatabaseException, ValidationException;
    void changePassword(int userId, String currentPassword, String newPassword, String confirmPassword) throws AuthenticationException, ValidationException, DatabaseException;
    void bootstrapDefaultUsers() throws DatabaseException;
    List<ActivityLog> getActivityLogs() throws DatabaseException;
    List<ActivityLog> getActivityLogsForUser(User user) throws DatabaseException;
}

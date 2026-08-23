package com.intelliflow.service.impl;

import com.intelliflow.dao.impl.ActivityLogDAOImpl;
import com.intelliflow.dao.impl.UserDAOImpl;
import com.intelliflow.dao.interfaces.ActivityLogDAO;
import com.intelliflow.dao.interfaces.UserDAO;
import com.intelliflow.enums.Role;
import com.intelliflow.exception.AuthenticationException;
import com.intelliflow.exception.DatabaseException;
import com.intelliflow.exception.ValidationException;
import com.intelliflow.model.ActivityLog;
import com.intelliflow.model.User;
import com.intelliflow.service.interfaces.UserService;
import com.intelliflow.util.PasswordUtil;
import com.intelliflow.util.ValidationUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class UserServiceImpl implements UserService {

    private final UserDAO userDAO;
    private final ActivityLogDAO logDAO;

    public UserServiceImpl() {
        this.userDAO = new UserDAOImpl();
        this.logDAO = new ActivityLogDAOImpl();
    }

    public UserServiceImpl(UserDAO userDAO, ActivityLogDAO logDAO) {
        this.userDAO = userDAO;
        this.logDAO = logDAO;
    }

    @Override
    public User authenticate(String username, String password) throws AuthenticationException, DatabaseException {
        if (!ValidationUtil.isNotEmpty(username) || !ValidationUtil.isNotEmpty(password)) {
            throw new AuthenticationException("Username and password are required.");
        }

        Optional<User> optUser = userDAO.findByUsername(username);
        if (optUser.isEmpty()) {
            throw new AuthenticationException("Invalid username or password.");
        }

        User user = optUser.get();
        if (!PasswordUtil.verify(password, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid username or password.");
        }

        // Log successful login
        ActivityLog audit = new ActivityLog();
        audit.setUserId(user.getId());
        audit.setAction("USER_LOGIN");
        audit.setDescription("User logged in: " + user.getUsername());
        logDAO.create(audit);

        return user;
    }

    @Override
    public User register(User user, String plainTextPassword) throws ValidationException, DatabaseException {
        // Validation Checks
        if (user == null) {
            throw new ValidationException("User information cannot be null.");
        }
        if (!ValidationUtil.isNotEmpty(user.getUsername())) {
            throw new ValidationException("Username is required.");
        }
        if (!ValidationUtil.isNotEmpty(user.getEmail())) {
            throw new ValidationException("Email is required.");
        }
        if (!ValidationUtil.isValidEmail(user.getEmail())) {
            throw new ValidationException("Invalid email format.");
        }
        if (!ValidationUtil.isNotEmpty(user.getFullName())) {
            throw new ValidationException("Full name is required.");
        }
        if (!ValidationUtil.isNotEmpty(plainTextPassword)) {
            throw new ValidationException("Password is required.");
        }
        if (!ValidationUtil.isValidPassword(plainTextPassword)) {
            throw new ValidationException("Password must contain at least 8 characters, including 1 uppercase, 1 lowercase, 1 number, and 1 special symbol.");
        }

        // Duplicate Check
        if (userDAO.findByUsername(user.getUsername()).isPresent()) {
            throw new ValidationException("Username '" + user.getUsername() + "' is already taken.");
        }
        if (userDAO.findByEmail(user.getEmail()).isPresent()) {
            throw new ValidationException("Email '" + user.getEmail() + "' is already registered.");
        }

        // Hash Password
        String hash = PasswordUtil.hash(plainTextPassword);
        user.setPasswordHash(hash);
        user.setCreatedAt(LocalDateTime.now());

        User createdUser = userDAO.create(user);

        // Audit Log
        ActivityLog audit = new ActivityLog();
        audit.setUserId(createdUser.getId());
        audit.setAction("USER_REGISTER");
        audit.setDescription("New user registered: " + createdUser.getUsername() + " (" + createdUser.getRole() + ")");
        logDAO.create(audit);

        return createdUser;
    }

    @Override
    public List<User> getAllUsers() throws DatabaseException {
        return userDAO.findAll();
    }

    @Override
    public void deleteUser(int id) throws DatabaseException {
        userDAO.delete(id);
    }

    @Override
    public void updateUser(User user) throws DatabaseException, ValidationException {
        if (user == null) {
            throw new ValidationException("User cannot be null.");
        }
        if (!ValidationUtil.isNotEmpty(user.getEmail()) || !ValidationUtil.isValidEmail(user.getEmail())) {
            throw new ValidationException("Invalid email address.");
        }
        if (!ValidationUtil.isNotEmpty(user.getFullName())) {
            throw new ValidationException("Full name cannot be blank.");
        }

        // Duplicate checks for other users
        Optional<User> byEmail = userDAO.findByEmail(user.getEmail());
        if (byEmail.isPresent() && byEmail.get().getId() != user.getId()) {
            throw new ValidationException("Email address is already used by another user.");
        }

        userDAO.update(user);

        ActivityLog audit = new ActivityLog();
        audit.setUserId(user.getId());
        audit.setAction("USER_UPDATE");
        audit.setDescription("Updated profile details for user: " + user.getUsername());
        logDAO.create(audit);
    }

    @Override
    public void bootstrapDefaultUsers() throws DatabaseException {
        List<User> existingUsers = userDAO.findAll();
        if (existingUsers.isEmpty()) {
            // Seed Admin
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@intelliflow.com");
            admin.setPasswordHash(PasswordUtil.hash("Admin123!"));
            admin.setRole(Role.ADMIN);
            admin.setFullName("System Administrator");
            userDAO.create(admin);

            // Seed Manager
            User manager = new User();
            manager.setUsername("manager1");
            manager.setEmail("manager1@intelliflow.com");
            manager.setPasswordHash(PasswordUtil.hash("Manager123!"));
            manager.setRole(Role.MANAGER);
            manager.setFullName("Project Manager One");
            userDAO.create(manager);

            // Seed Employee
            User employee = new User();
            employee.setUsername("employee1");
            employee.setEmail("employee1@intelliflow.com");
            employee.setPasswordHash(PasswordUtil.hash("Employee123!"));
            employee.setRole(Role.EMPLOYEE);
            employee.setFullName("Software Engineer One");
            userDAO.create(employee);

            // System Log
            ActivityLog systemLog = new ActivityLog();
            systemLog.setUserId(null); // System action
            systemLog.setAction("SYSTEM_BOOTSTRAP");
            systemLog.setDescription("Default role-based users seeded successfully.");
            logDAO.create(systemLog);
        }
    }

    @Override
    public List<ActivityLog> getActivityLogs() throws DatabaseException {
        return logDAO.findAll();
    }
}

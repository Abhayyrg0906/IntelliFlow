package com.intelliflow.service.impl;

import com.intelliflow.context.UserSession;
import com.intelliflow.dao.impl.ActivityLogDAOImpl;
import com.intelliflow.dao.impl.ProjectDAOImpl;
import com.intelliflow.dao.impl.TaskDAOImpl;
import com.intelliflow.dao.impl.UserDAOImpl;
import com.intelliflow.dao.interfaces.ActivityLogDAO;
import com.intelliflow.dao.interfaces.ProjectDAO;
import com.intelliflow.dao.interfaces.TaskDAO;
import com.intelliflow.dao.interfaces.UserDAO;
import com.intelliflow.enums.Role;
import com.intelliflow.exception.AuthenticationException;
import com.intelliflow.exception.DatabaseException;
import com.intelliflow.exception.UnauthorizedException;
import com.intelliflow.exception.ValidationException;
import com.intelliflow.model.ActivityLog;
import com.intelliflow.model.Project;
import com.intelliflow.model.Task;
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
    private final ProjectDAO projectDAO;
    private final TaskDAO taskDAO;

    public UserServiceImpl() {
        this.userDAO = new UserDAOImpl();
        this.logDAO = new ActivityLogDAOImpl();
        this.projectDAO = new ProjectDAOImpl();
        this.taskDAO = new TaskDAOImpl();
    }

    public UserServiceImpl(UserDAO userDAO, ActivityLogDAO logDAO) {
        this.userDAO = userDAO;
        this.logDAO = logDAO;
        this.projectDAO = new ProjectDAOImpl();
        this.taskDAO = new TaskDAOImpl();
    }

    public UserServiceImpl(UserDAO userDAO, ActivityLogDAO logDAO, ProjectDAO projectDAO, TaskDAO taskDAO) {
        this.userDAO = userDAO;
        this.logDAO = logDAO;
        this.projectDAO = projectDAO;
        this.taskDAO = taskDAO;
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
        // Authorization Enforce
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null) {
            if (currentUser.getRole() != Role.ADMIN) {
                throw new UnauthorizedException("Access Denied: Only Administrators can register new accounts inside the platform.");
            }
        } else {
            if (user != null && user.getRole() == Role.ADMIN) {
                if (!userDAO.findAll().isEmpty()) {
                    throw new UnauthorizedException("Access Denied: Self-registration as an Administrator is not allowed.");
                }
            }
        }

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

        // Uniqueness Checks
        if (userDAO.findByUsername(user.getUsername()).isPresent()) {
            throw new ValidationException("Username is already taken.");
        }
        if (userDAO.findByEmail(user.getEmail()).isPresent()) {
            throw new ValidationException("Email is already registered.");
        }

        // Hash password securely (no plaintext passwords saved or logged)
        user.setPasswordHash(PasswordUtil.hash(plainTextPassword));
        user.setCreatedAt(LocalDateTime.now());

        User createdUser = userDAO.create(user);

        // Audit Trail (No passwords or tokens exposed)
        String actor = currentUser != null ? currentUser.getFullName() : createdUser.getFullName();
        ActivityLog audit = new ActivityLog();
        audit.setUserId(currentUser != null ? currentUser.getId() : createdUser.getId());
        audit.setAction("USER_CREATE");
        audit.setDescription(actor + " created user " + createdUser.getUsername() + " (" + createdUser.getRole() + ")");
        logDAO.create(audit);

        return createdUser;
    }

    @Override
    public List<User> getAllUsers() throws DatabaseException {
        return userDAO.findAll();
    }

    @Override
    public void deleteUser(int id) throws DatabaseException {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Access Denied: Only Administrators can delete user accounts.");
        }
        userDAO.delete(id);

        ActivityLog audit = new ActivityLog();
        audit.setUserId(currentUser.getId());
        audit.setAction("USER_DELETE");
        audit.setDescription(currentUser.getFullName() + " deleted user account (ID: " + id + ")");
        logDAO.create(audit);
    }

    @Override
    public void updateUser(User user) throws DatabaseException, ValidationException {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Access Denied: Session is not active.");
        }
        if (currentUser.getRole() != Role.ADMIN && currentUser.getId() != user.getId()) {
            throw new UnauthorizedException("Access Denied: You can only update your own profile details.");
        }

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

        Optional<User> originalOpt = userDAO.findById(user.getId());
        User original = originalOpt.orElse(null);

        userDAO.update(user);

        if (original != null && original.getRole() != user.getRole()) {
            ActivityLog roleLog = new ActivityLog();
            roleLog.setUserId(currentUser.getId());
            roleLog.setAction("USER_ROLE_CHANGE");
            roleLog.setDescription("User " + user.getUsername() + " role changed from " + original.getRole() + " to " + user.getRole());
            logDAO.create(roleLog);
        } else {
            ActivityLog audit = new ActivityLog();
            audit.setUserId(currentUser.getId());
            audit.setAction("USER_UPDATE");
            audit.setDescription("Updated profile details for user: " + user.getUsername());
            logDAO.create(audit);
        }
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
        User currentUser = UserSession.getInstance().getCurrentUser();
        return getActivityLogsForUser(currentUser);
    }

    @Override
    public List<ActivityLog> getActivityLogsForUser(User user) throws DatabaseException {
        if (user == null) {
            throw new UnauthorizedException("Access Denied: Session is not active.");
        }
        List<ActivityLog> allLogs = logDAO.findAll();
        if (user.getRole() == Role.ADMIN) {
            return allLogs; // Full system audit visibility for Administrators
        }

        List<Project> allProjects = projectDAO != null ? projectDAO.findAll() : List.of();
        List<Task> allTasks = taskDAO != null ? taskDAO.findAll() : List.of();

        if (user.getRole() == Role.MANAGER) {
            List<Project> managed = allProjects.stream()
                    .filter(p -> p.getManagerId() != null && p.getManagerId() == user.getId())
                    .collect(java.util.stream.Collectors.toList());
            java.util.Set<String> managedProjectNames = managed.stream().map(Project::getName).collect(java.util.stream.Collectors.toSet());
            java.util.Set<Integer> managedProjectIds = managed.stream().map(Project::getId).collect(java.util.stream.Collectors.toSet());
            java.util.Set<String> managedTaskNames = allTasks.stream()
                    .filter(t -> managedProjectIds.contains(t.getProjectId()))
                    .map(Task::getName)
                    .collect(java.util.stream.Collectors.toSet());

            return allLogs.stream().filter(l -> {
                if (l.getUserId() != null && l.getUserId() == user.getId()) return true;
                String desc = l.getDescription() != null ? l.getDescription() : "";
                for (String pName : managedProjectNames) {
                    if (desc.contains(pName)) return true;
                }
                for (String tName : managedTaskNames) {
                    if (desc.contains(tName)) return true;
                }
                return false;
            }).collect(java.util.stream.Collectors.toList());
        } else {
            // Role.EMPLOYEE
            List<Task> assignedTasks = allTasks.stream()
                    .filter(t -> t.getAssignedEmployeeId() != null && t.getAssignedEmployeeId() == user.getId())
                    .collect(java.util.stream.Collectors.toList());
            java.util.Set<String> assignedTaskNames = assignedTasks.stream().map(Task::getName).collect(java.util.stream.Collectors.toSet());
            java.util.Set<Integer> assignedProjectIds = assignedTasks.stream().map(Task::getProjectId).collect(java.util.stream.Collectors.toSet());
            java.util.Set<String> assignedProjectNames = allProjects.stream()
                    .filter(p -> assignedProjectIds.contains(p.getId()))
                    .map(Project::getName)
                    .collect(java.util.stream.Collectors.toSet());

            return allLogs.stream().filter(l -> {
                if (l.getUserId() != null && l.getUserId() == user.getId()) return true;
                String desc = l.getDescription() != null ? l.getDescription() : "";
                for (String tName : assignedTaskNames) {
                    if (desc.contains(tName)) return true;
                }
                for (String pName : assignedProjectNames) {
                    if (desc.contains(pName)) return true;
                }
                return false;
            }).collect(java.util.stream.Collectors.toList());
        }
    }
}

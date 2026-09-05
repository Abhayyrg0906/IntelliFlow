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
        if (!user.isActive()) {
            throw new AuthenticationException("Account has been deactivated. Please contact an administrator.");
        }
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
        if (!ValidationUtil.isValidUsername(user.getUsername())) {
            throw new ValidationException("Username must be 3-50 alphanumeric characters (or . - _).");
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
    public List<User> searchUsers(String query, Role roleFilter, Boolean activeStatus) throws DatabaseException {
        List<User> all = userDAO.findAll();
        String q = query != null ? query.trim().toLowerCase() : "";

        return all.stream()
                .filter(u -> {
                    if (!q.isEmpty()) {
                        boolean matchesUsername = u.getUsername() != null && u.getUsername().toLowerCase().contains(q);
                        boolean matchesEmail = u.getEmail() != null && u.getEmail().toLowerCase().contains(q);
                        boolean matchesName = u.getFullName() != null && u.getFullName().toLowerCase().contains(q);
                        if (!matchesUsername && !matchesEmail && !matchesName) {
                            return false;
                        }
                    }
                    if (roleFilter != null && u.getRole() != roleFilter) {
                        return false;
                    }
                    if (activeStatus != null && u.isActive() != activeStatus) {
                        return false;
                    }
                    return true;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public void deleteUser(int id) throws DatabaseException, ValidationException {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Access Denied: Only Administrators can delete user accounts.");
        }
        if (currentUser.getId() == id) {
            throw new ValidationException("You cannot delete your own active administrator account.");
        }
        userDAO.delete(id);

        ActivityLog audit = new ActivityLog();
        audit.setUserId(currentUser.getId());
        audit.setAction("USER_DELETE");
        audit.setDescription(currentUser.getFullName() + " deleted user account (ID: " + id + ")");
        logDAO.create(audit);
    }

    @Override
    public void setUserActiveStatus(int userId, boolean active) throws DatabaseException, ValidationException {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Access Denied: Only Administrators can activate or deactivate accounts.");
        }
        if (currentUser.getId() == userId && !active) {
            throw new ValidationException("You cannot deactivate your own active administrator account.");
        }
        Optional<User> optUser = userDAO.findById(userId);
        if (optUser.isEmpty()) {
            throw new ValidationException("User account not found with ID: " + userId);
        }
        User user = optUser.get();
        if (user.isActive() == active) {
            return;
        }
        user.setActive(active);
        userDAO.update(user);

        ActivityLog statusLog = new ActivityLog();
        statusLog.setUserId(currentUser.getId());
        statusLog.setAction(active ? "USER_ACTIVATED" : "USER_DEACTIVATED");
        statusLog.setDescription("Admin " + currentUser.getFullName() + " " + (active ? "activated" : "deactivated") + " account for " + user.getUsername());
        logDAO.create(statusLog);
    }

    @Override
    public void updateUser(User user) throws DatabaseException, ValidationException {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Access Denied: Session is not active.");
        }
        if (user == null) {
            throw new ValidationException("User cannot be null.");
        }
        Optional<User> originalOpt = userDAO.findById(user.getId());
        if (originalOpt.isEmpty()) {
            throw new ValidationException("User account not found with ID: " + user.getId());
        }
        User original = originalOpt.get();

        if (currentUser.getRole() != Role.ADMIN) {
            if (currentUser.getId() != user.getId()) {
                throw new UnauthorizedException("Access Denied: You can only update your own profile details.");
            }
            if (user.getRole() != original.getRole()) {
                throw new UnauthorizedException("Access Denied: Only Administrators can change account roles.");
            }
            if (user.isActive() != original.isActive()) {
                throw new UnauthorizedException("Access Denied: Only Administrators can alter account status.");
            }
        } else {
            // Admin protections against self-lockout
            if (currentUser.getId() == user.getId()) {
                if (user.getRole() != Role.ADMIN) {
                    throw new ValidationException("You cannot remove Administrator role from your own active account.");
                }
                if (!user.isActive()) {
                    throw new ValidationException("You cannot deactivate your own active administrator account.");
                }
            }
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

        // Preserve password hash and created date if not set
        if (user.getPasswordHash() == null || user.getPasswordHash().isEmpty()) {
            user.setPasswordHash(original.getPasswordHash());
        }
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(original.getCreatedAt());
        }

        userDAO.update(user);

        if (original.getRole() != user.getRole()) {
            ActivityLog roleLog = new ActivityLog();
            roleLog.setUserId(currentUser.getId());
            roleLog.setAction("USER_ROLE_CHANGE");
            roleLog.setDescription("User " + user.getUsername() + " role changed from " + original.getRole() + " to " + user.getRole());
            logDAO.create(roleLog);
        }
        if (original.isActive() != user.isActive()) {
            ActivityLog statusLog = new ActivityLog();
            statusLog.setUserId(currentUser.getId());
            statusLog.setAction(user.isActive() ? "USER_ACTIVATED" : "USER_DEACTIVATED");
            statusLog.setDescription("Admin " + currentUser.getFullName() + " " + (user.isActive() ? "activated" : "deactivated") + " account for " + user.getUsername());
            logDAO.create(statusLog);
        }
        if (original.getRole() == user.getRole() && original.isActive() == user.isActive()) {
            ActivityLog audit = new ActivityLog();
            audit.setUserId(currentUser.getId());
            audit.setAction("USER_UPDATE");
            audit.setDescription("Updated profile details for user: " + user.getUsername());
            logDAO.create(audit);
        }
    }

    @Override
    public void changePassword(int userId, String currentPassword, String newPassword, String confirmPassword)
            throws AuthenticationException, ValidationException, DatabaseException {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Access Denied: Session is not active.");
        }
        if (currentUser.getRole() != Role.ADMIN && currentUser.getId() != userId) {
            throw new UnauthorizedException("Access Denied: You can only change password for your own account.");
        }

        Optional<User> optUser = userDAO.findById(userId);
        if (optUser.isEmpty()) {
            throw new ValidationException("User account not found with ID: " + userId);
        }
        User targetUser = optUser.get();

        if (!ValidationUtil.isNotEmpty(currentPassword)) {
            throw new ValidationException("Current password is required.");
        }
        if (!PasswordUtil.verify(currentPassword, targetUser.getPasswordHash())) {
            throw new AuthenticationException("Current password is incorrect.");
        }

        if (!ValidationUtil.isNotEmpty(newPassword)) {
            throw new ValidationException("New password is required.");
        }
        if (!ValidationUtil.isNotEmpty(confirmPassword)) {
            throw new ValidationException("Password confirmation is required.");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new ValidationException("New password and confirmation do not match.");
        }
        if (!ValidationUtil.isValidPassword(newPassword)) {
            throw new ValidationException("Password must contain at least 8 characters, including 1 uppercase, 1 lowercase, 1 number, and 1 special symbol.");
        }
        if (PasswordUtil.verify(newPassword, targetUser.getPasswordHash())) {
            throw new ValidationException("New password cannot be the same as the current password.");
        }

        targetUser.setPasswordHash(PasswordUtil.hash(newPassword));
        userDAO.update(targetUser);

        // Update active session user if changing self
        if (currentUser.getId() == targetUser.getId()) {
            currentUser.setPasswordHash(targetUser.getPasswordHash());
        }

        ActivityLog audit = new ActivityLog();
        audit.setUserId(currentUser.getId());
        audit.setAction("USER_PASSWORD_CHANGE");
        audit.setDescription("Password changed for user: " + targetUser.getUsername());
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

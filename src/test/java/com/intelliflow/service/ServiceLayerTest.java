package com.intelliflow.service;

import com.intelliflow.context.UserSession;
import com.intelliflow.dao.interfaces.*;
import com.intelliflow.enums.*;
import com.intelliflow.exception.*;
import com.intelliflow.model.*;
import com.intelliflow.service.impl.*;
import com.intelliflow.service.interfaces.*;
import com.intelliflow.util.PasswordUtil;
import com.intelliflow.util.ValidationUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ServiceLayerTest {

    private UserService userService;
    private ProjectService projectService;
    private TaskService taskService;

    // In-Memory DAO Stubs for fast database-independent testing
    private static class InMemoryUserDAO implements UserDAO {
        private final Map<Integer, User> users = new HashMap<>();
        private int idSequence = 1;

        @Override
        public Optional<User> findById(int id) {
            return Optional.ofNullable(users.get(id));
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return users.values().stream()
                    .filter(u -> u.getUsername().equalsIgnoreCase(username))
                    .findFirst();
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return users.values().stream()
                    .filter(u -> u.getEmail().equalsIgnoreCase(email))
                    .findFirst();
        }

        @Override
        public List<User> findAll() {
            return new ArrayList<>(users.values());
        }

        @Override
        public User create(User user) {
            user.setId(idSequence++);
            users.put(user.getId(), user);
            return user;
        }

        @Override
        public void update(User user) {
            users.put(user.getId(), user);
        }

        @Override
        public void delete(int id) {
            users.remove(id);
        }
    }

    private static class InMemoryProjectDAO implements ProjectDAO {
        private final Map<Integer, Project> projects = new HashMap<>();
        private int idSequence = 1;

        @Override
        public Optional<Project> findById(int id) {
            return Optional.ofNullable(projects.get(id));
        }

        @Override
        public List<Project> findAll() {
            return new ArrayList<>(projects.values());
        }

        @Override
        public List<Project> findByManagerId(int managerId) {
            List<Project> result = new ArrayList<>();
            for (Project p : projects.values()) {
                if (p.getManagerId() != null && p.getManagerId() == managerId) {
                    result.add(p);
                }
            }
            return result;
        }

        @Override
        public Project create(Project project) {
            project.setId(idSequence++);
            projects.put(project.getId(), project);
            return project;
        }

        @Override
        public void update(Project project) {
            projects.put(project.getId(), project);
        }

        @Override
        public void delete(int id) {
            projects.remove(id);
        }
    }

    private static class InMemoryTaskDAO implements TaskDAO {
        private final Map<Integer, Task> tasks = new HashMap<>();
        private int idSequence = 1;

        @Override
        public Optional<Task> findById(int id) {
            return Optional.ofNullable(tasks.get(id));
        }

        @Override
        public List<Task> findByProjectId(int projectId) {
            List<Task> result = new ArrayList<>();
            for (Task t : tasks.values()) {
                if (t.getProjectId() == projectId) {
                    result.add(t);
                }
            }
            return result;
        }

        @Override
        public List<Task> findByEmployeeId(int employeeId) {
            List<Task> result = new ArrayList<>();
            for (Task t : tasks.values()) {
                if (t.getAssignedEmployeeId() != null && t.getAssignedEmployeeId() == employeeId) {
                    result.add(t);
                }
            }
            return result;
        }

        @Override
        public List<Task> findAll() {
            return new ArrayList<>(tasks.values());
        }

        @Override
        public Task create(Task task) {
            task.setId(idSequence++);
            tasks.put(task.getId(), task);
            return task;
        }

        @Override
        public void update(Task task) {
            tasks.put(task.getId(), task);
        }

        @Override
        public void delete(int id) {
            tasks.remove(id);
        }
    }

    private static class InMemoryNotificationDAO implements NotificationDAO {
        private final List<Notification> notifications = new ArrayList<>();
        private int idSequence = 1;

        @Override
        public Optional<Notification> findById(int id) {
            return notifications.stream().filter(n -> n.getId() == id).findFirst();
        }

        @Override
        public List<Notification> findByUserId(int userId) {
            List<Notification> res = new ArrayList<>();
            for (Notification n : notifications) {
                if (n.getUserId() == userId) res.add(n);
            }
            return res;
        }

        @Override
        public List<Notification> findUnreadByUserId(int userId) {
            List<Notification> res = new ArrayList<>();
            for (Notification n : notifications) {
                if (n.getUserId() == userId && !n.isRead()) res.add(n);
            }
            return res;
        }

        @Override
        public Notification create(Notification notification) {
            notification.setId(idSequence++);
            notifications.add(notification);
            return notification;
        }

        @Override
        public void markAsRead(int id) {
            findById(id).ifPresent(n -> n.setRead(true));
        }

        @Override
        public void markAllAsRead(int userId) {
            findUnreadByUserId(userId).forEach(n -> n.setRead(true));
        }

        @Override
        public void delete(int id) {
            notifications.removeIf(n -> n.getId() == id);
        }
    }

    private static class InMemoryActivityLogDAO implements ActivityLogDAO {
        private final List<ActivityLog> logs = new ArrayList<>();
        private int idSequence = 1;

        @Override
        public List<ActivityLog> findAll() {
            return logs;
        }

        @Override
        public List<ActivityLog> findByUserId(int userId) {
            List<ActivityLog> res = new ArrayList<>();
            for (ActivityLog l : logs) {
                if (l.getUserId() != null && l.getUserId() == userId) res.add(l);
            }
            return res;
        }

        @Override
        public ActivityLog create(ActivityLog log) {
            log.setId(idSequence++);
            logs.add(log);
            return log;
        }
    }

    @BeforeEach
    public void setUp() {
        UserSession.getInstance().cleanSession();
        
        UserDAO userDAO = new InMemoryUserDAO();
        ProjectDAO projectDAO = new InMemoryProjectDAO();
        TaskDAO taskDAO = new InMemoryTaskDAO();
        NotificationDAO notificationDAO = new InMemoryNotificationDAO();
        ActivityLogDAO logDAO = new InMemoryActivityLogDAO();

        userService = new UserServiceImpl(userDAO, logDAO);
        projectService = new ProjectServiceImpl(projectDAO, logDAO);
        taskService = new TaskServiceImpl(taskDAO, projectDAO, userDAO, notificationDAO, logDAO);
    }

    // 1. Password Strength Validation Tests
    @Test
    public void testPasswordValidator() {
        // Valid password: at least 8 chars, 1 uppercase, 1 lowercase, 1 number, 1 special char
        assertTrue(ValidationUtil.isValidPassword("Admin123!"));
        assertTrue(ValidationUtil.isValidPassword("P@ssw0rd99"));
        assertTrue(ValidationUtil.isValidPassword("Strong#123"));

        // Invalid passwords
        assertFalse(ValidationUtil.isValidPassword("short"));          // Too short
        assertFalse(ValidationUtil.isValidPassword("Nodigits!"));       // No numbers
        assertFalse(ValidationUtil.isValidPassword("nouppercase1!"));  // No uppercase
        assertFalse(ValidationUtil.isValidPassword("NOLOWERCASE1!"));  // No lowercase
        assertFalse(ValidationUtil.isValidPassword("NoSpecial123"));    // No special character
    }

    // 2. Email Validation Tests
    @Test
    public void testEmailValidator() {
        assertTrue(ValidationUtil.isValidEmail("user@example.com"));
        assertTrue(ValidationUtil.isValidEmail("john.doe@company.co.uk"));

        assertFalse(ValidationUtil.isValidEmail("invalid-email"));
        assertFalse(ValidationUtil.isValidEmail("user@"));
        assertFalse(ValidationUtil.isValidEmail("@example.com"));
    }

    // 3. Authentication Tests
    @Test
    public void testAuthentication() throws Exception {
        User user = new User();
        user.setUsername("developer");
        user.setEmail("dev@intelliflow.com");
        user.setRole(Role.EMPLOYEE);
        user.setFullName("John Developer");
        
        userService.register(user, "Password123!");

        // Test login success
        User loggedIn = userService.authenticate("developer", "Password123!");
        assertNotNull(loggedIn);
        assertEquals("developer", loggedIn.getUsername());

        // Test login failure
        assertThrows(AuthenticationException.class, () -> {
            userService.authenticate("developer", "WrongPassword!");
        });
        assertThrows(AuthenticationException.class, () -> {
            userService.authenticate("nonexistent", "Password123!");
        });
    }

    // 4. Role Permissions and Project Creation Tests
    @Test
    public void testProjectPermissions() throws Exception {
        // Create an employee user
        User employee = new User();
        employee.setUsername("emp");
        employee.setEmail("emp@intelliflow.com");
        employee.setRole(Role.EMPLOYEE);
        employee.setFullName("Employee User");
        User empRegistered = userService.register(employee, "Password123!");

        // Start session as employee
        UserSession.getInstance().startSession(empRegistered);

        Project project = new Project();
        project.setName("Mobile App");
        project.setStartDate(LocalDate.now());
        project.setDeadline(LocalDate.now().plusDays(10));
        project.setStatus(ProjectStatus.PLANNED);

        // Employee should NOT be allowed to create project
        assertThrows(UnauthorizedException.class, () -> {
            projectService.createProject(project);
        });

        // Now create a manager user
        User manager = new User();
        manager.setUsername("manager");
        manager.setEmail("mgr@intelliflow.com");
        manager.setRole(Role.MANAGER);
        manager.setFullName("Manager User");
        User mgrRegistered = userService.register(manager, "Password123!");

        // Start session as manager
        UserSession.getInstance().startSession(mgrRegistered);

        // Manager should succeed
        Project created = projectService.createProject(project);
        assertNotNull(created);
        assertTrue(created.getId() > 0);
    }

    // 5. Task Creation & Deadline Validation Tests
    @Test
    public void testTaskDeadlineValidation() throws Exception {
        // Set session to Manager
        User manager = new User();
        manager.setUsername("manager");
        manager.setRole(Role.MANAGER);
        UserSession.getInstance().startSession(manager);

        // Create project
        Project project = new Project();
        project.setName("Website Revamp");
        project.setStartDate(LocalDate.of(2026, 9, 1));
        project.setDeadline(LocalDate.of(2026, 9, 30));
        project.setStatus(ProjectStatus.ACTIVE);
        Project savedProj = projectService.createProject(project);

        // 1. Task deadline exceeds project deadline
        Task badTask = new Task();
        badTask.setProjectId(savedProj.getId());
        badTask.setName("QA Tests");
        badTask.setDeadline(LocalDate.of(2026, 10, 5)); // Over project's Sep 30 deadline
        badTask.setPriority(TaskPriority.HIGH);
        badTask.setStatus(TaskStatus.TO_DO);

        assertThrows(ValidationException.class, () -> {
            taskService.createTask(badTask);
        });

        // 2. Task deadline before project start date
        Task badTask2 = new Task();
        badTask2.setProjectId(savedProj.getId());
        badTask2.setName("Requirements Doc");
        badTask2.setDeadline(LocalDate.of(2026, 8, 25)); // Before project's Sep 1 start
        badTask2.setPriority(TaskPriority.LOW);
        badTask2.setStatus(TaskStatus.TO_DO);

        assertThrows(ValidationException.class, () -> {
            taskService.createTask(badTask2);
        });

        // 3. Valid task deadline
        Task goodTask = new Task();
        goodTask.setProjectId(savedProj.getId());
        goodTask.setName("Implementation Phase 1");
        goodTask.setDeadline(LocalDate.of(2026, 9, 20)); // Within range
        goodTask.setPriority(TaskPriority.CRITICAL);
        goodTask.setStatus(TaskStatus.TO_DO);

        Task savedTask = taskService.createTask(goodTask);
        assertNotNull(savedTask);
    }

    // 6. Task Status Transition Rules Tests
    @Test
    public void testTaskStatusTransitions() throws Exception {
        // Create and register users properly
        User manager = new User();
        manager.setUsername("manager");
        manager.setEmail("manager@intelliflow.com");
        manager.setRole(Role.MANAGER);
        manager.setFullName("Test Manager");
        User savedManager = userService.register(manager, "Password123!");

        User employee = new User();
        employee.setUsername("employee");
        employee.setEmail("employee@intelliflow.com");
        employee.setRole(Role.EMPLOYEE);
        employee.setFullName("Test Employee");
        User savedEmployee = userService.register(employee, "Password123!");

        // Start session as manager to create project and task
        UserSession.getInstance().startSession(savedManager);

        // Project
        Project project = new Project();
        project.setName("API Dev");
        project.setStartDate(LocalDate.now());
        project.setDeadline(LocalDate.now().plusDays(30));
        project.setStatus(ProjectStatus.ACTIVE);
        Project savedProj = projectService.createProject(project);

        // Create Task assigned to employee
        Task task = new Task();
        task.setProjectId(savedProj.getId());
        task.setName("Write Controller Unit Tests");
        task.setAssignedEmployeeId(savedEmployee.getId()); // Assigned to registered employee
        task.setDeadline(LocalDate.now().plusDays(5));
        task.setPriority(TaskPriority.MEDIUM);
        task.setStatus(TaskStatus.TO_DO);
        Task savedTask = taskService.createTask(task);

        // Switch session to employee for status transitions
        UserSession.getInstance().startSession(savedEmployee);

        // Verify valid transitions: TO_DO -> IN_PROGRESS -> TESTING -> COMPLETED
        
        // 1. TO_DO -> IN_PROGRESS: Should succeed
        taskService.updateTaskStatus(savedTask.getId(), TaskStatus.IN_PROGRESS);
        assertEquals(TaskStatus.IN_PROGRESS, taskService.getTaskById(savedTask.getId()).get().getStatus());

        // 2. IN_PROGRESS -> COMPLETED: Should fail (must go through TESTING first)
        assertThrows(ValidationException.class, () -> {
            taskService.updateTaskStatus(savedTask.getId(), TaskStatus.COMPLETED);
        });

        // 3. IN_PROGRESS -> TESTING: Should succeed
        taskService.updateTaskStatus(savedTask.getId(), TaskStatus.TESTING);
        assertEquals(TaskStatus.TESTING, taskService.getTaskById(savedTask.getId()).get().getStatus());

        // 4. TESTING -> COMPLETED: Should succeed
        taskService.updateTaskStatus(savedTask.getId(), TaskStatus.COMPLETED);
        assertEquals(TaskStatus.COMPLETED, taskService.getTaskById(savedTask.getId()).get().getStatus());

        // 5. Reopening completed tasks as Employee should fail
        assertThrows(ValidationException.class, () -> {
            taskService.updateTaskStatus(savedTask.getId(), TaskStatus.IN_PROGRESS);
        });

        // Reopening completed tasks as Manager should succeed
        manager = new User();
        manager.setRole(Role.MANAGER);
        UserSession.getInstance().startSession(manager);
        taskService.updateTaskStatus(savedTask.getId(), TaskStatus.IN_PROGRESS);
        assertEquals(TaskStatus.IN_PROGRESS, taskService.getTaskById(savedTask.getId()).get().getStatus());
    }

    // 7. Task Assignment Role Validation Tests
    @Test
    public void testTaskAssignmentRoleValidation() throws Exception {
        // Manager Session
        User manager = new User();
        manager.setRole(Role.MANAGER);
        UserSession.getInstance().startSession(manager);

        // Project
        Project project = new Project();
        project.setName("DB Setup");
        project.setStartDate(LocalDate.now());
        project.setDeadline(LocalDate.now().plusDays(10));
        project.setStatus(ProjectStatus.ACTIVE);
        Project savedProj = projectService.createProject(project);

        // Register a Manager user to try and assign a task to them (which is invalid)
        User anotherManager = new User();
        anotherManager.setUsername("manager2");
        anotherManager.setEmail("mgr2@intelliflow.com");
        anotherManager.setRole(Role.MANAGER); // Role is MANAGER, not EMPLOYEE
        anotherManager.setFullName("Second Manager");
        User savedMgr2 = userService.register(anotherManager, "Password123!");

        Task task = new Task();
        task.setProjectId(savedProj.getId());
        task.setName("Configure Replica DB");
        task.setDeadline(LocalDate.now().plusDays(5));
        task.setPriority(TaskPriority.MEDIUM);
        task.setStatus(TaskStatus.TO_DO);
        task.setAssignedEmployeeId(savedMgr2.getId()); // Trying to assign task to a Manager

        // Task assignment should fail since manager2 is not an employee
        assertThrows(ValidationException.class, () -> {
            taskService.createTask(task);
        });
    }

    // 8. Admin, Manager, and Employee role-based authentication and authorization tests
    @Test
    public void testAdminManagerEmployeePermissions() throws Exception {
        // Register an Admin
        User admin = new User();
        admin.setUsername("testadmin");
        admin.setEmail("admin@test.com");
        admin.setRole(Role.ADMIN);
        admin.setFullName("Test Admin");
        User registeredAdmin = userService.register(admin, "AdminPass123!");
        assertNotNull(registeredAdmin);
        assertEquals(Role.ADMIN, registeredAdmin.getRole());

        // Authenticate Admin
        User loggedAdmin = userService.authenticate("testadmin", "AdminPass123!");
        assertNotNull(loggedAdmin);
        assertEquals(Role.ADMIN, loggedAdmin.getRole());

        // Start session as Admin
        UserSession.getInstance().startSession(loggedAdmin);

        // Admin should be able to create a project
        Project project = new Project();
        project.setName("Admin Project");
        project.setStartDate(LocalDate.now());
        project.setDeadline(LocalDate.now().plusDays(10));
        project.setStatus(ProjectStatus.PLANNED);
        Project adminProject = projectService.createProject(project);
        assertNotNull(adminProject);

        // Admin should be able to create a task
        Task task = new Task();
        task.setProjectId(adminProject.getId());
        task.setName("Admin Task");
        task.setDeadline(LocalDate.now().plusDays(5));
        task.setPriority(TaskPriority.HIGH);
        task.setStatus(TaskStatus.TO_DO);
        Task adminTask = taskService.createTask(task);
        assertNotNull(adminTask);

        // Register a Manager
        User manager = new User();
        manager.setUsername("testmanager");
        manager.setEmail("manager@test.com");
        manager.setRole(Role.MANAGER);
        manager.setFullName("Test Manager");
        User registeredManager = userService.register(manager, "ManagerPass123!");
        assertNotNull(registeredManager);
        assertEquals(Role.MANAGER, registeredManager.getRole());

        // Authenticate Manager
        User loggedManager = userService.authenticate("testmanager", "ManagerPass123!");
        assertNotNull(loggedManager);

        // Start session as Manager
        UserSession.getInstance().startSession(loggedManager);

        // Manager should be able to create a project
        Project mgrProject = new Project();
        mgrProject.setName("Manager Project");
        mgrProject.setStartDate(LocalDate.now());
        mgrProject.setDeadline(LocalDate.now().plusDays(10));
        mgrProject.setStatus(ProjectStatus.PLANNED);
        Project createdMgrProj = projectService.createProject(mgrProject);
        assertNotNull(createdMgrProj);

        // Register an Employee
        User employee = new User();
        employee.setUsername("testemployee");
        employee.setEmail("employee@test.com");
        employee.setRole(Role.EMPLOYEE);
        employee.setFullName("Test Employee");
        User registeredEmployee = userService.register(employee, "EmployeePass123!");
        assertNotNull(registeredEmployee);

        // Authenticate Employee
        User loggedEmployee = userService.authenticate("testemployee", "EmployeePass123!");
        assertNotNull(loggedEmployee);

        // Start session as Employee
        UserSession.getInstance().startSession(loggedEmployee);

        // Employee should NOT be able to create a project
        Project empProject = new Project();
        empProject.setName("Employee Project");
        empProject.setStartDate(LocalDate.now());
        empProject.setDeadline(LocalDate.now().plusDays(10));
        empProject.setStatus(ProjectStatus.PLANNED);
        assertThrows(UnauthorizedException.class, () -> {
            projectService.createProject(empProject);
        });

        // Employee should NOT be able to create a task
        Task empTask = new Task();
        empTask.setProjectId(adminProject.getId());
        empTask.setName("Employee Task");
        empTask.setDeadline(LocalDate.now().plusDays(5));
        empTask.setPriority(TaskPriority.HIGH);
        empTask.setStatus(TaskStatus.TO_DO);
        assertThrows(UnauthorizedException.class, () -> {
            taskService.createTask(empTask);
        });
    }
}

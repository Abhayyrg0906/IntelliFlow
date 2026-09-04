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
import com.intelliflow.util.WorkloadUtil;

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
    private ReportService reportService;

    // In-Memory DAO Stubs for fast database-independent testing
    private static class InMemoryUserDAO implements UserDAO {
        private final Map<Integer, User> users = new HashMap<>();
        private int idSequence = 1;

        private User copy(User u) {
            if (u == null) return null;
            User c = new User();
            c.setId(u.getId());
            c.setUsername(u.getUsername());
            c.setPasswordHash(u.getPasswordHash());
            c.setFullName(u.getFullName());
            c.setEmail(u.getEmail());
            c.setRole(u.getRole());
            c.setCreatedAt(u.getCreatedAt());
            return c;
        }

        @Override
        public Optional<User> findById(int id) {
            return Optional.ofNullable(copy(users.get(id)));
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return users.values().stream()
                    .filter(u -> u.getUsername().equalsIgnoreCase(username))
                    .map(this::copy)
                    .findFirst();
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return users.values().stream()
                    .filter(u -> u.getEmail().equalsIgnoreCase(email))
                    .map(this::copy)
                    .findFirst();
        }

        @Override
        public List<User> findAll() {
            List<User> list = new ArrayList<>();
            for (User u : users.values()) list.add(copy(u));
            return list;
        }

        @Override
        public User create(User user) {
            user.setId(idSequence++);
            users.put(user.getId(), copy(user));
            return copy(user);
        }

        @Override
        public void update(User user) {
            users.put(user.getId(), copy(user));
        }

        @Override
        public void delete(int id) {
            users.remove(id);
        }
    }

    private static class InMemoryProjectDAO implements ProjectDAO {
        private final Map<Integer, Project> projects = new HashMap<>();
        private int idSequence = 1;

        private Project copy(Project p) {
            if (p == null) return null;
            Project c = new Project();
            c.setId(p.getId());
            c.setName(p.getName());
            c.setDescription(p.getDescription());
            c.setManagerId(p.getManagerId());
            c.setStartDate(p.getStartDate());
            c.setDeadline(p.getDeadline());
            c.setStatus(p.getStatus());
            c.setCreatedAt(p.getCreatedAt());
            return c;
        }

        @Override
        public Optional<Project> findById(int id) {
            return Optional.ofNullable(copy(projects.get(id)));
        }

        @Override
        public List<Project> findAll() {
            List<Project> list = new ArrayList<>();
            for (Project p : projects.values()) list.add(copy(p));
            return list;
        }

        @Override
        public List<Project> findByManagerId(int managerId) {
            List<Project> result = new ArrayList<>();
            for (Project p : projects.values()) {
                if (p.getManagerId() != null && p.getManagerId() == managerId) {
                    result.add(copy(p));
                }
            }
            return result;
        }

        @Override
        public Project create(Project project) {
            project.setId(idSequence++);
            projects.put(project.getId(), copy(project));
            return copy(project);
        }

        @Override
        public void update(Project project) {
            projects.put(project.getId(), copy(project));
        }

        @Override
        public void delete(int id) {
            projects.remove(id);
        }
    }

    private static class InMemoryTaskDAO implements TaskDAO {
        private final Map<Integer, Task> tasks = new HashMap<>();
        private int idSequence = 1;

        private Task copy(Task t) {
            if (t == null) return null;
            Task c = new Task();
            c.setId(t.getId());
            c.setProjectId(t.getProjectId());
            c.setName(t.getName());
            c.setDescription(t.getDescription());
            c.setAssignedEmployeeId(t.getAssignedEmployeeId());
            c.setPriority(t.getPriority());
            c.setDeadline(t.getDeadline());
            c.setStatus(t.getStatus());
            c.setCreatedAt(t.getCreatedAt());
            c.setUpdatedAt(t.getUpdatedAt());
            return c;
        }

        @Override
        public Optional<Task> findById(int id) {
            return Optional.ofNullable(copy(tasks.get(id)));
        }

        @Override
        public List<Task> findByProjectId(int projectId) {
            List<Task> result = new ArrayList<>();
            for (Task t : tasks.values()) {
                if (t.getProjectId() == projectId) {
                    result.add(copy(t));
                }
            }
            return result;
        }

        @Override
        public List<Task> findByEmployeeId(int employeeId) {
            List<Task> result = new ArrayList<>();
            for (Task t : tasks.values()) {
                if (t.getAssignedEmployeeId() != null && t.getAssignedEmployeeId() == employeeId) {
                    result.add(copy(t));
                }
            }
            return result;
        }

        @Override
        public List<Task> findAll() {
            List<Task> list = new ArrayList<>();
            for (Task t : tasks.values()) list.add(copy(t));
            return list;
        }

        @Override
        public Task create(Task task) {
            task.setId(idSequence++);
            tasks.put(task.getId(), copy(task));
            return copy(task);
        }

        @Override
        public void update(Task task) {
            tasks.put(task.getId(), copy(task));
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

        @Override
        public void deleteAllByUserId(int userId) {
            notifications.removeIf(n -> n.getUserId() == userId);
        }

        @Override
        public boolean existsUnread(int userId, String message) {
            return notifications.stream().anyMatch(n -> n.getUserId() == userId && !n.isRead() && n.getMessage().equals(message));
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
        reportService = new ReportServiceImpl(projectDAO, taskDAO, userDAO);
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

        // Clean session before registering manager (simulating logout/guest registration flow)
        UserSession.getInstance().cleanSession();

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

        // Clean session before registering another manager user (simulating guest signup flow)
        UserSession.getInstance().cleanSession();

        // Register a Manager user to try and assign a task to them (which is invalid)
        User anotherManager = new User();
        anotherManager.setUsername("manager2");
        anotherManager.setEmail("mgr2@intelliflow.com");
        anotherManager.setRole(Role.MANAGER); // Role is MANAGER, not EMPLOYEE
        anotherManager.setFullName("Second Manager");
        User savedMgr2 = userService.register(anotherManager, "Password123!");

        // Restore Manager Session for task operations
        UserSession.getInstance().startSession(manager);

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

        // Clean active manager session before registering another user (simulating guest signup flow)
        UserSession.getInstance().cleanSession();

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

    @Test
    public void testTaskSortingLogic() {
        // Create sample tasks
        Task t1 = new Task();
        t1.setName("Task Low No Deadline");
        t1.setPriority(TaskPriority.LOW);
        t1.setDeadline(null);

        Task t2 = new Task();
        t2.setName("Task Critical Due Today");
        t2.setPriority(TaskPriority.CRITICAL);
        t2.setDeadline(LocalDate.now());

        Task t3 = new Task();
        t3.setName("Task Critical Due in 3 days");
        t3.setPriority(TaskPriority.CRITICAL);
        t3.setDeadline(LocalDate.now().plusDays(3));

        Task t4 = new Task();
        t4.setName("Task High Due Tomorrow");
        t4.setPriority(TaskPriority.HIGH);
        t4.setDeadline(LocalDate.now().plusDays(1));

        Task t5 = new Task();
        t5.setName("Task High Due in 7 days");
        t5.setPriority(TaskPriority.HIGH);
        t5.setDeadline(LocalDate.now().plusDays(7));

        Task t6 = new Task();
        t6.setName("Task Medium Due in 2 days");
        t6.setPriority(TaskPriority.MEDIUM);
        t6.setDeadline(LocalDate.now().plusDays(2));

        Task t7 = new Task();
        t7.setName("Task Low Due Tomorrow");
        t7.setPriority(TaskPriority.LOW);
        t7.setDeadline(LocalDate.now().plusDays(1));

        List<Task> tasks = new java.util.ArrayList<>(List.of(t1, t2, t3, t4, t5, t6, t7));

        // Sort using RECOMMENDED_COMPARATOR
        tasks.sort(com.intelliflow.util.TaskSorter.RECOMMENDED_COMPARATOR);

        // Expected order:
        // 1. Task Critical Due Today (t2)
        // 2. Task Critical Due in 3 days (t3)
        // 3. Task High Due Tomorrow (t4)
        // 4. Task High Due in 7 days (t5)
        // 5. Task Medium Due in 2 days (t6)
        // 6. Task Low Due Tomorrow (t7)
        // 7. Task Low No Deadline (t1)

        assertEquals("Task Critical Due Today", tasks.get(0).getName());
        assertEquals("Task Critical Due in 3 days", tasks.get(1).getName());
        assertEquals("Task High Due Tomorrow", tasks.get(2).getName());
        assertEquals("Task High Due in 7 days", tasks.get(3).getName());
        assertEquals("Task Medium Due in 2 days", tasks.get(4).getName());
        assertEquals("Task Low Due Tomorrow", tasks.get(5).getName());
        assertEquals("Task Low No Deadline", tasks.get(6).getName());

        // Sort using PRIORITY_COMPARATOR
        tasks.sort(com.intelliflow.util.TaskSorter.PRIORITY_COMPARATOR);
        assertEquals(TaskPriority.CRITICAL, tasks.get(0).getPriority());
        assertEquals(TaskPriority.CRITICAL, tasks.get(1).getPriority());
        assertEquals(TaskPriority.HIGH, tasks.get(2).getPriority());
        assertEquals(TaskPriority.HIGH, tasks.get(3).getPriority());

        // Sort using DEADLINE_COMPARATOR
        tasks.sort(com.intelliflow.util.TaskSorter.DEADLINE_COMPARATOR);
        // t2 (due today) comes first since it is earliest
        assertEquals("Task Critical Due Today", tasks.get(0).getName());
        // t1 (no deadline) comes last
        assertEquals("Task Low No Deadline", tasks.get(6).getName());
    }

    @Test
    public void testTaskOptionalDeadlineAndPrioritySorting() throws Exception {
        // Create user/project to support task creation
        User manager = new User();
        manager.setUsername("sortingmanager");
        manager.setEmail("sortingmanager@intelliflow.com");
        manager.setRole(Role.MANAGER);
        manager.setFullName("Sorting Manager");
        User savedManager = userService.register(manager, "Password123!");

        // Security context - manager is active user
        UserSession.getInstance().startSession(savedManager);

        Project p = new Project();
        p.setName("Sorting Project");
        p.setDescription("Project to test priority sorting");
        p.setManagerId(savedManager.getId());
        p.setStartDate(LocalDate.of(2026, 9, 1));
        p.setDeadline(LocalDate.of(2026, 9, 30));
        p.setStatus(ProjectStatus.ACTIVE);
        Project savedProj = projectService.createProject(p);

        // 1. Create a task without a deadline
        Task taskNoDeadline = new Task();
        taskNoDeadline.setProjectId(savedProj.getId());
        taskNoDeadline.setName("Task with null deadline");
        taskNoDeadline.setPriority(TaskPriority.HIGH);
        taskNoDeadline.setDeadline(null);
        taskNoDeadline.setStatus(TaskStatus.TO_DO);

        Task savedTaskNoDeadline = taskService.createTask(taskNoDeadline);
        assertNotNull(savedTaskNoDeadline);
        assertNull(savedTaskNoDeadline.getDeadline());

        // 2. Perform same-priority and mixed sorting validation
        Task tCritical = new Task();
        tCritical.setName("Critical Due Today");
        tCritical.setPriority(TaskPriority.CRITICAL);
        tCritical.setDeadline(LocalDate.now());

        Task tCriticalLater = new Task();
        tCriticalLater.setName("Critical Due Tomorrow");
        tCriticalLater.setPriority(TaskPriority.CRITICAL);
        tCriticalLater.setDeadline(LocalDate.now().plusDays(1));

        Task tHighDeadline = new Task();
        tHighDeadline.setName("High Due Tomorrow");
        tHighDeadline.setPriority(TaskPriority.HIGH);
        tHighDeadline.setDeadline(LocalDate.now().plusDays(1));

        Task tHighNoDeadline = new Task();
        tHighNoDeadline.setName("High No Deadline");
        tHighNoDeadline.setPriority(TaskPriority.HIGH);
        tHighNoDeadline.setDeadline(null);

        List<Task> sortingTasks = new java.util.ArrayList<>(List.of(
            tHighNoDeadline, tCriticalLater, tHighDeadline, tCritical
        ));

        // Sort using RECOMMENDED_COMPARATOR
        sortingTasks.sort(com.intelliflow.util.TaskSorter.RECOMMENDED_COMPARATOR);

        // Expected sorted order:
        // 1. Critical Due Today (tCritical)
        // 2. Critical Due Tomorrow (tCriticalLater)
        // 3. High Due Tomorrow (tHighDeadline)
        // 4. High No Deadline (tHighNoDeadline)

        assertEquals("Critical Due Today", sortingTasks.get(0).getName());
        assertEquals("Critical Due Tomorrow", sortingTasks.get(1).getName());
        assertEquals("High Due Tomorrow", sortingTasks.get(2).getName());
        assertEquals("High No Deadline", sortingTasks.get(3).getName());
    }

    @Test
    public void testDeadlineIntelligenceAndOverdueDetection() {
        LocalDate today = LocalDate.of(2026, 9, 4);

        // 1. Future deadline (>7 days) -> ON_SCHEDULE
        Task futureTask = new Task();
        futureTask.setName("Future Architecture");
        futureTask.setDeadline(today.plusDays(10)); // Sep 14, 2026
        futureTask.setStatus(TaskStatus.TO_DO);
        assertEquals(com.intelliflow.enums.DeadlineState.ON_SCHEDULE, com.intelliflow.util.DeadlineUtil.calculateDeadlineState(futureTask, today));
        assertEquals("📅 Due: Sep 14, 2026", com.intelliflow.util.DeadlineUtil.formatDeadlineDisplay(futureTask, today, null));

        // 2. Upcoming (3-7 days) -> UPCOMING
        Task upcomingTask = new Task();
        upcomingTask.setName("Upcoming Feature");
        upcomingTask.setDeadline(today.plusDays(5)); // Sep 09, 2026
        upcomingTask.setStatus(TaskStatus.IN_PROGRESS);
        assertEquals(com.intelliflow.enums.DeadlineState.UPCOMING, com.intelliflow.util.DeadlineUtil.calculateDeadlineState(upcomingTask, today));
        assertEquals("📅 Due Soon: Sep 09, 2026", com.intelliflow.util.DeadlineUtil.formatDeadlineDisplay(upcomingTask, today, null));

        // 3. Due Soon (1-2 days) -> DUE_SOON
        Task dueSoonTask = new Task();
        dueSoonTask.setName("Due Soon Bugfix");
        dueSoonTask.setDeadline(today.plusDays(2)); // Sep 06, 2026
        dueSoonTask.setStatus(TaskStatus.TESTING);
        assertEquals(com.intelliflow.enums.DeadlineState.DUE_SOON, com.intelliflow.util.DeadlineUtil.calculateDeadlineState(dueSoonTask, today));
        assertEquals("⚠️ Due Soon: Sep 06, 2026", com.intelliflow.util.DeadlineUtil.formatDeadlineDisplay(dueSoonTask, today, null));

        // 4. Due Today (0 days) -> DUE_TODAY
        Task dueTodayTask = new Task();
        dueTodayTask.setName("Deploy Today");
        dueTodayTask.setDeadline(today);
        dueTodayTask.setStatus(TaskStatus.IN_PROGRESS);
        assertEquals(com.intelliflow.enums.DeadlineState.DUE_TODAY, com.intelliflow.util.DeadlineUtil.calculateDeadlineState(dueTodayTask, today));
        assertEquals("🔥 Due Today", com.intelliflow.util.DeadlineUtil.formatDeadlineDisplay(dueTodayTask, today, null));

        // 5. Overdue (<0 days) -> OVERDUE
        Task overdueTask = new Task();
        overdueTask.setName("Overdue Migration");
        overdueTask.setDeadline(today.minusDays(3)); // Sep 01, 2026
        overdueTask.setStatus(TaskStatus.TO_DO);
        assertEquals(com.intelliflow.enums.DeadlineState.OVERDUE, com.intelliflow.util.DeadlineUtil.calculateDeadlineState(overdueTask, today));
        assertEquals("⛔ Overdue: Sep 01, 2026", com.intelliflow.util.DeadlineUtil.formatDeadlineDisplay(overdueTask, today, null));

        // 6. Completed Overdue Task -> COMPLETED (Must NOT be marked as active overdue)
        Task completedOverdueTask = new Task();
        completedOverdueTask.setName("Completed Past Task");
        completedOverdueTask.setDeadline(today.minusDays(10)); // Aug 25, 2026
        completedOverdueTask.setStatus(TaskStatus.COMPLETED);
        assertEquals(com.intelliflow.enums.DeadlineState.COMPLETED, com.intelliflow.util.DeadlineUtil.calculateDeadlineState(completedOverdueTask, today));
        assertEquals("📅 Due: Aug 25, 2026", com.intelliflow.util.DeadlineUtil.formatDeadlineDisplay(completedOverdueTask, today, null));

        // 7. No deadline -> NO_DEADLINE
        Task noDeadlineTask = new Task();
        noDeadlineTask.setName("No Deadline Research");
        noDeadlineTask.setDeadline(null);
        noDeadlineTask.setStatus(TaskStatus.TO_DO);
        assertEquals(com.intelliflow.enums.DeadlineState.NO_DEADLINE, com.intelliflow.util.DeadlineUtil.calculateDeadlineState(noDeadlineTask, today));
        assertEquals("📅 No deadline", com.intelliflow.util.DeadlineUtil.formatDeadlineDisplay(noDeadlineTask, today, null));
    }

    @Test
    public void testProjectHealthCalculation() {
        LocalDate today = LocalDate.of(2026, 9, 4);

        Project project = new Project();
        project.setId(1);
        project.setName("Cloud Native Migration");
        project.setStartDate(today.minusDays(15));
        project.setDeadline(today.plusDays(30));
        project.setStatus(ProjectStatus.ACTIVE);

        // 1. All tasks on track / completed -> ON_TRACK
        Task task1 = new Task();
        task1.setStatus(TaskStatus.COMPLETED);
        task1.setPriority(TaskPriority.MEDIUM);

        Task task2 = new Task();
        task2.setStatus(TaskStatus.IN_PROGRESS);
        task2.setPriority(TaskPriority.LOW);
        task2.setDeadline(today.plusDays(10));

        assertEquals(com.intelliflow.enums.ProjectHealth.ON_TRACK, 
                com.intelliflow.util.ProjectHealthUtil.calculateProjectHealth(project, List.of(task1, task2), today));

        // 2. Critical incomplete task -> AT_RISK
        Task taskCritical = new Task();
        taskCritical.setStatus(TaskStatus.TO_DO);
        taskCritical.setPriority(TaskPriority.CRITICAL);
        taskCritical.setDeadline(today.plusDays(10));

        assertEquals(com.intelliflow.enums.ProjectHealth.AT_RISK,
                com.intelliflow.util.ProjectHealthUtil.calculateProjectHealth(project, List.of(task1, task2, taskCritical), today));

        // 3. Overdue incomplete task -> DELAYED
        Task taskOverdue = new Task();
        taskOverdue.setStatus(TaskStatus.IN_PROGRESS);
        taskOverdue.setPriority(TaskPriority.LOW);
        taskOverdue.setDeadline(today.minusDays(2)); // Overdue

        assertEquals(com.intelliflow.enums.ProjectHealth.DELAYED,
                com.intelliflow.util.ProjectHealthUtil.calculateProjectHealth(project, List.of(task1, task2, taskOverdue), today));

        // 4. Overdue Project Deadline itself -> DELAYED
        Project expiredProject = new Project();
        expiredProject.setId(2);
        expiredProject.setName("Past Project");
        expiredProject.setDeadline(today.minusDays(1));
        expiredProject.setStatus(ProjectStatus.ACTIVE);

        assertEquals(com.intelliflow.enums.ProjectHealth.DELAYED,
                com.intelliflow.util.ProjectHealthUtil.calculateProjectHealth(expiredProject, List.of(task1), today));

        // 5. Completed Project -> ON_TRACK
        Project completedProject = new Project();
        completedProject.setId(3);
        completedProject.setName("Finished Project");
        completedProject.setDeadline(today.minusDays(5));
        completedProject.setStatus(ProjectStatus.COMPLETED);

        assertEquals(com.intelliflow.enums.ProjectHealth.ON_TRACK,
                com.intelliflow.util.ProjectHealthUtil.calculateProjectHealth(completedProject, List.of(task1, taskOverdue), today));
    }

    @Test
    public void testSmartTaskAlertsGenerationAndRoleFiltering() {
        LocalDate today = LocalDate.of(2026, 9, 4);

        User adminUser = new User();
        adminUser.setId(1);
        adminUser.setRole(Role.ADMIN);

        User manager1 = new User();
        manager1.setId(2);
        manager1.setRole(Role.MANAGER);

        User employee1 = new User();
        employee1.setId(3);
        employee1.setRole(Role.EMPLOYEE);

        User employee2 = new User();
        employee2.setId(4);
        employee2.setRole(Role.EMPLOYEE);

        Project projectManaged = new Project();
        projectManaged.setId(10);
        projectManaged.setName("Project Alpha");
        projectManaged.setManagerId(manager1.getId());
        projectManaged.setDeadline(today.plusDays(20));
        projectManaged.setStatus(ProjectStatus.ACTIVE);

        Project projectOther = new Project();
        projectOther.setId(20);
        projectOther.setName("Project Beta");
        projectOther.setManagerId(99); // Other manager
        projectOther.setDeadline(today.plusDays(20));
        projectOther.setStatus(ProjectStatus.ACTIVE);

        // Task 1: Assigned to Employee 1 (overdue)
        Task task1 = new Task();
        task1.setId(101);
        task1.setProjectId(projectManaged.getId());
        task1.setAssignedEmployeeId(employee1.getId());
        task1.setPriority(TaskPriority.HIGH);
        task1.setDeadline(today.minusDays(2)); // Overdue
        task1.setStatus(TaskStatus.IN_PROGRESS);

        // Task 2: Assigned to Employee 2 in other project (critical due today)
        Task task2 = new Task();
        task2.setId(102);
        task2.setProjectId(projectOther.getId());
        task2.setAssignedEmployeeId(employee2.getId());
        task2.setPriority(TaskPriority.CRITICAL);
        task2.setDeadline(today); // Due today
        task2.setStatus(TaskStatus.TO_DO);

        // Task 3: Assigned to Employee 2 in other project (medium due tomorrow -> due soon)
        Task task3 = new Task();
        task3.setId(103);
        task3.setProjectId(projectOther.getId());
        task3.setAssignedEmployeeId(employee2.getId());
        task3.setPriority(TaskPriority.MEDIUM);
        task3.setDeadline(today.plusDays(1)); // Due soon
        task3.setStatus(TaskStatus.TO_DO);

        List<Project> allProjects = List.of(projectManaged, projectOther);
        List<Task> allTasks = List.of(task1, task2, task3);

        // 1. Admin should see all alerts (overdue task + critical due today + due soon + delayed projectAlpha)
        List<com.intelliflow.util.AlertUtil.SmartAlert> adminAlerts = 
                com.intelliflow.util.AlertUtil.generateAlertsForUser(adminUser, allProjects, allTasks, today);
        assertTrue(adminAlerts.stream().anyMatch(a -> a.getMessage().contains("overdue task")));
        assertTrue(adminAlerts.stream().anyMatch(a -> a.getMessage().contains("due today")));
        assertTrue(adminAlerts.stream().anyMatch(a -> a.getMessage().contains("due soon")));

        // 2. Manager 1 should only see alerts for Project Alpha (task 1 overdue), NOT task 2 & 3 (Project Beta)
        List<com.intelliflow.util.AlertUtil.SmartAlert> managerAlerts = 
                com.intelliflow.util.AlertUtil.generateAlertsForUser(manager1, allProjects, allTasks, today);
        assertTrue(managerAlerts.stream().anyMatch(a -> a.getMessage().contains("overdue task")));
        assertFalse(managerAlerts.stream().anyMatch(a -> a.getMessage().contains("due today")));
        assertFalse(managerAlerts.stream().anyMatch(a -> a.getMessage().contains("due soon")));

        // 3. Employee 1 should only see alerts for their assigned task (task 1 overdue), NOT task 2 & 3
        List<com.intelliflow.util.AlertUtil.SmartAlert> emp1Alerts = 
                com.intelliflow.util.AlertUtil.generateAlertsForUser(employee1, allProjects, allTasks, today);
        assertEquals(1, emp1Alerts.size());
        assertTrue(emp1Alerts.get(0).getMessage().contains("overdue task"));

        // 4. Employee 2 should only see alerts for their assigned tasks (task 2 critical due today + task 3 due soon), NOT task 1
        List<com.intelliflow.util.AlertUtil.SmartAlert> emp2Alerts = 
                com.intelliflow.util.AlertUtil.generateAlertsForUser(employee2, allProjects, allTasks, today);
        assertEquals(2, emp2Alerts.size()); // critical due today + due soon
        assertTrue(emp2Alerts.stream().anyMatch(a -> a.getMessage().contains("due today")));
        assertTrue(emp2Alerts.stream().anyMatch(a -> a.getMessage().contains("due soon")));
        assertFalse(emp2Alerts.stream().anyMatch(a -> a.getMessage().contains("overdue task")));
    }

    @Test
    public void testNotificationCenterOperationsAndEventTriggers() throws Exception {
        InMemoryUserDAO uDAO = new InMemoryUserDAO();
        InMemoryProjectDAO pDAO = new InMemoryProjectDAO();
        InMemoryTaskDAO tDAO = new InMemoryTaskDAO();
        InMemoryNotificationDAO nDAO = new InMemoryNotificationDAO();
        InMemoryActivityLogDAO lDAO = new InMemoryActivityLogDAO();

        NotificationService notifService = new NotificationServiceImpl(nDAO, tDAO, pDAO);
        TaskService tService = new TaskServiceImpl(tDAO, pDAO, uDAO, nDAO, lDAO);
        ProjectService pService = new ProjectServiceImpl(pDAO, lDAO, nDAO);

        User admin = new User(0, "admin", "admin@test.com", "hash", Role.ADMIN, "Admin User", LocalDateTime.now());
        admin = uDAO.create(admin);

        User manager = new User(0, "manager", "manager@test.com", "hash", Role.MANAGER, "Manager User", LocalDateTime.now());
        manager = uDAO.create(manager);

        User emp = new User(0, "employee", "emp@test.com", "hash", Role.EMPLOYEE, "Employee User", LocalDateTime.now());
        emp = uDAO.create(emp);

        // 1. Notification CRUD & Read/Unread State Tests
        Notification n1 = notifService.createNotification(emp.getId(), "Test alert 1");
        Notification n2 = notifService.createNotification(emp.getId(), "Test alert 2");

        assertEquals(2, notifService.getNotificationsForUser(emp.getId()).size());
        assertEquals(2, notifService.getUnreadNotificationsForUser(emp.getId()).size());

        // Mark single as read
        notifService.markAsRead(n1.getId());
        assertEquals(1, notifService.getUnreadNotificationsForUser(emp.getId()).size());

        // Mark all as read
        notifService.markAllAsRead(emp.getId());
        assertEquals(0, notifService.getUnreadNotificationsForUser(emp.getId()).size());

        // Delete single
        notifService.deleteNotification(n1.getId());
        assertEquals(1, notifService.getNotificationsForUser(emp.getId()).size());

        // Clear all
        notifService.deleteAllNotificationsForUser(emp.getId());
        assertEquals(0, notifService.getNotificationsForUser(emp.getId()).size());

        // 2. Project Assignment Event Notification
        UserSession.getInstance().startSession(admin);
        Project proj = new Project();
        proj.setName("Cloud Migration");
        proj.setStartDate(LocalDate.now());
        proj.setDeadline(LocalDate.now().plusDays(30));
        proj.setStatus(ProjectStatus.ACTIVE);
        proj.setManagerId(manager.getId());
        Project createdProj = pService.createProject(proj);

        List<Notification> mgrNotifs = notifService.getNotificationsForUser(manager.getId());
        assertEquals(1, mgrNotifs.size());
        assertTrue(mgrNotifs.get(0).getMessage().contains("assigned as Manager"));

        // 3. Task Assignment Event Notification
        Task task = new Task();
        task.setProjectId(createdProj.getId());
        task.setName("Deploy Database Cluster");
        task.setPriority(TaskPriority.HIGH);
        task.setStatus(TaskStatus.TO_DO);
        task.setDeadline(LocalDate.now().plusDays(10));
        task.setAssignedEmployeeId(emp.getId());
        Task createdTask = tService.createTask(task);

        List<Notification> empNotifs = notifService.getNotificationsForUser(emp.getId());
        assertEquals(1, empNotifs.size());
        assertTrue(empNotifs.get(0).getMessage().contains("assigned to new task"));

        // 4. Task Priority Change Notification
        createdTask.setPriority(TaskPriority.CRITICAL);
        tService.updateTask(createdTask);

        empNotifs = notifService.getNotificationsForUser(emp.getId());
        assertEquals(2, empNotifs.size());
        assertTrue(empNotifs.stream().anyMatch(n -> n.getMessage().contains("priority changed")));

        // 5. Task Status Change Notification
        UserSession.getInstance().startSession(emp);
        tService.updateTaskStatus(createdTask.getId(), TaskStatus.IN_PROGRESS);

        mgrNotifs = notifService.getNotificationsForUser(manager.getId());
        assertTrue(mgrNotifs.stream().anyMatch(n -> n.getMessage().contains("status updated to IN_PROGRESS")));

        // 6. Deadline Notifications & Deduplication Test
        Task overdueTask = new Task();
        overdueTask.setProjectId(createdProj.getId());
        overdueTask.setName("Urgent Bugfix");
        overdueTask.setPriority(TaskPriority.CRITICAL);
        overdueTask.setStatus(TaskStatus.IN_PROGRESS);
        overdueTask.setDeadline(LocalDate.now().minusDays(3)); // Overdue
        overdueTask.setAssignedEmployeeId(emp.getId());
        tDAO.create(overdueTask);

        int generated = notifService.checkAndGenerateDeadlineNotifications(LocalDate.now());
        assertTrue(generated >= 1);

        empNotifs = notifService.getNotificationsForUser(emp.getId());
        assertTrue(empNotifs.stream().anyMatch(n -> n.getMessage().contains("Task overdue")));

        // Re-running deadline check should NOT duplicate unread notifications
        int generatedAgain = notifService.checkAndGenerateDeadlineNotifications(LocalDate.now());
        assertEquals(0, generatedAgain);
    }

    @Test
    public void testActivityTimelineTrackingAndRoleVisibility() throws Exception {
        InMemoryUserDAO uDAO = new InMemoryUserDAO();
        InMemoryProjectDAO pDAO = new InMemoryProjectDAO();
        InMemoryTaskDAO tDAO = new InMemoryTaskDAO();
        InMemoryNotificationDAO nDAO = new InMemoryNotificationDAO();
        InMemoryActivityLogDAO lDAO = new InMemoryActivityLogDAO();

        UserService uService = new UserServiceImpl(uDAO, lDAO, pDAO, tDAO);
        ProjectService pService = new ProjectServiceImpl(pDAO, lDAO, nDAO);
        TaskService tService = new TaskServiceImpl(tDAO, pDAO, uDAO, nDAO, lDAO);

        User admin = uDAO.create(new User(0, "admin", "admin@test.com", "hash", Role.ADMIN, "Abhay", LocalDateTime.now()));
        User manager1 = uDAO.create(new User(0, "manager1", "mgr1@test.com", "hash", Role.MANAGER, "Rahul", LocalDateTime.now()));
        User employee1 = uDAO.create(new User(0, "emp1", "emp1@test.com", "hash", Role.EMPLOYEE, "Harsha", LocalDateTime.now()));
        User otherManager = uDAO.create(new User(0, "manager2", "mgr2@test.com", "hash", Role.MANAGER, "Other Mgr", LocalDateTime.now()));

        // 1. User Creation & Role Change
        UserSession.getInstance().startSession(admin);
        User newDev = new User(0, "dev1", "dev1@test.com", "", Role.EMPLOYEE, "Dev One", LocalDateTime.now());
        User createdDev = uService.register(newDev, "Pass123!@#");

        List<ActivityLog> allLogs = lDAO.findAll();
        assertTrue(allLogs.stream().anyMatch(l -> "USER_CREATE".equals(l.getAction()) && l.getDescription().contains("created user dev1")));
        // Verify password is never logged
        assertFalse(allLogs.stream().anyMatch(l -> l.getDescription().contains("Pass123!@#")));

        // Role change
        createdDev.setRole(Role.MANAGER);
        uService.updateUser(createdDev);
        allLogs = lDAO.findAll();
        assertTrue(allLogs.stream().anyMatch(l -> "USER_ROLE_CHANGE".equals(l.getAction()) && l.getDescription().contains("role changed from EMPLOYEE to MANAGER")));

        // 2. Project Creation, Status & Deadline Change
        UserSession.getInstance().startSession(admin);
        Project project = new Project();
        project.setName("IntelliFlow");
        project.setStartDate(LocalDate.now());
        project.setDeadline(LocalDate.now().plusDays(20));
        project.setStatus(ProjectStatus.ACTIVE);
        project.setManagerId(manager1.getId());
        Project createdProj = pService.createProject(project);

        allLogs = lDAO.findAll();
        assertTrue(allLogs.stream().anyMatch(l -> "PROJECT_CREATE".equals(l.getAction()) && l.getDescription().contains("Abhay created project IntelliFlow")));

        // Project deadline change
        createdProj.setDeadline(LocalDate.now().plusDays(40));
        pService.updateProject(createdProj);
        allLogs = lDAO.findAll();
        assertTrue(allLogs.stream().anyMatch(l -> "PROJECT_DEADLINE_CHANGE".equals(l.getAction()) && l.getDescription().contains("deadline changed to")));

        // Project status change
        createdProj.setStatus(ProjectStatus.COMPLETED);
        pService.updateProject(createdProj);
        allLogs = lDAO.findAll();
        assertTrue(allLogs.stream().anyMatch(l -> "PROJECT_STATUS_CHANGE".equals(l.getAction()) && l.getDescription().contains("status changed ACTIVE → COMPLETED")));

        // 3. Task Creation, Assignment, Priority, Deadline & Status Change
        UserSession.getInstance().startSession(manager1);
        Task task = new Task();
        task.setProjectId(createdProj.getId());
        task.setName("Login Module");
        task.setPriority(TaskPriority.HIGH);
        task.setStatus(TaskStatus.TO_DO);
        task.setDeadline(LocalDate.now().plusDays(10));
        task.setAssignedEmployeeId(employee1.getId());
        Task createdTask = tService.createTask(task);

        allLogs = lDAO.findAll();
        assertTrue(allLogs.stream().anyMatch(l -> "TASK_CREATE".equals(l.getAction()) && l.getDescription().contains("Rahul created task Login Module")));
        assertTrue(allLogs.stream().anyMatch(l -> "TASK_ASSIGN".equals(l.getAction()) && l.getDescription().contains("Login Module' assigned to Harsha")));

        // Task priority change
        createdTask.setPriority(TaskPriority.CRITICAL);
        tService.updateTask(createdTask);
        allLogs = lDAO.findAll();
        assertTrue(allLogs.stream().anyMatch(l -> "TASK_PRIORITY_CHANGE".equals(l.getAction()) && l.getDescription().contains("Login Module priority changed HIGH → CRITICAL")));

        // Task deadline change
        createdTask.setDeadline(LocalDate.now().plusDays(15));
        tService.updateTask(createdTask);
        allLogs = lDAO.findAll();
        assertTrue(allLogs.stream().anyMatch(l -> "TASK_DEADLINE_CHANGE".equals(l.getAction()) && l.getDescription().contains("Login Module deadline changed to")));

        // Task status change (moved to TESTING)
        UserSession.getInstance().startSession(employee1);
        tService.updateTaskStatus(createdTask.getId(), TaskStatus.IN_PROGRESS);
        tService.updateTaskStatus(createdTask.getId(), TaskStatus.TESTING);
        allLogs = lDAO.findAll();
        assertTrue(allLogs.stream().anyMatch(l -> "TASK_STATUS_CHANGE".equals(l.getAction()) && l.getDescription().contains("Login Module moved to TESTING")));

        // 4. Role-based Visibility Verification
        // Admin sees all activity logs
        List<ActivityLog> adminVisibleLogs = uService.getActivityLogsForUser(admin);
        assertEquals(allLogs.size(), adminVisibleLogs.size());

        // Manager 1 sees activity for IntelliFlow and Login Module
        List<ActivityLog> mgr1Logs = uService.getActivityLogsForUser(manager1);
        assertTrue(mgr1Logs.size() > 0);
        assertTrue(mgr1Logs.stream().anyMatch(l -> l.getDescription().contains("IntelliFlow")));
        assertTrue(mgr1Logs.stream().anyMatch(l -> l.getDescription().contains("Login Module")));

        // Other manager (with no projects) should NOT see Manager 1's project activity
        List<ActivityLog> otherMgrLogs = uService.getActivityLogsForUser(otherManager);
        assertFalse(otherMgrLogs.stream().anyMatch(l -> l.getDescription().contains("IntelliFlow")));
        assertFalse(otherMgrLogs.stream().anyMatch(l -> l.getDescription().contains("Login Module")));

        // Employee 1 sees activity for their assigned task (Login Module)
        List<ActivityLog> emp1Logs = uService.getActivityLogsForUser(employee1);
        assertTrue(emp1Logs.stream().anyMatch(l -> l.getDescription().contains("Login Module")));
    }

    // 16. Advanced Real-Data Analytics & Role Visibility Tests (Phase 7)
    @Test
    public void testAdvancedAnalyticsAndRoleVisibility() throws DatabaseException, ValidationException {
        UserDAO uDAO = new InMemoryUserDAO();
        ProjectDAO pDAO = new InMemoryProjectDAO();
        TaskDAO tDAO = new InMemoryTaskDAO();
        NotificationDAO nDAO = new InMemoryNotificationDAO();
        ActivityLogDAO lDAO = new InMemoryActivityLogDAO();

        UserService uService = new UserServiceImpl(uDAO, lDAO);
        ProjectService pService = new ProjectServiceImpl(pDAO, lDAO);
        TaskService tService = new TaskServiceImpl(tDAO, pDAO, uDAO, nDAO, lDAO);
        ReportService rService = new ReportServiceImpl(pDAO, tDAO, uDAO);

        LocalDate today = LocalDate.of(2026, 9, 5);

        // 1. Setup Users
        User admin = uService.register(new User(0, "adminUser", "admin@test.com", "", Role.ADMIN, "Admin User", LocalDateTime.now()), "Pass123!@#");
        User mgr1 = uService.register(new User(0, "mgr1", "mgr1@test.com", "", Role.MANAGER, "Manager One", LocalDateTime.now()), "Pass123!@#");
        User mgr2 = uService.register(new User(0, "mgr2", "mgr2@test.com", "", Role.MANAGER, "Manager Two", LocalDateTime.now()), "Pass123!@#");
        User emp1 = uService.register(new User(0, "emp1", "emp1@test.com", "", Role.EMPLOYEE, "Employee One", LocalDateTime.now()), "Pass123!@#");
        User emp2 = uService.register(new User(0, "emp2", "emp2@test.com", "", Role.EMPLOYEE, "Employee Two", LocalDateTime.now()), "Pass123!@#");

        // 2. Setup Projects
        UserSession.getInstance().startSession(admin);
        Project p1 = new Project();
        p1.setName("IntelliFlow Core");
        p1.setManagerId(mgr1.getId());
        p1.setStartDate(today.minusDays(10));
        p1.setDeadline(today.plusDays(20));
        p1.setStatus(ProjectStatus.ACTIVE);
        p1 = pService.createProject(p1);

        Project p2 = new Project();
        p2.setName("Mobile App");
        p2.setManagerId(mgr2.getId());
        p2.setStartDate(today.minusDays(5));
        p2.setDeadline(today.minusDays(1)); // Overdue project deadline -> DELAYED
        p2.setStatus(ProjectStatus.ACTIVE);
        p2 = pService.createProject(p2);

        // 3. Setup Tasks for Project 1 (Managed by mgr1)
        UserSession.getInstance().startSession(mgr1);
        // Task 1: Completed, LOW priority
        Task t1 = new Task();
        t1.setProjectId(p1.getId());
        t1.setName("Setup DB");
        t1.setPriority(TaskPriority.LOW);
        t1.setStatus(TaskStatus.COMPLETED);
        t1.setAssignedEmployeeId(emp1.getId());
        t1.setDeadline(today.minusDays(2));
        tService.createTask(t1);

        // Task 2: In Progress, CRITICAL priority, due in 1 day (Due Soon)
        Task t2 = new Task();
        t2.setProjectId(p1.getId());
        t2.setName("Auth Module");
        t2.setPriority(TaskPriority.CRITICAL);
        t2.setStatus(TaskStatus.IN_PROGRESS);
        t2.setAssignedEmployeeId(emp1.getId());
        t2.setDeadline(today.plusDays(1));
        tService.createTask(t2);

        // Task 3: Testing, HIGH priority, due in 5 days
        Task t3 = new Task();
        t3.setProjectId(p1.getId());
        t3.setName("API Testing");
        t3.setPriority(TaskPriority.HIGH);
        t3.setStatus(TaskStatus.TESTING);
        t3.setAssignedEmployeeId(emp2.getId());
        t3.setDeadline(today.plusDays(5));
        tService.createTask(t3);

        // Task 4: To Do, MEDIUM priority, overdue
        Task t4 = new Task();
        t4.setProjectId(p1.getId());
        t4.setName("Documentation");
        t4.setPriority(TaskPriority.MEDIUM);
        t4.setStatus(TaskStatus.TO_DO);
        t4.setAssignedEmployeeId(emp2.getId());
        t4.setDeadline(today.minusDays(1)); // Overdue
        tService.createTask(t4);

        // 4. Setup Tasks for Project 2 (Managed by mgr2)
        UserSession.getInstance().startSession(mgr2);
        Task t5 = new Task();
        t5.setProjectId(p2.getId());
        t5.setName("UI Mockups");
        t5.setPriority(TaskPriority.MEDIUM);
        t5.setStatus(TaskStatus.COMPLETED);
        t5.setAssignedEmployeeId(emp2.getId());
        t5.setDeadline(today.minusDays(3));
        tService.createTask(t5);

        // ============================================
        // A. ADMIN ANALYTICS (System-wide: 5 tasks total, 2 projects)
        // ============================================
        AnalyticsSummary adminSummary = rService.getAnalyticsSummary(admin, today);
        assertEquals(5, adminSummary.getTotalTasks());
        assertEquals(2, adminSummary.getCompletedTasks());
        assertEquals(40.0, adminSummary.getTaskCompletionRate()); // 2/5 = 40.0%
        assertEquals(1, adminSummary.getOverdueTaskCount()); // t4 is overdue
        assertEquals(1, adminSummary.getDueSoonTaskCount()); // t2 is due in 1 day

        // Priority distribution
        assertEquals(1, adminSummary.getPriorityDistribution().get(TaskPriority.CRITICAL));
        assertEquals(1, adminSummary.getPriorityDistribution().get(TaskPriority.HIGH));
        assertEquals(2, adminSummary.getPriorityDistribution().get(TaskPriority.MEDIUM));
        assertEquals(1, adminSummary.getPriorityDistribution().get(TaskPriority.LOW));

        // Status distribution
        assertEquals(1, adminSummary.getStatusDistribution().get(TaskStatus.TO_DO));
        assertEquals(1, adminSummary.getStatusDistribution().get(TaskStatus.IN_PROGRESS));
        assertEquals(1, adminSummary.getStatusDistribution().get(TaskStatus.TESTING));
        assertEquals(2, adminSummary.getStatusDistribution().get(TaskStatus.COMPLETED));

        // Project progress & health
        assertEquals(2, adminSummary.getProjectProgressList().size());
        assertEquals(2, adminSummary.getEmployeeWorkloads().size());

        // ============================================
        // B. MANAGER 1 ANALYTICS (Scoped to Project 1: 4 tasks)
        // ============================================
        AnalyticsSummary mgr1Summary = rService.getAnalyticsSummary(mgr1, today);
        assertEquals(4, mgr1Summary.getTotalTasks());
        assertEquals(1, mgr1Summary.getCompletedTasks());
        assertEquals(25.0, mgr1Summary.getTaskCompletionRate()); // 1/4 = 25.0%
        assertEquals(1, mgr1Summary.getOverdueTaskCount());
        assertEquals(1, mgr1Summary.getDueSoonTaskCount());
        assertEquals(1, mgr1Summary.getProjectProgressList().size());
        assertEquals("IntelliFlow Core", mgr1Summary.getProjectProgressList().get(0).getProjectName());

        // ============================================
        // C. EMPLOYEE 1 ANALYTICS (Scoped to emp1 assigned tasks: t1, t2)
        // ============================================
        AnalyticsSummary emp1Summary = rService.getAnalyticsSummary(emp1, today);
        assertEquals(2, emp1Summary.getTotalTasks());
        assertEquals(1, emp1Summary.getCompletedTasks());
        assertEquals(50.0, emp1Summary.getTaskCompletionRate()); // 1/2 = 50.0%
        assertEquals(0, emp1Summary.getOverdueTaskCount());
        assertEquals(1, emp1Summary.getDueSoonTaskCount()); // t2 is due soon
        assertEquals(1, emp1Summary.getPriorityDistribution().get(TaskPriority.CRITICAL));
        assertEquals(1, emp1Summary.getPriorityDistribution().get(TaskPriority.LOW));

        // ============================================
        // D. Visual ASCII Progress Track Formatting
        // ============================================
        String progressVisual = com.intelliflow.ui.views.ReportsView.formatAsciiProgressBar(78.0);
        assertTrue(progressVisual.contains("78%"));
        assertTrue(progressVisual.contains("█"));
        assertTrue(progressVisual.contains("░"));
    }

    // 17. Team Workload Management & Assignment Authorization Tests (Phase 8)
    @Test
    public void testTeamWorkloadManagementAndAuthorization() throws DatabaseException, ValidationException {
        UserDAO uDAO = new InMemoryUserDAO();
        ProjectDAO pDAO = new InMemoryProjectDAO();
        TaskDAO tDAO = new InMemoryTaskDAO();
        NotificationDAO nDAO = new InMemoryNotificationDAO();
        ActivityLogDAO lDAO = new InMemoryActivityLogDAO();

        UserService uService = new UserServiceImpl(uDAO, lDAO);
        ProjectService pService = new ProjectServiceImpl(pDAO, lDAO);
        TaskService tService = new TaskServiceImpl(tDAO, pDAO, uDAO, nDAO, lDAO);

        LocalDate today = LocalDate.of(2026, 9, 5);

        // 1. Setup Users (Admin, 2 Managers, 2 Employees)
        User admin = uService.register(new User(0, "adminUser", "admin@test.com", "", Role.ADMIN, "Admin User", LocalDateTime.now()), "Pass123!@#");
        User mgr1 = uService.register(new User(0, "mgr1", "mgr1@test.com", "", Role.MANAGER, "Manager One", LocalDateTime.now()), "Pass123!@#");
        User mgr2 = uService.register(new User(0, "mgr2", "mgr2@test.com", "", Role.MANAGER, "Manager Two", LocalDateTime.now()), "Pass123!@#");
        User priya = uService.register(new User(0, "priya", "priya@test.com", "", Role.EMPLOYEE, "Priya", LocalDateTime.now()), "Pass123!@#");
        User rahul = uService.register(new User(0, "rahul", "rahul@test.com", "", Role.EMPLOYEE, "Rahul", LocalDateTime.now()), "Pass123!@#");

        // 2. Setup Projects
        UserSession.getInstance().startSession(admin);
        Project p1 = new Project();
        p1.setName("IntelliFlow");
        p1.setManagerId(mgr1.getId());
        p1.setStartDate(today.minusDays(10));
        p1.setDeadline(today.plusDays(30));
        p1.setStatus(ProjectStatus.ACTIVE);
        p1 = pService.createProject(p1);

        Project p2 = new Project();
        p2.setName("Finance App");
        p2.setManagerId(mgr2.getId());
        p2.setStartDate(today.minusDays(10));
        p2.setDeadline(today.plusDays(30));
        p2.setStatus(ProjectStatus.ACTIVE);
        p2 = pService.createProject(p2);

        // 3. Workload Calculation Test for Priya:
        // 8 assigned, 6 completed, 1 in-progress, 1 overdue -> 75% completion
        UserSession.getInstance().startSession(mgr1);
        List<Task> priyaTasks = new ArrayList<>();
        // 6 Completed Tasks
        for (int i = 1; i <= 6; i++) {
            Task t = new Task();
            t.setProjectId(p1.getId());
            t.setName("Completed Task " + i);
            t.setStatus(TaskStatus.COMPLETED);
            t.setPriority(TaskPriority.MEDIUM);
            t.setAssignedEmployeeId(priya.getId());
            t.setDeadline(today.minusDays(5));
            priyaTasks.add(tService.createTask(t));
        }
        // 1 In Progress (due in future)
        Task tInProgress = new Task();
        tInProgress.setProjectId(p1.getId());
        tInProgress.setName("Active Task");
        tInProgress.setStatus(TaskStatus.IN_PROGRESS);
        tInProgress.setPriority(TaskPriority.HIGH);
        tInProgress.setAssignedEmployeeId(priya.getId());
        tInProgress.setDeadline(today.plusDays(5));
        priyaTasks.add(tService.createTask(tInProgress));

        // 1 Overdue Task
        Task tOverdue = new Task();
        tOverdue.setProjectId(p1.getId());
        tOverdue.setName("Overdue Task");
        tOverdue.setStatus(TaskStatus.TO_DO);
        tOverdue.setPriority(TaskPriority.CRITICAL);
        tOverdue.setAssignedEmployeeId(priya.getId());
        tOverdue.setDeadline(today.minusDays(2));
        priyaTasks.add(tService.createTask(tOverdue));

        TeamMemberWorkload priyaWorkload = WorkloadUtil.calculateMemberWorkload(priya, priyaTasks, today);
        assertEquals(8, priyaWorkload.getAssignedTasks());
        assertEquals(6, priyaWorkload.getCompletedTasks());
        assertEquals(2, priyaWorkload.getInProgressTasks());
        assertEquals(1, priyaWorkload.getOverdueTasks());
        assertEquals(75.0, priyaWorkload.getCompletionPercentage()); // 6/8 = 75.0%
        assertEquals("🟠 HEAVY", priyaWorkload.getWorkloadIndicator()); // 1 overdue -> HEAVY

        // 4. Team Workload in Project 1
        List<TeamMemberWorkload> teamWorkloads = WorkloadUtil.calculateTeamWorkloadForProject(
                List.of(priya, rahul), priyaTasks, today
        );
        assertEquals(1, teamWorkloads.size());
        assertEquals("Priya", teamWorkloads.get(0).getEmployeeName());

        // 5. Authorization: Avoid assigning tasks to unauthorized users (non-employees / managers / admins)
        Task invalidAssignTask = new Task();
        invalidAssignTask.setProjectId(p1.getId());
        invalidAssignTask.setName("Invalid Assignee Task");
        invalidAssignTask.setStatus(TaskStatus.TO_DO);
        invalidAssignTask.setPriority(TaskPriority.LOW);
        invalidAssignTask.setAssignedEmployeeId(mgr2.getId()); // mgr2 is Role.MANAGER, not EMPLOYEE
        assertThrows(ValidationException.class, () -> tService.createTask(invalidAssignTask));

        // Assign to non-existent user id
        invalidAssignTask.setAssignedEmployeeId(9999);
        assertThrows(ValidationException.class, () -> tService.createTask(invalidAssignTask));

        // 6. Authorization: Do not give Employees Manager permissions
        UserSession.getInstance().startSession(priya); // Employee session
        Task empCreateAttempt = new Task();
        empCreateAttempt.setProjectId(p1.getId());
        empCreateAttempt.setName("Employee Creating Task");
        empCreateAttempt.setStatus(TaskStatus.TO_DO);
        empCreateAttempt.setPriority(TaskPriority.LOW);
        empCreateAttempt.setAssignedEmployeeId(priya.getId());
        assertThrows(UnauthorizedException.class, () -> tService.createTask(empCreateAttempt));

        assertThrows(UnauthorizedException.class, () -> tService.deleteTask(tInProgress.getId()));

        // Employee cannot update status of a task assigned to another employee
        Task rahulTask = new Task();
        rahulTask.setProjectId(p1.getId());
        rahulTask.setName("Rahul Task");
        rahulTask.setStatus(TaskStatus.TO_DO);
        rahulTask.setPriority(TaskPriority.MEDIUM);
        rahulTask.setAssignedEmployeeId(rahul.getId());

        UserSession.getInstance().startSession(mgr1);
        Task createdRahulTask = tService.createTask(rahulTask);

        UserSession.getInstance().startSession(priya); // Priya tries to update Rahul's task status
        assertThrows(UnauthorizedException.class, () -> tService.updateTaskStatus(createdRahulTask.getId(), TaskStatus.IN_PROGRESS));

        // 7. Authorization: Manager 1 cannot manage or delete tasks in Manager 2's project
        UserSession.getInstance().startSession(mgr2);
        Task p2Task = new Task();
        p2Task.setProjectId(p2.getId());
        p2Task.setName("Finance Task");
        p2Task.setStatus(TaskStatus.TO_DO);
        p2Task.setPriority(TaskPriority.HIGH);
        p2Task.setAssignedEmployeeId(rahul.getId());
        Task createdP2Task = tService.createTask(p2Task);

        UserSession.getInstance().startSession(mgr1); // Manager 1 tries to edit or delete Manager 2's task
        assertThrows(UnauthorizedException.class, () -> tService.deleteTask(createdP2Task.getId()));
        createdP2Task.setName("Hacked Task Name");
        assertThrows(UnauthorizedException.class, () -> tService.updateTask(createdP2Task));
    }

    @Test
    public void testKanbanWorkflowTransitionsAndValidation() throws Exception {
        UserDAO uDAO = new InMemoryUserDAO();
        ProjectDAO pDAO = new InMemoryProjectDAO();
        TaskDAO tDAO = new InMemoryTaskDAO();
        NotificationDAO nDAO = new InMemoryNotificationDAO();
        ActivityLogDAO lDAO = new InMemoryActivityLogDAO();

        UserService uService = new UserServiceImpl(uDAO, lDAO);
        ProjectService pService = new ProjectServiceImpl(pDAO, lDAO);
        TaskService tService = new TaskServiceImpl(tDAO, pDAO, uDAO, nDAO, lDAO);

        LocalDate today = LocalDate.of(2026, 9, 5);

        // 1. Setup Users (Admin, Manager, 2 Employees)
        User admin = uService.register(new User(0, "kanbanAdmin", "admin@kanban.com", "", Role.ADMIN, "Kanban Admin", LocalDateTime.now()), "Pass123!@#");
        User mgr = uService.register(new User(0, "kanbanMgr", "mgr@kanban.com", "", Role.MANAGER, "Kanban Manager", LocalDateTime.now()), "Pass123!@#");
        User dev1 = uService.register(new User(0, "kanbanDev1", "dev1@kanban.com", "", Role.EMPLOYEE, "Dev One", LocalDateTime.now()), "Pass123!@#");
        User dev2 = uService.register(new User(0, "kanbanDev2", "dev2@kanban.com", "", Role.EMPLOYEE, "Dev Two", LocalDateTime.now()), "Pass123!@#");

        // 2. Setup Project
        UserSession.getInstance().startSession(mgr);
        Project project = new Project();
        project.setName("Kanban Project");
        project.setManagerId(mgr.getId());
        project.setStartDate(today.minusDays(5));
        project.setDeadline(today.plusDays(30));
        project.setStatus(ProjectStatus.ACTIVE);
        project = pService.createProject(project);

        // 3. Create a Task in TO_DO assigned to dev1
        Task task1 = new Task();
        task1.setProjectId(project.getId());
        task1.setName("Feature Auth");
        task1.setStatus(TaskStatus.TO_DO);
        task1.setPriority(TaskPriority.HIGH);
        task1.setAssignedEmployeeId(dev1.getId());
        task1.setDeadline(today.plusDays(10));
        task1 = tService.createTask(task1);

        // 4. Test Invalid Transitions from TO_DO
        UserSession.getInstance().startSession(dev1);
        final int taskId = task1.getId();
        // TO_DO -> TESTING is invalid
        ValidationException ex1 = assertThrows(ValidationException.class, () -> tService.updateTaskStatus(taskId, TaskStatus.TESTING));
        assertTrue(ex1.getMessage().contains("must move to IN_PROGRESS"));

        // TO_DO -> COMPLETED is invalid
        ValidationException ex2 = assertThrows(ValidationException.class, () -> tService.updateTaskStatus(taskId, TaskStatus.COMPLETED));
        assertTrue(ex2.getMessage().contains("must move to IN_PROGRESS"));

        // 5. Test Valid Transition: TO_DO -> IN_PROGRESS
        tService.updateTaskStatus(taskId, TaskStatus.IN_PROGRESS);
        Task updatedT1 = tService.getTaskById(taskId).orElseThrow();
        assertEquals(TaskStatus.IN_PROGRESS, updatedT1.getStatus());

        // Verify Manager received a notification from Employee transition
        List<Notification> mgrNotifs = nDAO.findByUserId(mgr.getId());
        assertTrue(mgrNotifs.stream().anyMatch(n -> n.getMessage().contains("Feature Auth") && n.getMessage().contains("IN_PROGRESS")));

        // Verify ActivityLog was created
        List<ActivityLog> logs = lDAO.findAll();
        assertTrue(logs.stream().anyMatch(l -> "TASK_STATUS_CHANGE".equals(l.getAction()) && l.getDescription().contains("Feature Auth moved to IN_PROGRESS")));

        // 6. Test Invalid Transition from IN_PROGRESS: IN_PROGRESS -> COMPLETED
        assertThrows(ValidationException.class, () -> tService.updateTaskStatus(taskId, TaskStatus.COMPLETED));

        // 7. Test Valid Transition: IN_PROGRESS -> TESTING
        tService.updateTaskStatus(taskId, TaskStatus.TESTING);
        Task inTesting = tService.getTaskById(taskId).orElseThrow();
        assertEquals(TaskStatus.TESTING, inTesting.getStatus());

        // 8. Test Testing Failure (Rework): TESTING -> IN_PROGRESS
        tService.updateTaskStatus(taskId, TaskStatus.IN_PROGRESS);
        Task reworked = tService.getTaskById(taskId).orElseThrow();
        assertEquals(TaskStatus.IN_PROGRESS, reworked.getStatus());

        // Send back to testing
        tService.updateTaskStatus(taskId, TaskStatus.TESTING);

        // 9. Test Valid Transition: TESTING -> COMPLETED (Pass testing)
        tService.updateTaskStatus(taskId, TaskStatus.COMPLETED);
        Task completedTask = tService.getTaskById(taskId).orElseThrow();
        assertEquals(TaskStatus.COMPLETED, completedTask.getStatus());

        // 10. Test Reopening Restrictions: Employee cannot reopen completed task
        assertThrows(ValidationException.class, () -> tService.updateTaskStatus(taskId, TaskStatus.IN_PROGRESS));

        // 11. Test Manager CAN reopen completed task
        UserSession.getInstance().startSession(mgr);
        tService.updateTaskStatus(taskId, TaskStatus.IN_PROGRESS);
        Task reopenedTask = tService.getTaskById(taskId).orElseThrow();
        assertEquals(TaskStatus.IN_PROGRESS, reopenedTask.getStatus());

        // Verify Assigned Employee received notification that Manager reopened task
        List<Notification> dev1Notifs = nDAO.findByUserId(dev1.getId());
        assertTrue(dev1Notifs.stream().anyMatch(n -> n.getMessage().contains("Feature Auth") && n.getMessage().contains("IN_PROGRESS")));

        // 12. Test Blocked Status: Any state can transition to/from BLOCKED
        tService.updateTaskStatus(taskId, TaskStatus.BLOCKED);
        assertEquals(TaskStatus.BLOCKED, tService.getTaskById(taskId).orElseThrow().getStatus());
        tService.updateTaskStatus(taskId, TaskStatus.IN_PROGRESS);
        assertEquals(TaskStatus.IN_PROGRESS, tService.getTaskById(taskId).orElseThrow().getStatus());

        // 13. Test Employee Authorization: Dev2 cannot move Dev1's task
        UserSession.getInstance().startSession(dev2);
        assertThrows(UnauthorizedException.class, () -> tService.updateTaskStatus(taskId, TaskStatus.TESTING));

        // 14. Priority & Deadline Ordering Preservation in Kanban Columns
        UserSession.getInstance().startSession(mgr);
        Task lowTask = new Task(0, project.getId(), "Low Task", "", dev1.getId(), TaskPriority.LOW, today.plusDays(2), TaskStatus.TO_DO, LocalDateTime.now(), LocalDateTime.now());
        Task critTaskLate = new Task(0, project.getId(), "Critical Late", "", dev1.getId(), TaskPriority.CRITICAL, today.plusDays(10), TaskStatus.TO_DO, LocalDateTime.now(), LocalDateTime.now());
        Task critTaskEarly = new Task(0, project.getId(), "Critical Early", "", dev1.getId(), TaskPriority.CRITICAL, today.plusDays(3), TaskStatus.TO_DO, LocalDateTime.now(), LocalDateTime.now());
        Task highTaskNoDeadline = new Task(0, project.getId(), "High No DL", "", dev1.getId(), TaskPriority.HIGH, null, TaskStatus.TO_DO, LocalDateTime.now(), LocalDateTime.now());

        tService.createTask(lowTask);
        tService.createTask(critTaskLate);
        tService.createTask(critTaskEarly);
        tService.createTask(highTaskNoDeadline);

        List<Task> todoTasks = tService.getAllTasks().stream()
                .filter(t -> t.getStatus() == TaskStatus.TO_DO)
                .sorted(com.intelliflow.util.TaskSorter.RECOMMENDED_COMPARATOR)
                .toList();

        assertEquals(4, todoTasks.size());
        assertEquals("Critical Early", todoTasks.get(0).getName());
        assertEquals("Critical Late", todoTasks.get(1).getName());
        assertEquals("High No DL", todoTasks.get(2).getName());
        assertEquals("Low Task", todoTasks.get(3).getName());
    }
}


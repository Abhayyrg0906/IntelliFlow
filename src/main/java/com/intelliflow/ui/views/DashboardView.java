package com.intelliflow.ui.views;

import com.intelliflow.context.UserSession;
import com.intelliflow.enums.Role;
import com.intelliflow.enums.TaskStatus;
import com.intelliflow.enums.ProjectStatus;
import com.intelliflow.model.*;
import com.intelliflow.ui.MainFrame;
import com.intelliflow.ui.ThemeManager;
import com.intelliflow.ui.components.*;
import com.intelliflow.exception.DatabaseException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DashboardView extends BaseView {
    private final MainFrame mainFrame;
    private final JPanel roleCardsPanel;
    private final CardLayout cardLayout;

    // --- Admin UI Elements ---
    private JPanel adminPanel;
    private JLabel adminWelcomeLabel;
    private JLabel adminSubtitleLabel;
    private DashboardCard adminCardUsers;
    private DashboardCard adminCardManagers;
    private DashboardCard adminCardEmployees;
    private DashboardCard adminCardProjects;
    private DashboardCard adminCardActiveTasks;
    private DashboardCard adminCardCompletedTasks;
    private DefaultTableModel adminProjectsModel;
    private DefaultTableModel adminTasksModel;
    private DefaultTableModel adminUsersModel;
    private DefaultTableModel adminLogsModel;

    // --- Manager UI Elements ---
    private JPanel managerPanel;
    private JLabel managerWelcomeLabel;
    private JLabel managerSubtitleLabel;
    private DashboardCard managerCardProjects;
    private DashboardCard managerCardActiveProjects;
    private DashboardCard managerCardTasks;
    private DashboardCard managerCardCompletedTasks;
    private DefaultTableModel managerProjectsModel;
    private DefaultTableModel managerTasksModel;
    private DefaultTableModel managerTeamModel;
    
    // Manager Progress Bars
    private JProgressBar managerTodoBar;
    private JProgressBar managerProgressBar;
    private JProgressBar managerTestingBar;
    private JProgressBar managerCompletedBar;
    private JProgressBar managerBlockedBar;
    private JLabel managerTodoVal;
    private JLabel managerProgressVal;
    private JLabel managerTestingVal;
    private JLabel managerCompletedVal;
    private JLabel managerBlockedVal;

    // --- Employee UI Elements ---
    private JPanel employeePanel;
    private JLabel employeeWelcomeLabel;
    private JLabel employeeSubtitleLabel;
    private DashboardCard employeeCardAssigned;
    private DashboardCard employeeCardTodo;
    private DashboardCard employeeCardProgress;
    private DashboardCard employeeCardCompleted;
    private DefaultTableModel employeeActiveModel;
    private DefaultTableModel employeeUpcomingModel;
    private DefaultTableModel employeeCompletedModel;

    public DashboardView(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(ThemeManager.COLOR_BACKGROUND);

        cardLayout = new CardLayout();
        roleCardsPanel = new JPanel(cardLayout);
        roleCardsPanel.setOpaque(false);

        // Pre-initialize role-specific dashboards
        initAdminPanel();
        initManagerPanel();
        initEmployeePanel();

        roleCardsPanel.add(adminPanel, "ADMIN");
        roleCardsPanel.add(managerPanel, "MANAGER");
        roleCardsPanel.add(employeePanel, "EMPLOYEE");

        // Transparent scroll wrapper for responsiveness
        JScrollPane mainScrollPane = new JScrollPane(roleCardsPanel);
        mainScrollPane.setBorder(BorderFactory.createEmptyBorder());
        mainScrollPane.setOpaque(false);
        mainScrollPane.getViewport().setOpaque(false);
        mainScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(mainScrollPane, BorderLayout.CENTER);
    }

    // ==========================================
    // ADMIN DASHBOARD SETUP
    // ==========================================
    private void initAdminPanel() {
        adminPanel = new JPanel(new BorderLayout(20, 20));
        adminPanel.setOpaque(false);
        adminPanel.setBorder(new EmptyBorder(25, 25, 25, 25));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        adminWelcomeLabel = new JLabel("Welcome back, Admin!");
        adminWelcomeLabel.setFont(ThemeManager.FONT_TITLE);
        adminWelcomeLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);

        adminSubtitleLabel = new JLabel("Here's an overview of your IntelliFlow workspace.");
        adminSubtitleLabel.setFont(ThemeManager.FONT_BODY);
        adminSubtitleLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 4, 4));
        textPanel.setOpaque(false);
        textPanel.add(adminWelcomeLabel);
        textPanel.add(adminSubtitleLabel);
        headerPanel.add(textPanel, BorderLayout.WEST);
        adminPanel.add(headerPanel, BorderLayout.NORTH);

        // Content
        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        // KPI Cards Grid (2 rows, 3 columns)
        JPanel kpiGrid = new JPanel(new GridLayout(2, 3, 20, 20));
        kpiGrid.setOpaque(false);
        kpiGrid.setPreferredSize(new Dimension(1000, 220));

        adminCardUsers = new DashboardCard("👥", "Total Users", "0", ThemeManager.COLOR_PRIMARY);
        adminCardManagers = new DashboardCard("💼", "Total Managers", "0", ThemeManager.COLOR_PRIMARY_HOVER);
        adminCardEmployees = new DashboardCard("👷", "Total Employees", "0", new Color(13, 148, 136));
        adminCardProjects = new DashboardCard("📁", "Total Projects", "0", new Color(245, 158, 11));
        adminCardActiveTasks = new DashboardCard("⏳", "Active Tasks", "0", ThemeManager.COLOR_WARNING);
        adminCardCompletedTasks = new DashboardCard("✓", "Completed Tasks", "0", ThemeManager.COLOR_SUCCESS);

        kpiGrid.add(adminCardUsers);
        kpiGrid.add(adminCardManagers);
        kpiGrid.add(adminCardEmployees);
        kpiGrid.add(adminCardProjects);
        kpiGrid.add(adminCardActiveTasks);
        kpiGrid.add(adminCardCompletedTasks);
        contentPanel.add(kpiGrid);

        contentPanel.add(Box.createVerticalStrut(25));

        // Sections Grid (2x2 tables)
        JPanel sectionsGrid = new JPanel(new GridLayout(2, 2, 20, 20));
        sectionsGrid.setOpaque(false);
        sectionsGrid.setPreferredSize(new Dimension(1000, 540));

        // Recent Projects
        adminProjectsModel = new DefaultTableModel(new Object[]{"Project Name", "Manager", "Deadline", "Status"}, 0);
        ModernTable projectsTable = new ModernTable();
        projectsTable.setModel(adminProjectsModel);
        projectsTable.setPlaceholderText("No projects available.");
        sectionsGrid.add(createTableCard("Recent Projects", projectsTable));

        // Recent Tasks
        adminTasksModel = new DefaultTableModel(new Object[]{"Task Name", "Project", "Assignee", "Priority", "Status"}, 0);
        ModernTable tasksTable = new ModernTable();
        tasksTable.setModel(adminTasksModel);
        tasksTable.setPlaceholderText("No tasks available.");
        sectionsGrid.add(createTableCard("Recent Tasks", tasksTable));

        // User Overview
        adminUsersModel = new DefaultTableModel(new Object[]{"Full Name", "Username", "Email", "Role"}, 0);
        ModernTable usersTable = new ModernTable();
        usersTable.setModel(adminUsersModel);
        usersTable.setPlaceholderText("No users registered.");
        sectionsGrid.add(createTableCard("User Overview", usersTable));

        // Recent Activity
        adminLogsModel = new DefaultTableModel(new Object[]{"User", "Action", "Description", "Timestamp"}, 0);
        ModernTable logsTable = new ModernTable();
        logsTable.setModel(adminLogsModel);
        logsTable.setPlaceholderText("No logs available.");
        sectionsGrid.add(createTableCard("Recent Activity Log", logsTable));

        contentPanel.add(sectionsGrid);
        adminPanel.add(contentPanel, BorderLayout.CENTER);
    }

    // ==========================================
    // MANAGER DASHBOARD SETUP
    // ==========================================
    private void initManagerPanel() {
        managerPanel = new JPanel(new BorderLayout(20, 20));
        managerPanel.setOpaque(false);
        managerPanel.setBorder(new EmptyBorder(25, 25, 25, 25));

        // Header Panel (Includes Quick Actions)
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        managerWelcomeLabel = new JLabel("Welcome back, Manager!");
        managerWelcomeLabel.setFont(ThemeManager.FONT_TITLE);
        managerWelcomeLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);

        managerSubtitleLabel = new JLabel("Here's your project management workspace overview.");
        managerSubtitleLabel.setFont(ThemeManager.FONT_BODY);
        managerSubtitleLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 4, 4));
        textPanel.setOpaque(false);
        textPanel.add(managerWelcomeLabel);
        textPanel.add(managerSubtitleLabel);
        headerPanel.add(textPanel, BorderLayout.WEST);

        // Quick Actions
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        actionsPanel.setOpaque(false);

        JButton btnCreateProj = new JButton("➕ Create Project");
        btnCreateProj.setBackground(ThemeManager.COLOR_PRIMARY);
        btnCreateProj.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        btnCreateProj.setFont(ThemeManager.FONT_BOLD_SMALL);
        btnCreateProj.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCreateProj.addActionListener(e -> {
            mainFrame.showView("projects");
            ProjectManagementView view = (ProjectManagementView) mainFrame.getView("projects");
            if (view != null) view.showProjectForm(null);
        });

        JButton btnCreateTask = new JButton("➕ Create Task");
        btnCreateTask.setBackground(ThemeManager.COLOR_CARD);
        btnCreateTask.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        btnCreateTask.setFont(ThemeManager.FONT_BOLD_SMALL);
        btnCreateTask.setBorder(BorderFactory.createLineBorder(ThemeManager.COLOR_BORDER, 1));
        btnCreateTask.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCreateTask.addActionListener(e -> {
            mainFrame.showView("tasks");
            TaskManagementView view = (TaskManagementView) mainFrame.getView("tasks");
            if (view != null) view.showTaskForm(null);
        });

        actionsPanel.add(btnCreateProj);
        actionsPanel.add(btnCreateTask);
        headerPanel.add(actionsPanel, BorderLayout.EAST);

        managerPanel.add(headerPanel, BorderLayout.NORTH);

        // Content
        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        // KPI Cards Grid (1 row, 4 columns)
        JPanel kpiGrid = new JPanel(new GridLayout(1, 4, 20, 0));
        kpiGrid.setOpaque(false);
        kpiGrid.setPreferredSize(new Dimension(1000, 110));

        managerCardProjects = new DashboardCard("📁", "Total Projects", "0", ThemeManager.COLOR_PRIMARY);
        managerCardActiveProjects = new DashboardCard("⏳", "Active Projects", "0", ThemeManager.COLOR_WARNING);
        managerCardTasks = new DashboardCard("📋", "Total Tasks", "0", ThemeManager.COLOR_PRIMARY_HOVER);
        managerCardCompletedTasks = new DashboardCard("✓", "Completed Tasks", "0", ThemeManager.COLOR_SUCCESS);

        kpiGrid.add(managerCardProjects);
        kpiGrid.add(managerCardActiveProjects);
        kpiGrid.add(managerCardTasks);
        kpiGrid.add(managerCardCompletedTasks);
        contentPanel.add(kpiGrid);

        contentPanel.add(Box.createVerticalStrut(25));

        // Sections Panel (2x2 grid)
        JPanel sectionsGrid = new JPanel(new GridLayout(2, 2, 20, 20));
        sectionsGrid.setOpaque(false);
        sectionsGrid.setPreferredSize(new Dimension(1000, 540));

        // Recent Projects
        managerProjectsModel = new DefaultTableModel(new Object[]{"Project Name", "Start Date", "Deadline", "Status"}, 0);
        ModernTable managerProjectsTable = new ModernTable();
        managerProjectsTable.setModel(managerProjectsModel);
        managerProjectsTable.setPlaceholderText("No projects managed.");
        sectionsGrid.add(createTableCard("Your Projects", managerProjectsTable));

        // Active Tasks
        managerTasksModel = new DefaultTableModel(new Object[]{"Task Name", "Project", "Assignee", "Priority", "Status"}, 0);
        ModernTable managerTasksTable = new ModernTable();
        managerTasksTable.setModel(managerTasksModel);
        managerTasksTable.setPlaceholderText("No active tasks found.");
        sectionsGrid.add(createTableCard("Team's Active Tasks", managerTasksTable));

        // Task Progress Charts card
        JPanel progressCard = new RoundedPanel(12, ThemeManager.COLOR_CARD);
        progressCard.setLayout(new BorderLayout(10, 10));
        progressCard.setBorder(new EmptyBorder(15, 18, 15, 18));
        progressCard.add(new SectionHeader("Task Status Progress"), BorderLayout.NORTH);

        JPanel barsPanel = new JPanel(new GridLayout(5, 1, 10, 10));
        barsPanel.setOpaque(false);

        managerTodoBar = createStyledProgressBar(ThemeManager.COLOR_TEXT_PRIMARY);
        managerProgressBar = createStyledProgressBar(new Color(59, 130, 246));
        managerTestingBar = createStyledProgressBar(new Color(245, 158, 11));
        managerCompletedBar = createStyledProgressBar(ThemeManager.COLOR_SUCCESS);
        managerBlockedBar = createStyledProgressBar(ThemeManager.COLOR_DANGER);

        managerTodoVal = new JLabel("0 (0%)"); managerTodoVal.setForeground(ThemeManager.COLOR_TEXT_MUTED);
        managerProgressVal = new JLabel("0 (0%)"); managerProgressVal.setForeground(ThemeManager.COLOR_TEXT_MUTED);
        managerTestingVal = new JLabel("0 (0%)"); managerTestingVal.setForeground(ThemeManager.COLOR_TEXT_MUTED);
        managerCompletedVal = new JLabel("0 (0%)"); managerCompletedVal.setForeground(ThemeManager.COLOR_TEXT_MUTED);
        managerBlockedVal = new JLabel("0 (0%)"); managerBlockedVal.setForeground(ThemeManager.COLOR_TEXT_MUTED);

        barsPanel.add(createProgressRow("To Do", managerTodoBar, managerTodoVal));
        barsPanel.add(createProgressRow("In Progress", managerProgressBar, managerProgressVal));
        barsPanel.add(createProgressRow("Testing", managerTestingBar, managerTestingVal));
        barsPanel.add(createProgressRow("Completed", managerCompletedBar, managerCompletedVal));
        barsPanel.add(createProgressRow("Blocked", managerBlockedBar, managerBlockedVal));
        progressCard.add(barsPanel, BorderLayout.CENTER);
        sectionsGrid.add(progressCard);

        // Team Overview
        managerTeamModel = new DefaultTableModel(new Object[]{"Employee Name", "Total Tasks", "Completed Tasks", "Workload"}, 0);
        ModernTable managerTeamTable = new ModernTable();
        managerTeamTable.setModel(managerTeamModel);
        managerTeamTable.setPlaceholderText("No team activity records.");
        sectionsGrid.add(createTableCard("Team Performance & Workload", managerTeamTable));

        contentPanel.add(sectionsGrid);
        managerPanel.add(contentPanel, BorderLayout.CENTER);
    }

    // ==========================================
    // EMPLOYEE DASHBOARD SETUP
    // ==========================================
    private void initEmployeePanel() {
        employeePanel = new JPanel(new BorderLayout(20, 20));
        employeePanel.setOpaque(false);
        employeePanel.setBorder(new EmptyBorder(25, 25, 25, 25));

        // Header Panel (Includes Quick Actions)
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        employeeWelcomeLabel = new JLabel("Welcome back, Employee!");
        employeeWelcomeLabel.setFont(ThemeManager.FONT_TITLE);
        employeeWelcomeLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);

        employeeSubtitleLabel = new JLabel("Here's your personal workflow productivity dashboard.");
        employeeSubtitleLabel.setFont(ThemeManager.FONT_BODY);
        employeeSubtitleLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 4, 4));
        textPanel.setOpaque(false);
        textPanel.add(employeeWelcomeLabel);
        textPanel.add(employeeSubtitleLabel);
        headerPanel.add(textPanel, BorderLayout.WEST);

        // Quick Actions
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        actionsPanel.setOpaque(false);

        JButton btnGoTasks = new JButton("📋 Go to Task Board");
        btnGoTasks.setBackground(ThemeManager.COLOR_PRIMARY);
        btnGoTasks.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        btnGoTasks.setFont(ThemeManager.FONT_BOLD_SMALL);
        btnGoTasks.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnGoTasks.addActionListener(e -> mainFrame.showView("tasks"));

        JButton btnViewProj = new JButton("📂 View Projects");
        btnViewProj.setBackground(ThemeManager.COLOR_CARD);
        btnViewProj.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        btnViewProj.setFont(ThemeManager.FONT_BOLD_SMALL);
        btnViewProj.setBorder(BorderFactory.createLineBorder(ThemeManager.COLOR_BORDER, 1));
        btnViewProj.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnViewProj.addActionListener(e -> mainFrame.showView("projects"));

        JButton btnProfile = new JButton("👤 Edit Profile");
        btnProfile.setBackground(ThemeManager.COLOR_CARD);
        btnProfile.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        btnProfile.setFont(ThemeManager.FONT_BOLD_SMALL);
        btnProfile.setBorder(BorderFactory.createLineBorder(ThemeManager.COLOR_BORDER, 1));
        btnProfile.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnProfile.addActionListener(e -> mainFrame.showView("profile"));

        actionsPanel.add(btnGoTasks);
        actionsPanel.add(btnViewProj);
        actionsPanel.add(btnProfile);
        headerPanel.add(actionsPanel, BorderLayout.EAST);

        employeePanel.add(headerPanel, BorderLayout.NORTH);

        // Content
        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        // KPI Cards Grid (1 row, 4 columns)
        JPanel kpiGrid = new JPanel(new GridLayout(1, 4, 20, 0));
        kpiGrid.setOpaque(false);
        kpiGrid.setPreferredSize(new Dimension(1000, 110));

        employeeCardAssigned = new DashboardCard("📋", "Assigned Tasks", "0", ThemeManager.COLOR_PRIMARY);
        employeeCardTodo = new DashboardCard("⏳", "To Do Tasks", "0", ThemeManager.COLOR_WARNING);
        employeeCardProgress = new DashboardCard("⚡ In Progress", "0", "0", ThemeManager.COLOR_PRIMARY_HOVER);
        employeeCardCompleted = new DashboardCard("✓", "Completed Tasks", "0", ThemeManager.COLOR_SUCCESS);

        kpiGrid.add(employeeCardAssigned);
        kpiGrid.add(employeeCardTodo);
        kpiGrid.add(employeeCardProgress);
        kpiGrid.add(employeeCardCompleted);
        contentPanel.add(kpiGrid);

        contentPanel.add(Box.createVerticalStrut(25));

        // Sections Panel (1 row, 3 columns)
        JPanel sectionsGrid = new JPanel(new GridLayout(1, 3, 20, 0));
        sectionsGrid.setOpaque(false);
        sectionsGrid.setPreferredSize(new Dimension(1000, 540));

        // Active Tasks
        employeeActiveModel = new DefaultTableModel(new Object[]{"Task Name", "Project", "Priority", "Deadline", "Status"}, 0);
        ModernTable activeTasksTable = new ModernTable();
        activeTasksTable.setModel(employeeActiveModel);
        activeTasksTable.setPlaceholderText("No active tasks assigned.");
        sectionsGrid.add(createTableCard("My Active Tasks", activeTasksTable));

        // Upcoming Tasks
        employeeUpcomingModel = new DefaultTableModel(new Object[]{"Task Name", "Project", "Deadline", "Status"}, 0);
        ModernTable upcomingTable = new ModernTable();
        upcomingTable.setModel(employeeUpcomingModel);
        upcomingTable.setPlaceholderText("No upcoming deadlines.");
        sectionsGrid.add(createTableCard("Upcoming Priorities", upcomingTable));

        // Recent Completed
        employeeCompletedModel = new DefaultTableModel(new Object[]{"Task Name", "Project", "Deadline", "Status"}, 0);
        ModernTable completedTable = new ModernTable();
        completedTable.setModel(employeeCompletedModel);
        completedTable.setPlaceholderText("No tasks completed yet.");
        sectionsGrid.add(createTableCard("Recently Completed", completedTable));

        contentPanel.add(sectionsGrid);
        employeePanel.add(contentPanel, BorderLayout.CENTER);
    }

    // ==========================================
    // REUSABLE HELPER CREATION METHODS
    // ==========================================
    private JPanel createTableCard(String title, JTable table) {
        RoundedPanel card = new RoundedPanel(12, ThemeManager.COLOR_CARD);
        card.setDrawBorder(true);
        card.setBorderColor(ThemeManager.COLOR_BORDER);
        card.setLayout(new BorderLayout(10, 10));
        card.setBorder(new EmptyBorder(15, 18, 15, 18));

        card.add(new SectionHeader(title), BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        card.add(scrollPane, BorderLayout.CENTER);

        return card;
    }

    private JProgressBar createStyledProgressBar(Color progressColor) {
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setForeground(progressColor);
        bar.setBackground(ThemeManager.COLOR_BORDER);
        bar.setPreferredSize(new Dimension(100, 16));
        return bar;
    }

    private JPanel createProgressRow(String labelText, JProgressBar progressBar, JLabel valLabel) {
        JPanel row = new JPanel(new BorderLayout(15, 0));
        row.setOpaque(false);

        JLabel statusLbl = new JLabel(labelText);
        statusLbl.setFont(ThemeManager.FONT_BOLD_SMALL);
        statusLbl.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        statusLbl.setPreferredSize(new Dimension(100, 20));

        row.add(statusLbl, BorderLayout.WEST);
        row.add(progressBar, BorderLayout.CENTER);

        valLabel.setPreferredSize(new Dimension(80, 20));
        valLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        row.add(valLabel, BorderLayout.EAST);

        return row;
    }

    // ==========================================
    // REFRESH & DATABASE STATS PIPELINE
    // ==========================================
    @Override
    public void refresh() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // Show matching Dashboard View Card
        Role role = currentUser.getRole();
        cardLayout.show(roleCardsPanel, role.name());

        // Update welcome texts
        String welcomeText = "Welcome back, " + currentUser.getFullName() + "!";
        if (role == Role.ADMIN) {
            adminWelcomeLabel.setText(welcomeText);
        } else if (role == Role.MANAGER) {
            managerWelcomeLabel.setText(welcomeText);
        } else {
            employeeWelcomeLabel.setText(welcomeText);
        }

        // Run SwingWorker background loader for database queries
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            // Shared caches
            private final Map<Integer, String> userNames = new HashMap<>();
            private final Map<Integer, String> projectNames = new HashMap<>();

            // Admin data
            private int adminUsers, adminManagers, adminEmployees, adminProjects, adminActiveTasks, adminCompletedTasks;
            private List<String[]> adminProjRows, adminTaskRows, adminUserRows, adminLogRows;

            // Manager data
            private int mgrProjects, mgrActiveProjects, mgrTasks, mgrCompletedTasks;
            private int mgrTodo, mgrProgress, mgrTesting, mgrCompleted, mgrBlocked;
            private List<String[]> mgrProjRows, mgrTaskRows, mgrTeamRows;

            // Employee data
            private int empAssigned, empTodo, empProgress, empCompleted;
            private List<String[]> empActiveRows, empUpcomingRows, empCompletedRows;

            @Override
            protected Void doInBackground() throws Exception {
                // Pre-fetch caches for mappings
                List<User> allUsers = mainFrame.getUserService().getAllUsers();
                for (User u : allUsers) {
                    userNames.put(u.getId(), u.getFullName());
                }

                List<Project> allProjects = mainFrame.getProjectService().getAllProjects();
                for (Project p : allProjects) {
                    projectNames.put(p.getId(), p.getName());
                }

                if (role == Role.ADMIN) {
                    // KPI Calculations
                    adminUsers = allUsers.size();
                    adminManagers = (int) allUsers.stream().filter(u -> u.getRole() == Role.MANAGER).count();
                    adminEmployees = (int) allUsers.stream().filter(u -> u.getRole() == Role.EMPLOYEE).count();
                    
                    adminProjects = allProjects.size();
                    
                    List<Task> allTasks = mainFrame.getTaskService().getAllTasks();
                    adminActiveTasks = (int) allTasks.stream().filter(t -> t.getStatus() != TaskStatus.COMPLETED).count();
                    adminCompletedTasks = (int) allTasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();

                    // Table Rows
                    adminProjRows = allProjects.stream().limit(5).map(p -> new String[]{
                            p.getName(),
                            p.getManagerId() != null ? userNames.getOrDefault(p.getManagerId(), "Unknown") : "Unassigned",
                            p.getDeadline().toString(),
                            p.getStatus().name()
                    }).collect(Collectors.toList());

                    adminTaskRows = allTasks.stream().limit(5).map(t -> new String[]{
                            t.getName(),
                            projectNames.getOrDefault(t.getProjectId(), "Unknown"),
                            t.getAssignedEmployeeId() != null ? userNames.getOrDefault(t.getAssignedEmployeeId(), "Unknown") : "Unassigned",
                            t.getPriority().name(),
                            t.getStatus().name()
                    }).collect(Collectors.toList());

                    adminUserRows = allUsers.stream().limit(5).map(u -> new String[]{
                            u.getFullName(),
                            u.getUsername(),
                            u.getEmail(),
                            u.getRole().name()
                    }).collect(Collectors.toList());

                    List<ActivityLog> logs = mainFrame.getUserService().getActivityLogs();
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    adminLogRows = logs.stream().limit(5).map(l -> new String[]{
                            l.getUserId() != null ? userNames.getOrDefault(l.getUserId(), "System") : "System",
                            l.getAction(),
                            l.getDescription(),
                            l.getTimestamp().format(dtf)
                    }).collect(Collectors.toList());

                } else if (role == Role.MANAGER) {
                    List<Project> managed = mainFrame.getProjectService().getProjectsManagedBy(currentUser.getId());
                    List<Integer> managedIds = managed.stream().map(Project::getId).collect(Collectors.toList());
                    
                    List<Task> allTasks = mainFrame.getTaskService().getAllTasks();
                    List<Task> teamTasks = allTasks.stream().filter(t -> managedIds.contains(t.getProjectId())).collect(Collectors.toList());

                    mgrProjects = managed.size();
                    mgrActiveProjects = (int) managed.stream().filter(p -> p.getStatus() == ProjectStatus.ACTIVE).count();
                    mgrTasks = teamTasks.size();
                    mgrCompletedTasks = (int) teamTasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();

                    // Status Breakdown
                    for (Task t : teamTasks) {
                        switch (t.getStatus()) {
                            case TO_DO -> mgrTodo++;
                            case IN_PROGRESS -> mgrProgress++;
                            case TESTING -> mgrTesting++;
                            case COMPLETED -> mgrCompleted++;
                            case BLOCKED -> mgrBlocked++;
                        }
                    }

                    // Table Rows
                    mgrProjRows = managed.stream().limit(5).map(p -> new String[]{
                            p.getName(),
                            p.getStartDate().toString(),
                            p.getDeadline().toString(),
                            p.getStatus().name()
                    }).collect(Collectors.toList());

                    mgrTaskRows = teamTasks.stream()
                            .filter(t -> t.getStatus() != TaskStatus.COMPLETED)
                            .limit(5)
                            .map(t -> new String[]{
                                    t.getName(),
                                    projectNames.getOrDefault(t.getProjectId(), "Unknown"),
                                    t.getAssignedEmployeeId() != null ? userNames.getOrDefault(t.getAssignedEmployeeId(), "Unknown") : "Unassigned",
                                    t.getPriority().name(),
                                    t.getStatus().name()
                            }).collect(Collectors.toList());

                    // Employee Workloads
                    List<EmployeePerformanceReport> perf = mainFrame.getReportService().getEmployeePerformanceReports();
                    mgrTeamRows = perf.stream()
                            .filter(r -> r.getTotalTasks() > 0) // Filter only employees with assignments
                            .limit(5)
                            .map(r -> new String[]{
                                    r.getEmployeeName(),
                                    String.valueOf(r.getTotalTasks()),
                                    String.valueOf(r.getCompletedTasks()),
                                    r.getCompletionRate() + "%"
                            }).collect(Collectors.toList());

                } else { // Role.EMPLOYEE
                    List<Task> assigned = mainFrame.getTaskService().getTasksByEmployee(currentUser.getId());
                    empAssigned = assigned.size();
                    empTodo = (int) assigned.stream().filter(t -> t.getStatus() == TaskStatus.TO_DO).count();
                    empProgress = (int) assigned.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
                    empCompleted = (int) assigned.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();

                    LocalDate today = LocalDate.now();

                    // My Active
                    empActiveRows = assigned.stream()
                            .filter(t -> t.getStatus() != TaskStatus.COMPLETED)
                            .limit(5)
                            .map(t -> new String[]{
                                    t.getName(),
                                    projectNames.getOrDefault(t.getProjectId(), "Unknown"),
                                    t.getPriority().name(),
                                    t.getDeadline().toString(),
                                    t.getStatus().name()
                            }).collect(Collectors.toList());

                    // Upcoming Priorities (due in next 7 days, sorted by deadline)
                    empUpcomingRows = assigned.stream()
                            .filter(t -> t.getStatus() != TaskStatus.COMPLETED)
                            .sorted((t1, t2) -> t1.getDeadline().compareTo(t2.getDeadline()))
                            .limit(5)
                            .map(t -> new String[]{
                                    t.getName(),
                                    projectNames.getOrDefault(t.getProjectId(), "Unknown"),
                                    t.getDeadline().toString(),
                                    t.getStatus().name()
                            }).collect(Collectors.toList());

                    // Recent Completed
                    empCompletedRows = assigned.stream()
                            .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                            .limit(5)
                            .map(t -> new String[]{
                                    t.getName(),
                                    projectNames.getOrDefault(t.getProjectId(), "Unknown"),
                                    t.getDeadline().toString(),
                                    t.getStatus().name()
                            }).collect(Collectors.toList());
                }

                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Throws execution exceptions if query failed

                    if (role == Role.ADMIN) {
                        // Admin KPIs
                        adminCardUsers.setValue(String.valueOf(adminUsers));
                        adminCardManagers.setValue(String.valueOf(adminManagers));
                        adminCardEmployees.setValue(String.valueOf(adminEmployees));
                        adminCardProjects.setValue(String.valueOf(adminProjects));
                        adminCardActiveTasks.setValue(String.valueOf(adminActiveTasks));
                        adminCardCompletedTasks.setValue(String.valueOf(adminCompletedTasks));

                        // Admin Tables
                        populateTable(adminProjectsModel, adminProjRows);
                        populateTable(adminTasksModel, adminTaskRows);
                        populateTable(adminUsersModel, adminUserRows);
                        populateTable(adminLogsModel, adminLogRows);

                    } else if (role == Role.MANAGER) {
                        // Manager KPIs
                        managerCardProjects.setValue(String.valueOf(mgrProjects));
                        managerCardActiveProjects.setValue(String.valueOf(mgrActiveProjects));
                        managerCardTasks.setValue(String.valueOf(mgrTasks));
                        managerCardCompletedTasks.setValue(String.valueOf(mgrCompletedTasks));

                        // Manager Tables
                        populateTable(managerProjectsModel, mgrProjRows);
                        populateTable(managerTasksModel, mgrTaskRows);
                        populateTable(managerTeamModel, mgrTeamRows);

                        // Manager Progress Bars
                        updateProgressBar(managerTodoBar, managerTodoVal, mgrTodo, mgrTasks);
                        updateProgressBar(managerProgressBar, managerProgressVal, mgrProgress, mgrTasks);
                        updateProgressBar(managerTestingBar, managerTestingVal, mgrTesting, mgrTasks);
                        updateProgressBar(managerCompletedBar, managerCompletedVal, mgrCompleted, mgrTasks);
                        updateProgressBar(managerBlockedBar, managerBlockedVal, mgrBlocked, mgrTasks);

                    } else { // EMPLOYEE
                        // Employee KPIs
                        employeeCardAssigned.setValue(String.valueOf(empAssigned));
                        employeeCardTodo.setValue(String.valueOf(empTodo));
                        employeeCardProgress.setLabel("In Progress");
                        employeeCardProgress.setValue(String.valueOf(empProgress));
                        employeeCardCompleted.setValue(String.valueOf(empCompleted));

                        // Employee Tables
                        populateTable(employeeActiveModel, empActiveRows);
                        populateTable(employeeUpcomingModel, empUpcomingRows);
                        populateTable(employeeCompletedModel, empCompletedRows);
                    }

                } catch (Exception e) {
                    System.err.println("Dashboard refresh query calculations failed: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    private void populateTable(DefaultTableModel model, List<String[]> rows) {
        model.setRowCount(0);
        if (rows != null) {
            for (String[] row : rows) {
                model.addRow(row);
            }
        }
    }

    private void updateProgressBar(JProgressBar bar, JLabel valLabel, int value, int total) {
        int percent = total > 0 ? (int) Math.round(((double) value / total) * 100) : 0;
        bar.setValue(percent);
        valLabel.setText(value + " (" + percent + "%)");
    }
}

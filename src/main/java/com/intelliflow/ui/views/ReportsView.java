package com.intelliflow.ui.views;

import com.intelliflow.context.UserSession;
import com.intelliflow.enums.Role;
import com.intelliflow.enums.ProjectStatus;
import com.intelliflow.enums.TaskStatus;
import com.intelliflow.enums.TaskPriority;
import com.intelliflow.exception.ValidationException;
import com.intelliflow.model.*;
import com.intelliflow.ui.MainFrame;
import com.intelliflow.ui.ThemeManager;
import com.intelliflow.ui.components.ModernTable;
import com.intelliflow.ui.components.RoundedPanel;
import com.intelliflow.ui.components.DashboardCard;
import com.intelliflow.util.CSVExporter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ReportsView extends BaseView {
    private final MainFrame mainFrame;

    private JPanel contentPanel;
    private JScrollPane scrollPane;

    // --- Project Audit Dashboard Components ---
    private JComboBox<ProjectItem> projectCombo;
    private JButton exportButton;
    private JLabel totalTasksVal;
    private JLabel completedTasksVal;
    private JLabel pendingTasksVal;
    private JLabel overdueTasksVal;
    private JLabel blockedTasksVal;
    private JProgressBar completionProgressBar;
    private JLabel completionPercentageLabel;
    private ModernTable reportTaskTable;
    private DefaultTableModel tableModel;

    // --- State Cache ---
    private List<Project> projectsList = new ArrayList<>();
    private List<Task> currentProjectTasks = new ArrayList<>();
    private ProjectProgressReport currentReport = null;
    private List<User> allUsersList = new ArrayList<>();
    private List<Task> allTasksList = new ArrayList<>();

    private static class ProjectItem {
        int id;
        String name;

        ProjectItem(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public ReportsView(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(ThemeManager.COLOR_BACKGROUND);

        contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) return;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private List<Project> projects = new ArrayList<>();
            private List<Task> tasks = new ArrayList<>();
            private List<User> users = new ArrayList<>();

            @Override
            protected Void doInBackground() throws Exception {
                users = mainFrame.getUserService().getAllUsers();

                if (currentUser.getRole() == Role.ADMIN) {
                    projects = mainFrame.getProjectService().getAllProjects();
                    tasks = mainFrame.getTaskService().getAllTasks();
                } else if (currentUser.getRole() == Role.MANAGER) {
                    projects = mainFrame.getProjectService().getProjectsManagedBy(currentUser.getId());
                    Set<Integer> managedProjectIds = projects.stream().map(Project::getId).collect(Collectors.toSet());
                    List<Task> allTasks = mainFrame.getTaskService().getAllTasks();
                    tasks = allTasks.stream()
                            .filter(t -> managedProjectIds.contains(t.getProjectId()))
                            .collect(Collectors.toList());
                } else {
                    projects = mainFrame.getProjectService().getAllProjects();
                    tasks = mainFrame.getTaskService().getTasksByEmployee(currentUser.getId());
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    allUsersList = users;
                    projectsList = projects;
                    allTasksList = tasks;

                    contentPanel.removeAll();

                    if (currentUser.getRole() == Role.ADMIN) {
                        buildAdminReports(tasks, projects, users);
                    } else if (currentUser.getRole() == Role.MANAGER) {
                        buildManagerReports(tasks, projects, users);
                    } else {
                        buildEmployeeReports(tasks);
                    }

                    contentPanel.revalidate();
                    contentPanel.repaint();
                } catch (Exception e) {
                    System.err.println("Failed to build analytics dashboard: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    // --- Builder Panel Methods ---

    private void buildAdminReports(List<Task> tasks, List<Project> projects, List<User> users) {
        // Title block
        addHeaderTitle("System Dashboard", "Global system metrics, user roles profile, and tasks status overview");

        // KPI Summary cards
        JPanel kpiGrid = new JPanel(new GridLayout(1, 3, 20, 0));
        kpiGrid.setOpaque(false);
        kpiGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        kpiGrid.add(new DashboardCard("👥", "Total Users", String.valueOf(users.size()), ThemeManager.COLOR_PRIMARY));
        kpiGrid.add(new DashboardCard("📁", "Total Projects", String.valueOf(projects.size()), ThemeManager.COLOR_WARNING));
        kpiGrid.add(new DashboardCard("📋", "Total Tasks", String.valueOf(tasks.size()), ThemeManager.COLOR_PRIMARY_HOVER));
        contentPanel.add(kpiGrid);
        contentPanel.add(Box.createVerticalStrut(25));

        // Distribution breakdowns
        JPanel distribPanel = new JPanel(new GridLayout(1, 2, 25, 0));
        distribPanel.setOpaque(false);
        distribPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 380));

        // 1. Users by Role
        JPanel userBreakdownPanel = new RoundedPanel(12, ThemeManager.COLOR_CARD);
        userBreakdownPanel.setLayout(new BorderLayout());
        userBreakdownPanel.setBorder(new EmptyBorder(18, 20, 18, 20));

        JLabel userTitle = new JLabel("User Accounts Distribution");
        userTitle.setFont(ThemeManager.FONT_SUBTITLE);
        userTitle.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        userTitle.setBorder(new EmptyBorder(0, 0, 15, 0));
        userBreakdownPanel.add(userTitle, BorderLayout.NORTH);

        JPanel userRows = new JPanel();
        userRows.setOpaque(false);
        userRows.setLayout(new BoxLayout(userRows, BoxLayout.Y_AXIS));

        long adminCount = users.stream().filter(u -> u.getRole() == Role.ADMIN).count();
        long managerCount = users.stream().filter(u -> u.getRole() == Role.MANAGER).count();
        long employeeCount = users.stream().filter(u -> u.getRole() == Role.EMPLOYEE).count();
        int totalUsers = users.size();

        userRows.add(createBreakdownRow("🛡️ ADMINS", (int) adminCount, totalUsers, ThemeManager.COLOR_PRIMARY));
        userRows.add(createBreakdownRow("👤 MANAGERS", (int) managerCount, totalUsers, ThemeManager.COLOR_PRIMARY_HOVER));
        userRows.add(createBreakdownRow("👷 EMPLOYEES", (int) employeeCount, totalUsers, ThemeManager.COLOR_SUCCESS));

        userBreakdownPanel.add(userRows, BorderLayout.CENTER);
        distribPanel.add(userBreakdownPanel);

        // 2. Global Task Breakdown
        JPanel taskBreakdownPanel = new RoundedPanel(12, ThemeManager.COLOR_CARD);
        taskBreakdownPanel.setLayout(new GridLayout(2, 1, 0, 15));
        taskBreakdownPanel.setBorder(new EmptyBorder(18, 20, 18, 20));

        // Statuses
        JPanel statusBreakdown = new JPanel();
        statusBreakdown.setOpaque(false);
        statusBreakdown.setLayout(new BoxLayout(statusBreakdown, BoxLayout.Y_AXIS));

        JLabel statusTitle = new JLabel("Tasks by Status");
        statusTitle.setFont(ThemeManager.FONT_BOLD_SMALL);
        statusTitle.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        statusBreakdown.add(statusTitle);

        Map<TaskStatus, Long> statusCounts = tasks.stream().collect(Collectors.groupingBy(Task::getStatus, Collectors.counting()));
        int totalTasks = tasks.size();

        statusBreakdown.add(createBreakdownRow("📋 TO DO", statusCounts.getOrDefault(TaskStatus.TO_DO, 0L).intValue(), totalTasks, new Color(148, 163, 184)));
        statusBreakdown.add(createBreakdownRow("⚡ IN PROGRESS", statusCounts.getOrDefault(TaskStatus.IN_PROGRESS, 0L).intValue(), totalTasks, new Color(59, 130, 246)));
        statusBreakdown.add(createBreakdownRow("✓ COMPLETED", statusCounts.getOrDefault(TaskStatus.COMPLETED, 0L).intValue(), totalTasks, ThemeManager.COLOR_SUCCESS));

        // Priorities
        JPanel priorityBreakdown = new JPanel();
        priorityBreakdown.setOpaque(false);
        priorityBreakdown.setLayout(new BoxLayout(priorityBreakdown, BoxLayout.Y_AXIS));

        JLabel priorityTitle = new JLabel("Tasks by Priority");
        priorityTitle.setFont(ThemeManager.FONT_BOLD_SMALL);
        priorityTitle.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        priorityBreakdown.add(priorityTitle);

        Map<TaskPriority, Long> priorityCounts = tasks.stream().collect(Collectors.groupingBy(Task::getPriority, Collectors.counting()));

        priorityBreakdown.add(createBreakdownRow("🔴 CRITICAL", priorityCounts.getOrDefault(TaskPriority.CRITICAL, 0L).intValue(), totalTasks, ThemeManager.COLOR_DANGER));
        priorityBreakdown.add(createBreakdownRow("🟠 HIGH", priorityCounts.getOrDefault(TaskPriority.HIGH, 0L).intValue(), totalTasks, ThemeManager.COLOR_WARNING));
        priorityBreakdown.add(createBreakdownRow("🔵 MEDIUM", priorityCounts.getOrDefault(TaskPriority.MEDIUM, 0L).intValue(), totalTasks, new Color(79, 70, 229)));

        taskBreakdownPanel.add(statusBreakdown);
        taskBreakdownPanel.add(priorityBreakdown);
        distribPanel.add(taskBreakdownPanel);

        contentPanel.add(distribPanel);
    }

    private void buildManagerReports(List<Task> tasks, List<Project> projects, List<User> users) {
        addHeaderTitle("Project & Team Performance Analytics", "Performance metrics for your managed projects and team tasks");

        // KPI Panel (5 summary cards)
        JPanel kpiGrid = new JPanel(new GridLayout(1, 5, 15, 0));
        kpiGrid.setOpaque(false);
        kpiGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        long activePrjs = projects.stream().filter(p -> p.getStatus() == ProjectStatus.ACTIVE).count();
        long compPrjs = projects.stream().filter(p -> p.getStatus() == ProjectStatus.COMPLETED).count();
        long compTasks = tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();

        kpiGrid.add(new DashboardCard("📁", "Total Projects", String.valueOf(projects.size()), ThemeManager.COLOR_PRIMARY));
        kpiGrid.add(new DashboardCard("⏳", "Active Projects", String.valueOf(activePrjs), ThemeManager.COLOR_WARNING));
        kpiGrid.add(new DashboardCard("✓", "Completed Projects", String.valueOf(compPrjs), ThemeManager.COLOR_SUCCESS));
        kpiGrid.add(new DashboardCard("📋", "Total Tasks", String.valueOf(tasks.size()), ThemeManager.COLOR_PRIMARY_HOVER));
        kpiGrid.add(new DashboardCard("✓", "Completed Tasks", String.valueOf(compTasks), ThemeManager.COLOR_SUCCESS));

        contentPanel.add(kpiGrid);
        contentPanel.add(Box.createVerticalStrut(25));

        // Distribution Panel
        JPanel distribPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        distribPanel.setOpaque(false);
        distribPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        // Status Breakdown
        JPanel statusPanel = new RoundedPanel(12, ThemeManager.COLOR_CARD);
        statusPanel.setLayout(new BorderLayout());
        statusPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel statusTitle = new JLabel("Task Status Statistics");
        statusTitle.setFont(ThemeManager.FONT_SUBTITLE);
        statusTitle.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        statusTitle.setBorder(new EmptyBorder(0, 0, 12, 0));
        statusPanel.add(statusTitle, BorderLayout.NORTH);

        JPanel statusRows = new JPanel();
        statusRows.setOpaque(false);
        statusRows.setLayout(new BoxLayout(statusRows, BoxLayout.Y_AXIS));

        Map<TaskStatus, Long> statusCounts = tasks.stream().collect(Collectors.groupingBy(Task::getStatus, Collectors.counting()));
        int totalTasks = tasks.size();

        statusRows.add(createBreakdownRow("📋 TO DO", statusCounts.getOrDefault(TaskStatus.TO_DO, 0L).intValue(), totalTasks, new Color(148, 163, 184)));
        statusRows.add(createBreakdownRow("⚡ IN PROGRESS", statusCounts.getOrDefault(TaskStatus.IN_PROGRESS, 0L).intValue(), totalTasks, new Color(59, 130, 246)));
        statusRows.add(createBreakdownRow("🧪 TESTING", statusCounts.getOrDefault(TaskStatus.TESTING, 0L).intValue(), totalTasks, new Color(245, 158, 11)));
        statusRows.add(createBreakdownRow("✓ COMPLETED", statusCounts.getOrDefault(TaskStatus.COMPLETED, 0L).intValue(), totalTasks, ThemeManager.COLOR_SUCCESS));
        statusRows.add(createBreakdownRow("🚫 BLOCKED", statusCounts.getOrDefault(TaskStatus.BLOCKED, 0L).intValue(), totalTasks, ThemeManager.COLOR_DANGER));

        statusPanel.add(statusRows, BorderLayout.CENTER);
        distribPanel.add(statusPanel);

        // Priority Breakdown
        JPanel priorityPanel = new RoundedPanel(12, ThemeManager.COLOR_CARD);
        priorityPanel.setLayout(new BorderLayout());
        priorityPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel priorityTitle = new JLabel("Task Priority Distributions");
        priorityTitle.setFont(ThemeManager.FONT_SUBTITLE);
        priorityTitle.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        priorityTitle.setBorder(new EmptyBorder(0, 0, 12, 0));
        priorityPanel.add(priorityTitle, BorderLayout.NORTH);

        JPanel priorityRows = new JPanel();
        priorityRows.setOpaque(false);
        priorityRows.setLayout(new BoxLayout(priorityRows, BoxLayout.Y_AXIS));

        Map<TaskPriority, Long> priorityCounts = tasks.stream().collect(Collectors.groupingBy(Task::getPriority, Collectors.counting()));

        priorityRows.add(createBreakdownRow("🔴 CRITICAL", priorityCounts.getOrDefault(TaskPriority.CRITICAL, 0L).intValue(), totalTasks, ThemeManager.COLOR_DANGER));
        priorityRows.add(createBreakdownRow("🟠 HIGH", priorityCounts.getOrDefault(TaskPriority.HIGH, 0L).intValue(), totalTasks, ThemeManager.COLOR_WARNING));
        priorityRows.add(createBreakdownRow("🔵 MEDIUM", priorityCounts.getOrDefault(TaskPriority.MEDIUM, 0L).intValue(), totalTasks, new Color(79, 70, 229)));
        priorityRows.add(createBreakdownRow("🟢 LOW", priorityCounts.getOrDefault(TaskPriority.LOW, 0L).intValue(), totalTasks, new Color(71, 85, 105)));

        priorityPanel.add(priorityRows, BorderLayout.CENTER);
        distribPanel.add(priorityPanel);

        contentPanel.add(distribPanel);
        contentPanel.add(Box.createVerticalStrut(25));

        // Project Specific Auditing Directory (Original function)
        JPanel auditPanel = new RoundedPanel(12, ThemeManager.COLOR_CARD);
        auditPanel.setLayout(new BorderLayout(15, 10));
        auditPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        auditPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 500));

        // ComboBox + Export row
        JPanel filterRow = new JPanel(new BorderLayout());
        filterRow.setOpaque(false);

        JPanel comboPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        comboPanel.setOpaque(false);
        comboPanel.add(new JLabel("Select Project for Audit:"));

        projectCombo = new JComboBox<>();
        projectCombo.setPreferredSize(new Dimension(250, 30));
        for (Project p : projects) {
            projectCombo.addItem(new ProjectItem(p.getId(), p.getName()));
        }
        projectCombo.addActionListener(e -> calculateProjectReport());
        comboPanel.add(projectCombo);
        filterRow.add(comboPanel, BorderLayout.WEST);

        exportButton = new JButton("📤 Export CSV Report");
        exportButton.setBackground(ThemeManager.COLOR_PRIMARY);
        exportButton.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        exportButton.setFont(ThemeManager.FONT_BOLD_SMALL);
        exportButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        exportButton.addActionListener(e -> handleExportCSV());
        filterRow.add(exportButton, BorderLayout.EAST);

        auditPanel.add(filterRow, BorderLayout.NORTH);

        // Core Audit Center wrapper
        JPanel auditCenterPanel = new JPanel(new BorderLayout(0, 15));
        auditCenterPanel.setOpaque(false);

        // Sub project cards
        JPanel subGrid = new JPanel(new GridLayout(1, 5, 12, 0));
        subGrid.setOpaque(false);
        subGrid.setPreferredSize(new Dimension(800, 75));

        totalTasksVal = new JLabel("0", SwingConstants.CENTER);
        completedTasksVal = new JLabel("0", SwingConstants.CENTER);
        pendingTasksVal = new JLabel("0", SwingConstants.CENTER);
        blockedTasksVal = new JLabel("0", SwingConstants.CENTER);
        overdueTasksVal = new JLabel("0", SwingConstants.CENTER);

        subGrid.add(createStatCard(totalTasksVal, new JLabel("Total Tasks", SwingConstants.CENTER), ThemeManager.COLOR_PRIMARY));
        subGrid.add(createStatCard(completedTasksVal, new JLabel("Completed", SwingConstants.CENTER), ThemeManager.COLOR_SUCCESS));
        subGrid.add(createStatCard(pendingTasksVal, new JLabel("Pending", SwingConstants.CENTER), ThemeManager.COLOR_PRIMARY_HOVER));
        subGrid.add(createStatCard(blockedTasksVal, new JLabel("Blocked", SwingConstants.CENTER), ThemeManager.COLOR_WARNING));
        subGrid.add(createStatCard(overdueTasksVal, new JLabel("Overdue", SwingConstants.CENTER), ThemeManager.COLOR_DANGER));
        auditCenterPanel.add(subGrid, BorderLayout.NORTH);

        // Progress panel
        JPanel progressPanel = new JPanel(new BorderLayout(10, 4));
        progressPanel.setOpaque(false);

        completionPercentageLabel = new JLabel("Project Completion Rate: 0.0%");
        completionPercentageLabel.setFont(ThemeManager.FONT_BOLD_SMALL);
        completionPercentageLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        progressPanel.add(completionPercentageLabel, BorderLayout.WEST);

        completionProgressBar = new JProgressBar(0, 100);
        completionProgressBar.setStringPainted(true);
        completionProgressBar.setForeground(ThemeManager.COLOR_SUCCESS);
        progressPanel.add(completionProgressBar, BorderLayout.CENTER);
        auditCenterPanel.add(progressPanel, BorderLayout.SOUTH);

        auditPanel.add(auditCenterPanel, BorderLayout.CENTER);

        // JTable listing tasks
        tableModel = new DefaultTableModel(
                new Object[]{"Task ID", "Task Name", "Assigned Employee", "Priority", "Deadline Date", "Current Status"}, 0
        );
        reportTaskTable = new ModernTable();
        reportTaskTable.setPlaceholderText("No tasks assigned to this project.");
        reportTaskTable.setModel(tableModel);

        JScrollPane scroll = new JScrollPane(reportTaskTable);
        scroll.setPreferredSize(new Dimension(800, 220));
        scroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel tableWrapper = new JPanel(new BorderLayout(0, 8));
        tableWrapper.setOpaque(false);
        JLabel tableTitle = new JLabel("Project Tasks Audit Directory");
        tableTitle.setFont(ThemeManager.FONT_BOLD_SMALL);
        tableTitle.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        tableWrapper.add(tableTitle, BorderLayout.NORTH);
        tableWrapper.add(scroll, BorderLayout.CENTER);

        auditPanel.add(tableWrapper, BorderLayout.SOUTH);

        contentPanel.add(auditPanel);

        // Initial project report calculate
        calculateProjectReport();
    }

    private void buildEmployeeReports(List<Task> tasks) {
        addHeaderTitle("My Tasks Analytics Dashboard", "Personal progress rates, task completions, and timeline warnings");

        // KPI Summary cards
        JPanel kpiGrid = new JPanel(new GridLayout(1, 5, 15, 0));
        kpiGrid.setOpaque(false);
        kpiGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        long completed = tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
        long inProgress = tasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
        long testing = tasks.stream().filter(t -> t.getStatus() == TaskStatus.TESTING).count();
        long todo = tasks.stream().filter(t -> t.getStatus() == TaskStatus.TO_DO).count();

        kpiGrid.add(new DashboardCard("📋", "Assigned Tasks", String.valueOf(tasks.size()), ThemeManager.COLOR_PRIMARY));
        kpiGrid.add(new DashboardCard("✓", "Completed", String.valueOf(completed), ThemeManager.COLOR_SUCCESS));
        kpiGrid.add(new DashboardCard("⚡", "In Progress", String.valueOf(inProgress), ThemeManager.COLOR_PRIMARY_HOVER));
        kpiGrid.add(new DashboardCard("🧪", "Testing Tasks", String.valueOf(testing), ThemeManager.COLOR_WARNING));
        kpiGrid.add(new DashboardCard("📋", "To Do Tasks", String.valueOf(todo), new Color(148, 163, 184)));

        contentPanel.add(kpiGrid);
        contentPanel.add(Box.createVerticalStrut(25));

        // Overall progress Completion rate
        RoundedPanel progressPanel = new RoundedPanel(12, ThemeManager.COLOR_CARD);
        progressPanel.setDrawBorder(true);
        progressPanel.setBorderColor(ThemeManager.COLOR_BORDER);
        progressPanel.setLayout(new BorderLayout(15, 8));
        progressPanel.setBorder(new EmptyBorder(18, 20, 18, 20));
        progressPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));

        double progressPercentage = tasks.isEmpty() ? 0.0 : (completed * 100.0 / tasks.size());
        int progressInt = (int) Math.round(progressPercentage);

        JLabel progLabel = new JLabel("My Task Completion Progress: " + progressInt + "%");
        progLabel.setFont(ThemeManager.FONT_BOLD_SMALL);
        progLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        progressPanel.add(progLabel, BorderLayout.NORTH);

        JProgressBar employeeBar = new JProgressBar(0, 100);
        employeeBar.setValue(progressInt);
        employeeBar.setForeground(ThemeManager.COLOR_SUCCESS);
        progressPanel.add(employeeBar, BorderLayout.CENTER);

        contentPanel.add(progressPanel);
        contentPanel.add(Box.createVerticalStrut(25));

        // Deadlines alert panel
        JPanel alertPanel = new RoundedPanel(12, ThemeManager.COLOR_CARD);
        alertPanel.setLayout(new BorderLayout(10, 10));
        alertPanel.setBorder(new EmptyBorder(18, 20, 18, 20));
        alertPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 380));

        JLabel alertTitle = new JLabel("Deadline Action Alert Directory");
        alertTitle.setFont(ThemeManager.FONT_SUBTITLE);
        alertTitle.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        alertPanel.add(alertTitle, BorderLayout.NORTH);

        DefaultTableModel employeeModel = new DefaultTableModel(
                new Object[]{"Task ID", "Task Name", "Priority", "Deadline Date", "Days Remaining", "Current Status"}, 0
        );

        // Custom renderer to highlight overdue active rows in red
        ModernTable alertTable = new ModernTable() {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                Object daysVal = getValueAt(row, 4);
                if (daysVal instanceof String && ((String) daysVal).startsWith("OVERDUE")) {
                    c.setForeground(ThemeManager.COLOR_DANGER);
                    c.setFont(ThemeManager.FONT_BOLD_SMALL);
                } else {
                    c.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
                }
                return c;
            }
        };
        alertTable.setPlaceholderText("All assigned tasks completed! Great job.");
        alertTable.setModel(employeeModel);

        // Filter active tasks and sort by deadline
        List<Task> activeTasks = tasks.stream()
                .filter(t -> t.getStatus() != TaskStatus.COMPLETED)
                .sorted(Comparator.comparing(Task::getDeadline))
                .collect(Collectors.toList());

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (Task t : activeTasks) {
            String daysRemaining;
            long diff = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), t.getDeadline());
            if (diff < 0) {
                daysRemaining = "OVERDUE (" + Math.abs(diff) + " days ago)";
            } else if (diff == 0) {
                daysRemaining = "DUE TODAY";
            } else {
                daysRemaining = diff + " days left";
            }

            employeeModel.addRow(new Object[]{
                    t.getId(),
                    t.getName(),
                    t.getPriority().toString(),
                    t.getDeadline().format(dtf),
                    daysRemaining,
                    t.getStatus().toString()
            });
        }

        JScrollPane alertScroll = new JScrollPane(alertTable);
        alertScroll.setBorder(BorderFactory.createEmptyBorder());
        alertScroll.setPreferredSize(new Dimension(800, 260));
        alertPanel.add(alertScroll, BorderLayout.CENTER);

        contentPanel.add(alertPanel);
    }

    // --- Helpers ---

    private void addHeaderTitle(String title, String subtitle) {
        JPanel titlePanel = new JPanel(new GridBagLayout());
        titlePanel.setOpaque(false);
        titlePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;

        JLabel mainTitleLabel = new JLabel(title);
        mainTitleLabel.setFont(ThemeManager.FONT_TITLE);
        mainTitleLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        titlePanel.add(mainTitleLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(4, 0, 15, 0);
        JLabel subLabel = new JLabel(subtitle);
        subLabel.setFont(ThemeManager.FONT_BODY);
        subLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);
        titlePanel.add(subLabel, gbc);

        contentPanel.add(titlePanel);
    }

    private JPanel createStatCard(JLabel valLabel, JLabel lblLabel, Color borderHighlight) {
        RoundedPanel card = new RoundedPanel(12, ThemeManager.COLOR_CARD);
        card.setDrawBorder(true);
        card.setBorderColor(ThemeManager.COLOR_BORDER);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(8, 8, 8, 8));

        valLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        valLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);

        lblLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        lblLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);

        card.add(valLabel, BorderLayout.CENTER);
        card.add(lblLabel, BorderLayout.SOUTH);

        JPanel strip = new JPanel();
        strip.setBackground(borderHighlight);
        strip.setPreferredSize(new Dimension(100, 3));
        card.add(strip, BorderLayout.NORTH);

        return card;
    }

    private JPanel createBreakdownRow(String label, int count, int total, Color color) {
        JPanel row = new JPanel(new BorderLayout(15, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(6, 0, 6, 0));

        JLabel nameLabel = new JLabel(label);
        nameLabel.setFont(ThemeManager.FONT_BOLD_SMALL);
        nameLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        nameLabel.setPreferredSize(new Dimension(130, 20));
        row.add(nameLabel, BorderLayout.WEST);

        int pct = total > 0 ? (int) Math.round(count * 100.0 / total) : 0;
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(pct);
        bar.setForeground(color);
        bar.setPreferredSize(new Dimension(150, 12));
        row.add(bar, BorderLayout.CENTER);

        JLabel countLabel = new JLabel(count + " (" + pct + "%)", SwingConstants.RIGHT);
        countLabel.setFont(ThemeManager.FONT_SMALL);
        countLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);
        countLabel.setPreferredSize(new Dimension(100, 20));
        row.add(countLabel, BorderLayout.EAST);

        return row;
    }

    private void calculateProjectReport() {
        if (projectCombo == null) return;
        ProjectItem item = (ProjectItem) projectCombo.getSelectedItem();
        if (item == null) {
            clearReportFields();
            return;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                currentReport = mainFrame.getReportService().getProjectProgressReport(item.id);
                currentProjectTasks = mainFrame.getTaskService().getTasksByProject(item.id);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    if (currentReport != null) {
                        totalTasksVal.setText(String.valueOf(currentReport.getTotalTasks()));
                        completedTasksVal.setText(String.valueOf(currentReport.getCompletedTasks()));
                        pendingTasksVal.setText(String.valueOf(currentReport.getPendingTasks()));
                        blockedTasksVal.setText(String.valueOf(currentReport.getBlockedTasks()));
                        overdueTasksVal.setText(String.valueOf(currentReport.getOverdueTasks()));

                        int compRate = (int) Math.round(currentReport.getCompletionPercentage());
                        completionProgressBar.setValue(compRate);
                        completionPercentageLabel.setText("Project Completion Rate: " + currentReport.getCompletionPercentage() + "%");

                        tableModel.setRowCount(0);
                        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                        for (Task t : currentProjectTasks) {
                            String empName = "Unassigned";
                            if (t.getAssignedEmployeeId() != null) {
                                Optional<User> u = allUsersList.stream().filter(usr -> usr.getId() == t.getAssignedEmployeeId()).findFirst();
                                if (u.isPresent()) {
                                    empName = u.get().getFullName();
                                }
                            }
                            tableModel.addRow(new Object[]{
                                    t.getId(),
                                    t.getName(),
                                    empName,
                                    t.getPriority().name(),
                                    t.getDeadline() != null ? t.getDeadline().format(dtf) : "",
                                    t.getStatus().name()
                            });
                        }
                        exportButton.setEnabled(true);
                    }
                } catch (Exception ex) {
                    System.err.println("Failed to compile project progress metrics: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void clearReportFields() {
        totalTasksVal.setText("0");
        completedTasksVal.setText("0");
        pendingTasksVal.setText("0");
        blockedTasksVal.setText("0");
        overdueTasksVal.setText("0");
        completionProgressBar.setValue(0);
        completionPercentageLabel.setText("Project Completion Rate: 0.0%");
        tableModel.setRowCount(0);
        exportButton.setEnabled(false);
    }

    private void handleExportCSV() {
        if (currentReport == null) return;

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export CSV Report");
        fileChooser.setSelectedFile(new File("Project_Report_" + currentReport.getProjectName().replace(" ", "_") + ".csv"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();

            if (!fileToSave.getName().toLowerCase().endsWith(".csv")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".csv");
            }

            File finalFile = fileToSave;
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    CSVExporter.exportProjectReport(currentReport, currentProjectTasks, allUsersList, finalFile);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(ReportsView.this,
                                "Project report CSV exported successfully to:\n" + finalFile.getAbsolutePath(),
                                "Export Complete",
                                JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(ReportsView.this,
                                "Failed to write CSV file. Make sure file is not open elsewhere and path is writable.",
                                "Write Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }
}

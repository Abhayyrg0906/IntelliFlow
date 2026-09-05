package com.intelliflow.ui.views;

import com.intelliflow.context.UserSession;
import com.intelliflow.enums.ProjectHealth;
import com.intelliflow.enums.ProjectStatus;
import com.intelliflow.enums.Role;
import com.intelliflow.enums.TaskPriority;
import com.intelliflow.enums.TaskStatus;
import com.intelliflow.exception.ValidationException;
import com.intelliflow.model.*;
import com.intelliflow.ui.MainFrame;
import com.intelliflow.ui.ThemeManager;
import com.intelliflow.ui.components.DashboardCard;
import com.intelliflow.ui.components.ModernTable;
import com.intelliflow.ui.components.RoundedPanel;
import com.intelliflow.util.CSVExporter;
import com.intelliflow.util.PDFExporter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
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
            private AnalyticsSummary summary;

            @Override
            protected Void doInBackground() throws Exception {
                users = mainFrame.getUserService().getAllUsers();
                summary = mainFrame.getReportService().getAnalyticsSummary(currentUser);

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
                        buildAdminReports(summary, tasks, projects, users);
                    } else if (currentUser.getRole() == Role.MANAGER) {
                        buildManagerReports(summary, tasks, projects, users);
                    } else {
                        buildEmployeeReports(summary, tasks, projects);
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

    // ==========================================
    // ADMIN REPORTS & ANALYTICS
    // ==========================================
    private void buildAdminReports(AnalyticsSummary summary, List<Task> tasks, List<Project> projects, List<User> users) {
        addHeaderTitle("System Analytics & Intelligence Hub", "Real-time system-wide analytics, project health monitoring, and performance audit");

        // 1. Primary KPIs Row (5 cards)
        JPanel kpiGrid = new JPanel(new GridLayout(1, 5, 15, 0));
        kpiGrid.setOpaque(false);
        kpiGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        kpiGrid.add(new DashboardCard("📋", "Total Tasks", String.valueOf(summary.getTotalTasks()), ThemeManager.COLOR_PRIMARY));
        kpiGrid.add(new DashboardCard("🎯", "Completion Rate", summary.getTaskCompletionRate() + "%", ThemeManager.COLOR_SUCCESS));
        kpiGrid.add(new DashboardCard("⛔", "Overdue Tasks", String.valueOf(summary.getOverdueTaskCount()), ThemeManager.COLOR_DANGER));
        kpiGrid.add(new DashboardCard("⚠️", "Due Soon (1-2d)", String.valueOf(summary.getDueSoonTaskCount()), ThemeManager.COLOR_WARNING));
        kpiGrid.add(new DashboardCard("📁", "Total Projects", String.valueOf(projects.size()), new Color(139, 92, 246)));

        contentPanel.add(kpiGrid);
        contentPanel.add(Box.createVerticalStrut(20));

        // 2. Distributions Grid (Priority & Status)
        JPanel distribPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        distribPanel.setOpaque(false);
        distribPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        distribPanel.add(createPriorityDistributionCard(summary.getPriorityDistribution(), summary.getTotalTasks()));
        distribPanel.add(createStatusDistributionCard(summary.getStatusDistribution(), summary.getTotalTasks()));

        contentPanel.add(distribPanel);
        contentPanel.add(Box.createVerticalStrut(20));

        // 3. Project Progress & Health Section
        contentPanel.add(createProjectProgressSection(summary.getProjectProgressList(), summary.getProjectHealthDistribution()));
        contentPanel.add(Box.createVerticalStrut(20));

        // 4. Employee Workload Section
        contentPanel.add(createEmployeeWorkloadSection(summary.getEmployeeWorkloads()));
        contentPanel.add(Box.createVerticalStrut(20));

        // 5. Interactive Project Audit & CSV Export
        buildProjectAuditSection(projects);
    }

    // ==========================================
    // MANAGER REPORTS & ANALYTICS
    // ==========================================
    private void buildManagerReports(AnalyticsSummary summary, List<Task> tasks, List<Project> projects, List<User> users) {
        addHeaderTitle("Managed Projects & Team Analytics", "Project completion rates, workload distribution, and deadline health for your managed portfolio");

        // 1. Primary KPIs Row (5 cards)
        JPanel kpiGrid = new JPanel(new GridLayout(1, 5, 15, 0));
        kpiGrid.setOpaque(false);
        kpiGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        kpiGrid.add(new DashboardCard("📁", "Managed Projects", String.valueOf(projects.size()), ThemeManager.COLOR_PRIMARY));
        kpiGrid.add(new DashboardCard("📋", "Team Tasks", String.valueOf(summary.getTotalTasks()), ThemeManager.COLOR_PRIMARY_HOVER));
        kpiGrid.add(new DashboardCard("🎯", "Completion Rate", summary.getTaskCompletionRate() + "%", ThemeManager.COLOR_SUCCESS));
        kpiGrid.add(new DashboardCard("⛔", "Overdue Tasks", String.valueOf(summary.getOverdueTaskCount()), ThemeManager.COLOR_DANGER));
        kpiGrid.add(new DashboardCard("⚠️", "Due Soon (1-2d)", String.valueOf(summary.getDueSoonTaskCount()), ThemeManager.COLOR_WARNING));

        contentPanel.add(kpiGrid);
        contentPanel.add(Box.createVerticalStrut(20));

        // 2. Distributions Grid (Priority & Status)
        JPanel distribPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        distribPanel.setOpaque(false);
        distribPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        distribPanel.add(createPriorityDistributionCard(summary.getPriorityDistribution(), summary.getTotalTasks()));
        distribPanel.add(createStatusDistributionCard(summary.getStatusDistribution(), summary.getTotalTasks()));

        contentPanel.add(distribPanel);
        contentPanel.add(Box.createVerticalStrut(20));

        // 3. Project Progress & Health Section
        contentPanel.add(createProjectProgressSection(summary.getProjectProgressList(), summary.getProjectHealthDistribution()));
        contentPanel.add(Box.createVerticalStrut(20));

        // 4. Team Workload Section
        contentPanel.add(createEmployeeWorkloadSection(summary.getEmployeeWorkloads()));
        contentPanel.add(Box.createVerticalStrut(20));

        // 5. Interactive Project Audit & CSV Export
        buildProjectAuditSection(projects);
    }

    // ==========================================
    // EMPLOYEE REPORTS & ANALYTICS
    // ==========================================
    private void buildEmployeeReports(AnalyticsSummary summary, List<Task> tasks, List<Project> projects) {
        addHeaderTitle("Personal Productivity Analytics", "Assigned task completion rate, priority distribution, and upcoming deadline tracking");

        // 1. Primary KPIs Row (5 cards)
        JPanel kpiGrid = new JPanel(new GridLayout(1, 5, 15, 0));
        kpiGrid.setOpaque(false);
        kpiGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        kpiGrid.add(new DashboardCard("📋", "My Tasks", String.valueOf(summary.getTotalTasks()), ThemeManager.COLOR_PRIMARY));
        kpiGrid.add(new DashboardCard("✓", "Completed", String.valueOf(summary.getCompletedTasks()), ThemeManager.COLOR_SUCCESS));
        kpiGrid.add(new DashboardCard("🎯", "Completion Rate", summary.getTaskCompletionRate() + "%", ThemeManager.COLOR_PRIMARY_HOVER));
        kpiGrid.add(new DashboardCard("⛔", "Overdue Tasks", String.valueOf(summary.getOverdueTaskCount()), ThemeManager.COLOR_DANGER));
        kpiGrid.add(new DashboardCard("⚠️", "Due Soon (1-2d)", String.valueOf(summary.getDueSoonTaskCount()), ThemeManager.COLOR_WARNING));

        contentPanel.add(kpiGrid);
        contentPanel.add(Box.createVerticalStrut(20));

        // 2. Distributions Grid (Priority & Status)
        JPanel distribPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        distribPanel.setOpaque(false);
        distribPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        distribPanel.add(createPriorityDistributionCard(summary.getPriorityDistribution(), summary.getTotalTasks()));
        distribPanel.add(createStatusDistributionCard(summary.getStatusDistribution(), summary.getTotalTasks()));

        contentPanel.add(distribPanel);
        contentPanel.add(Box.createVerticalStrut(20));

        // 3. Project Progress for Contributed Projects
        if (!summary.getProjectProgressList().isEmpty()) {
            contentPanel.add(createProjectProgressSection(summary.getProjectProgressList(), summary.getProjectHealthDistribution()));
            contentPanel.add(Box.createVerticalStrut(20));
        }

        // 4. Deadlines Action Directory
        buildEmployeeDeadlinesSection(tasks);
    }

    // ==========================================
    // REUSABLE VISUAL ANALYTICS COMPONENTS
    // ==========================================

    private JPanel createPriorityDistributionCard(Map<TaskPriority, Integer> priorityMap, int totalTasks) {
        JPanel card = new RoundedPanel(12, ThemeManager.COLOR_CARD);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel title = new JLabel("TASK PRIORITY DISTRIBUTION");
        title.setFont(ThemeManager.FONT_SUBTITLE);
        title.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(0, 0, 12, 0));
        card.add(title, BorderLayout.NORTH);

        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));

        rows.add(createBreakdownRow("🔴 CRITICAL", priorityMap.getOrDefault(TaskPriority.CRITICAL, 0), totalTasks, ThemeManager.COLOR_DANGER));
        rows.add(createBreakdownRow("🟠 HIGH", priorityMap.getOrDefault(TaskPriority.HIGH, 0), totalTasks, ThemeManager.COLOR_WARNING));
        rows.add(createBreakdownRow("🔵 MEDIUM", priorityMap.getOrDefault(TaskPriority.MEDIUM, 0), totalTasks, new Color(59, 130, 246)));
        rows.add(createBreakdownRow("🟢 LOW", priorityMap.getOrDefault(TaskPriority.LOW, 0), totalTasks, new Color(71, 85, 105)));

        card.add(rows, BorderLayout.CENTER);
        return card;
    }

    private JPanel createStatusDistributionCard(Map<TaskStatus, Integer> statusMap, int totalTasks) {
        JPanel card = new RoundedPanel(12, ThemeManager.COLOR_CARD);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel title = new JLabel("TASK STATUS DISTRIBUTION");
        title.setFont(ThemeManager.FONT_SUBTITLE);
        title.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(0, 0, 12, 0));
        card.add(title, BorderLayout.NORTH);

        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));

        rows.add(createBreakdownRow("📋 TO DO", statusMap.getOrDefault(TaskStatus.TO_DO, 0), totalTasks, new Color(148, 163, 184)));
        rows.add(createBreakdownRow("⚡ IN PROGRESS", statusMap.getOrDefault(TaskStatus.IN_PROGRESS, 0), totalTasks, new Color(59, 130, 246)));
        rows.add(createBreakdownRow("🧪 TESTING", statusMap.getOrDefault(TaskStatus.TESTING, 0), totalTasks, new Color(245, 158, 11)));
        rows.add(createBreakdownRow("✓ COMPLETED", statusMap.getOrDefault(TaskStatus.COMPLETED, 0), totalTasks, ThemeManager.COLOR_SUCCESS));
        rows.add(createBreakdownRow("🚫 BLOCKED", statusMap.getOrDefault(TaskStatus.BLOCKED, 0), totalTasks, ThemeManager.COLOR_DANGER));

        card.add(rows, BorderLayout.CENTER);
        return card;
    }

    private JPanel createProjectProgressSection(List<ProjectProgressReport> projectReports, Map<ProjectHealth, Integer> healthMap) {
        RoundedPanel section = new RoundedPanel(12, ThemeManager.COLOR_CARD);
        section.setLayout(new BorderLayout(15, 12));
        section.setBorder(new EmptyBorder(18, 20, 18, 20));
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));

        // Header with Health Summary Badges
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("PROJECT PROGRESS & HEALTH MONITORING");
        title.setFont(ThemeManager.FONT_SUBTITLE);
        title.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);

        JPanel healthBadges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        healthBadges.setOpaque(false);

        int onTrack = healthMap.getOrDefault(ProjectHealth.ON_TRACK, 0);
        int atRisk = healthMap.getOrDefault(ProjectHealth.AT_RISK, 0);
        int delayed = healthMap.getOrDefault(ProjectHealth.DELAYED, 0);

        healthBadges.add(createPillBadge("🟢 ON TRACK: " + onTrack, new Color(34, 197, 94, 40), ThemeManager.COLOR_SUCCESS));
        healthBadges.add(createPillBadge("🟡 AT RISK: " + atRisk, new Color(245, 158, 11, 40), ThemeManager.COLOR_WARNING));
        healthBadges.add(createPillBadge("🔴 DELAYED: " + delayed, new Color(239, 68, 68, 40), ThemeManager.COLOR_DANGER));
        header.add(healthBadges, BorderLayout.EAST);

        section.add(header, BorderLayout.NORTH);

        // Progress Table
        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Project Name", "Health", "Tasks (Done / Total)", "Progress Track", "Completion %"}, 0
        );
        ModernTable table = new ModernTable();
        table.setModel(model);
        table.setPlaceholderText("No projects available to monitor.");

        for (ProjectProgressReport p : projectReports) {
            String asciiBar = formatAsciiProgressBar(p.getCompletionPercentage());
            String healthLabel = switch (p.getHealth()) {
                case ON_TRACK -> "🟢 ON TRACK";
                case AT_RISK -> "🟡 AT RISK";
                case DELAYED -> "🔴 DELAYED";
            };

            model.addRow(new Object[]{
                    p.getProjectName(),
                    healthLabel,
                    p.getCompletedTasks() + " / " + p.getTotalTasks(),
                    asciiBar,
                    p.getCompletionPercentage() + "%"
            });
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setPreferredSize(new Dimension(800, 160));
        section.add(scroll, BorderLayout.CENTER);

        return section;
    }

    private JPanel createEmployeeWorkloadSection(List<EmployeePerformanceReport> workloads) {
        RoundedPanel section = new RoundedPanel(12, ThemeManager.COLOR_CARD);
        section.setLayout(new BorderLayout(15, 12));
        section.setBorder(new EmptyBorder(18, 20, 18, 20));
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));

        JLabel title = new JLabel("EMPLOYEE WORKLOAD & PERFORMANCE ANALYTICS");
        title.setFont(ThemeManager.FONT_SUBTITLE);
        title.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        section.add(title, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Employee Name", "Total Assigned", "Active Tasks", "Completed Tasks", "Overdue Tasks", "Workload Completion %"}, 0
        );
        ModernTable table = new ModernTable();
        table.setModel(model);
        table.setPlaceholderText("No employee workload records available.");

        for (EmployeePerformanceReport emp : workloads) {
            model.addRow(new Object[]{
                    emp.getEmployeeName(),
                    emp.getTotalTasks(),
                    emp.getPendingTasks(),
                    emp.getCompletedTasks(),
                    emp.getOverdueTasks() > 0 ? "⛔ " + emp.getOverdueTasks() : "0",
                    emp.getCompletionRate() + "% (" + formatAsciiProgressBar(emp.getCompletionRate()) + ")"
            });
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setPreferredSize(new Dimension(800, 160));
        section.add(scroll, BorderLayout.CENTER);

        return section;
    }

    private void buildProjectAuditSection(List<Project> projects) {
        JPanel auditPanel = new RoundedPanel(12, ThemeManager.COLOR_CARD);
        auditPanel.setLayout(new BorderLayout(15, 10));
        auditPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        auditPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 500));

        // ComboBox + Export row
        JPanel filterRow = new JPanel(new BorderLayout());
        filterRow.setOpaque(false);

        JPanel comboPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        comboPanel.setOpaque(false);
        JLabel comboLbl = new JLabel("Project Audit Directory:");
        comboLbl.setFont(ThemeManager.FONT_BOLD_SMALL);
        comboLbl.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        comboPanel.add(comboLbl);

        projectCombo = new JComboBox<>();
        projectCombo.setPreferredSize(new Dimension(250, 30));
        for (Project p : projects) {
            projectCombo.addItem(new ProjectItem(p.getId(), p.getName()));
        }
        projectCombo.addActionListener(e -> calculateProjectReport());
        comboPanel.add(projectCombo);
        filterRow.add(comboPanel, BorderLayout.WEST);

        JPanel exportBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        exportBtns.setOpaque(false);

        JButton exportPdfBtn = new JButton("📄 Export PDF");
        exportPdfBtn.setBackground(ThemeManager.COLOR_PRIMARY_HOVER);
        exportPdfBtn.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        exportPdfBtn.setFont(ThemeManager.FONT_BOLD_SMALL);
        exportPdfBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        exportPdfBtn.addActionListener(e -> handleExportProjectPDF());

        exportButton = new JButton("📊 Export CSV");
        exportButton.setBackground(ThemeManager.COLOR_PRIMARY);
        exportButton.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        exportButton.setFont(ThemeManager.FONT_BOLD_SMALL);
        exportButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        exportButton.addActionListener(e -> handleExportCSV());

        exportBtns.add(exportPdfBtn);
        exportBtns.add(exportButton);
        filterRow.add(exportBtns, BorderLayout.EAST);

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
        scroll.setPreferredSize(new Dimension(800, 200));
        scroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel tableWrapper = new JPanel(new BorderLayout(0, 8));
        tableWrapper.setOpaque(false);
        JLabel tableTitle = new JLabel("Project Tasks Breakdown");
        tableTitle.setFont(ThemeManager.FONT_BOLD_SMALL);
        tableTitle.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        tableWrapper.add(tableTitle, BorderLayout.NORTH);
        tableWrapper.add(scroll, BorderLayout.CENTER);

        auditPanel.add(tableWrapper, BorderLayout.SOUTH);

        contentPanel.add(auditPanel);

        // Initial project report calculate
        calculateProjectReport();
    }

    private void buildEmployeeDeadlinesSection(List<Task> tasks) {
        JPanel alertPanel = new RoundedPanel(12, ThemeManager.COLOR_CARD);
        alertPanel.setLayout(new BorderLayout(10, 10));
        alertPanel.setBorder(new EmptyBorder(18, 20, 18, 20));
        alertPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 380));

        JLabel alertTitle = new JLabel("DEADLINE ACTION & SCHEDULE DIRECTORY");
        alertTitle.setFont(ThemeManager.FONT_SUBTITLE);
        alertTitle.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        alertPanel.add(alertTitle, BorderLayout.NORTH);

        DefaultTableModel employeeModel = new DefaultTableModel(
                new Object[]{"Task ID", "Task Name", "Priority", "Deadline Date", "Days Remaining", "Current Status"}, 0
        );

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

        List<Task> activeTasks = tasks.stream()
                .filter(t -> t.getStatus() != TaskStatus.COMPLETED)
                .sorted(Comparator.comparing(Task::getDeadline, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate today = LocalDate.now();
        for (Task t : activeTasks) {
            String daysRemaining;
            String deadlineStr;
            if (t.getDeadline() == null) {
                daysRemaining = "No Deadline";
                deadlineStr = "No Deadline";
            } else {
                long diff = ChronoUnit.DAYS.between(today, t.getDeadline());
                if (diff < 0) {
                    daysRemaining = "OVERDUE (" + Math.abs(diff) + " days ago)";
                } else if (diff == 0) {
                    daysRemaining = "DUE TODAY";
                } else {
                    daysRemaining = diff + " days left";
                }
                deadlineStr = t.getDeadline().format(dtf);
            }

            employeeModel.addRow(new Object[]{
                    t.getId(),
                    t.getName(),
                    t.getPriority().toString(),
                    deadlineStr,
                    daysRemaining,
                    t.getStatus().toString()
            });
        }

        JScrollPane alertScroll = new JScrollPane(alertTable);
        alertScroll.setBorder(BorderFactory.createEmptyBorder());
        alertScroll.setPreferredSize(new Dimension(800, 240));
        alertPanel.add(alertScroll, BorderLayout.CENTER);

        contentPanel.add(alertPanel);
    }

    // --- Helpers ---

    public static String formatAsciiProgressBar(double percentage) {
        int totalBlocks = 18;
        int filledBlocks = (int) Math.round((Math.max(0.0, Math.min(100.0, percentage)) / 100.0) * totalBlocks);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < filledBlocks; i++) {
            sb.append("█");
        }
        for (int i = filledBlocks; i < totalBlocks; i++) {
            sb.append("░");
        }
        sb.append(String.format(" %d%%", (int) Math.round(percentage)));
        return sb.toString();
    }

    private void addHeaderTitle(String title, String subtitle) {
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        titlePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        JLabel mainTitleLabel = new JLabel(title);
        mainTitleLabel.setFont(ThemeManager.FONT_TITLE);
        mainTitleLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        textPanel.add(mainTitleLabel);
        textPanel.add(Box.createVerticalStrut(4));

        JLabel subLabel = new JLabel(subtitle);
        subLabel.setFont(ThemeManager.FONT_BODY);
        subLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);
        textPanel.add(subLabel);

        titlePanel.add(textPanel, BorderLayout.WEST);

        // Export Reports Hub button
        JButton exportHubBtn = new JButton("📥 Export Reports Hub");
        exportHubBtn.setBackground(ThemeManager.COLOR_PRIMARY);
        exportHubBtn.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        exportHubBtn.setFont(ThemeManager.FONT_BOLD_SMALL);
        exportHubBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        exportHubBtn.addActionListener(e -> showExportHubDialog());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
        btnPanel.setOpaque(false);
        btnPanel.add(exportHubBtn);
        titlePanel.add(btnPanel, BorderLayout.EAST);

        contentPanel.add(titlePanel);
        contentPanel.add(Box.createVerticalStrut(15));
    }

    private JPanel createPillBadge(String text, Color bg, Color fg) {
        JPanel badge = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        badge.setBackground(bg);
        badge.setBorder(BorderFactory.createLineBorder(fg, 1));
        JLabel lbl = new JLabel(text);
        lbl.setFont(ThemeManager.FONT_BOLD_SMALL);
        lbl.setForeground(fg);
        badge.add(lbl);
        return badge;
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

        JLabel titleLbl = new JLabel(label);
        titleLbl.setFont(ThemeManager.FONT_BODY);
        titleLbl.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        titleLbl.setPreferredSize(new Dimension(140, 20));
        row.add(titleLbl, BorderLayout.WEST);

        int percent = total > 0 ? (int) Math.round(((double) count / total) * 100) : 0;

        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(percent);
        bar.setForeground(color);
        bar.setBackground(ThemeManager.COLOR_BACKGROUND);
        bar.setBorderPainted(false);
        bar.setPreferredSize(new Dimension(150, 10));
        row.add(bar, BorderLayout.CENTER);

        JLabel valLbl = new JLabel(count + " (" + percent + "%)", SwingConstants.RIGHT);
        valLbl.setFont(ThemeManager.FONT_BOLD_SMALL);
        valLbl.setForeground(ThemeManager.COLOR_TEXT_MUTED);
        valLbl.setPreferredSize(new Dimension(80, 20));
        row.add(valLbl, BorderLayout.EAST);

        return row;
    }

    // --- Action Methods ---

    private void calculateProjectReport() {
        if (projectCombo == null || projectCombo.getSelectedItem() == null) return;
        ProjectItem item = (ProjectItem) projectCombo.getSelectedItem();

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private List<Task> tasks = new ArrayList<>();

            @Override
            protected Void doInBackground() throws Exception {
                clearReportFields();
                try {
                    tasks = mainFrame.getTaskService().getTasksByProject(item.id);
                    currentReport = mainFrame.getReportService().getProjectProgressReport(item.id);
                } catch (ValidationException e) {
                    currentReport = null;
                }
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

                        if (compRate >= 100) {
                            completionProgressBar.setForeground(ThemeManager.COLOR_SUCCESS);
                        } else if (compRate > 50) {
                            completionProgressBar.setForeground(ThemeManager.COLOR_PRIMARY_HOVER);
                        } else {
                            completionProgressBar.setForeground(ThemeManager.COLOR_WARNING);
                        }
                    }

                    currentProjectTasks = tasks;
                    tableModel.setRowCount(0);

                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    for (Task t : tasks) {
                        String empName = "Unassigned";
                        if (t.getAssignedEmployeeId() != null && t.getAssignedEmployeeId() > 0) {
                            for (User u : allUsersList) {
                                if (u.getId() == t.getAssignedEmployeeId()) {
                                    empName = u.getFullName();
                                    break;
                                }
                            }
                        }
                        String deadlineStr = t.getDeadline() != null ? t.getDeadline().format(dtf) : "No Deadline";

                        tableModel.addRow(new Object[]{
                                t.getId(),
                                t.getName(),
                                empName,
                                t.getPriority().toString(),
                                deadlineStr,
                                t.getStatus().toString()
                        });
                    }
                } catch (Exception e) {
                    System.err.println("Error rendering project report details: " + e.getMessage());
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
    }

    private void handleExportCSV() {
        if (currentReport == null) {
            JOptionPane.showMessageDialog(this, "Please select a project with valid data to export.", "No Data", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String defaultName = CSVExporter.getSafeFilename("Project_Report_" + currentReport.getProjectName(), "csv");
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Project Report (CSV)");
        fileChooser.setSelectedFile(new File(defaultName));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (!selectedFile.getName().toLowerCase().endsWith(".csv")) {
                selectedFile = new File(selectedFile.getAbsolutePath() + ".csv");
            }

            final File finalFile = selectedFile;
            SwingWorker<Boolean, Void> exportWorker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    CSVExporter.exportProjectReport(currentReport, currentProjectTasks, allUsersList, finalFile);
                    return true;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(ReportsView.this,
                                "Project report CSV exported successfully to:\n" + finalFile.getAbsolutePath(),
                                "Export Successful",
                                JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(ReportsView.this,
                                "Failed to export report: " + ex.getMessage(),
                                "Export Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            exportWorker.execute();
        }
    }

    private void handleExportProjectPDF() {
        if (currentReport == null) {
            JOptionPane.showMessageDialog(this, "Please select a project with valid data to export.", "No Data", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String defaultName = CSVExporter.getSafeFilename("Project_Report_" + currentReport.getProjectName(), "pdf");
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Project Report (PDF)");
        fileChooser.setSelectedFile(new File(defaultName));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (!selectedFile.getName().toLowerCase().endsWith(".pdf")) {
                selectedFile = new File(selectedFile.getAbsolutePath() + ".pdf");
            }

            final File finalFile = selectedFile;
            SwingWorker<Boolean, Void> exportWorker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    PDFExporter.exportProjectReport(currentReport, currentProjectTasks, allUsersList, finalFile);
                    return true;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(ReportsView.this,
                                "Project report PDF exported successfully to:\n" + finalFile.getAbsolutePath(),
                                "Export Successful",
                                JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(ReportsView.this,
                                "Failed to export PDF report: " + ex.getMessage(),
                                "Export Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            exportWorker.execute();
        }
    }

    private void showExportHubDialog() {
        JDialog dialog = new JDialog(mainFrame, "📥 Export Intelligence & Reports Hub", true);
        dialog.setSize(480, 420);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 20, 8, 20);
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;

        JLabel titleLbl = new JLabel("Select Report Type & Export Format");
        titleLbl.setFont(ThemeManager.FONT_SUBTITLE);
        titleLbl.setForeground(ThemeManager.COLOR_PRIMARY_HOVER);
        dialog.add(titleLbl, gbc);

        gbc.gridy++;
        dialog.add(new JSeparator(JSeparator.HORIZONTAL), gbc);

        // Report Type Dropdown
        gbc.gridy++;
        dialog.add(new JLabel("Report Category:"), gbc);
        gbc.gridy++;
        String[] reportOptions = new String[]{
                "📊 Executive Analytics Report",
                "📁 Project Summary & Tasks",
                "📋 Comprehensive Task Directory",
                "👥 User & Security Role Directory",
                "🔥 Task Priority Audit",
                "📅 Deadlines & Milestone Schedule",
                "⚙️ Activity & System Audit Logs"
        };
        JComboBox<String> reportCombo = new JComboBox<>(reportOptions);
        dialog.add(reportCombo, gbc);

        // Project selector (shown if Project Summary is selected)
        gbc.gridy++;
        JLabel projSelLbl = new JLabel("Select Target Project:");
        dialog.add(projSelLbl, gbc);
        gbc.gridy++;
        JComboBox<ProjectItem> targetProjCombo = new JComboBox<>();
        for (Project p : projectsList) {
            targetProjCombo.addItem(new ProjectItem(p.getId(), p.getName()));
        }
        dialog.add(targetProjCombo, gbc);

        // Visibility listener for target project selector
        projSelLbl.setVisible(false);
        targetProjCombo.setVisible(false);
        reportCombo.addActionListener(e -> {
            boolean isProj = reportCombo.getSelectedIndex() == 1;
            projSelLbl.setVisible(isProj);
            targetProjCombo.setVisible(isProj);
            dialog.revalidate();
            dialog.repaint();
        });

        // Format selector
        gbc.gridy++;
        dialog.add(new JLabel("Export File Format:"), gbc);
        gbc.gridy++;
        JPanel formatPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        formatPanel.setOpaque(false);
        JRadioButton pdfRadio = new JRadioButton("📄 PDF Document (.pdf)", true);
        JRadioButton csvRadio = new JRadioButton("📊 CSV Spreadsheet (.csv)");
        ButtonGroup formatGroup = new ButtonGroup();
        formatGroup.add(pdfRadio);
        formatGroup.add(csvRadio);
        formatPanel.add(pdfRadio);
        formatPanel.add(csvRadio);
        dialog.add(formatPanel, gbc);

        gbc.gridy++;
        dialog.add(Box.createVerticalStrut(10), gbc);

        // Action Buttons
        gbc.gridy++;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton exportBtn = new JButton("Generate & Export");
        exportBtn.setBackground(ThemeManager.COLOR_PRIMARY);
        exportBtn.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        exportBtn.setFont(ThemeManager.FONT_BOLD_SMALL);

        exportBtn.addActionListener(e -> {
            int selIdx = reportCombo.getSelectedIndex();
            boolean isPdf = pdfRadio.isSelected();
            String ext = isPdf ? "pdf" : "csv";

            String baseName = switch (selIdx) {
                case 0 -> "IntelliFlow_Executive_Analytics";
                case 1 -> {
                    ProjectItem item = (ProjectItem) targetProjCombo.getSelectedItem();
                    yield "IntelliFlow_Project_" + (item != null ? item.name : "Summary");
                }
                case 2 -> "IntelliFlow_Task_Directory";
                case 3 -> "IntelliFlow_User_Directory";
                case 4 -> "IntelliFlow_Priority_Audit";
                case 5 -> "IntelliFlow_Deadline_Schedule";
                case 6 -> "IntelliFlow_Activity_Audit";
                default -> "IntelliFlow_Report";
            };

            String defaultFilename = CSVExporter.getSafeFilename(baseName, ext);
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Export " + (isPdf ? "PDF" : "CSV") + " Report");
            fileChooser.setSelectedFile(new File(defaultFilename));

            int result = fileChooser.showSaveDialog(dialog);
            if (result == JFileChooser.APPROVE_OPTION) {
                File targetFile = fileChooser.getSelectedFile();
                if (!targetFile.getName().toLowerCase().endsWith("." + ext)) {
                    targetFile = new File(targetFile.getAbsolutePath() + "." + ext);
                }

                final File fileToSave = targetFile;
                dialog.dispose();

                SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                    @Override
                    protected Boolean doInBackground() throws Exception {
                        User currentUser = UserSession.getInstance().getCurrentUser();
                        AnalyticsSummary summary = mainFrame.getReportService().getAnalyticsSummary(currentUser);
                        List<Project> allProjects = mainFrame.getProjectService().getAllProjects();
                        List<Task> allTasks = mainFrame.getTaskService().getAllTasks();
                        List<User> allUsers = mainFrame.getUserService().getAllUsers();

                        if (isPdf) {
                            switch (selIdx) {
                                case 0 -> PDFExporter.exportAnalyticsReport(summary, allProjects, fileToSave);
                                case 1 -> {
                                    ProjectItem item = (ProjectItem) targetProjCombo.getSelectedItem();
                                    int pId = item != null ? item.id : allProjects.get(0).getId();
                                    ProjectProgressReport pReport = mainFrame.getReportService().getProjectProgressReport(pId);
                                    List<Task> pTasks = mainFrame.getTaskService().getTasksByProject(pId);
                                    PDFExporter.exportProjectReport(pReport, pTasks, allUsers, fileToSave);
                                }
                                case 2 -> PDFExporter.exportTaskReport(allTasks, allProjects, allUsers, fileToSave);
                                case 3 -> PDFExporter.exportUserReport(allUsers, fileToSave);
                                case 4 -> PDFExporter.exportPriorityReport(allTasks, allProjects, allUsers, fileToSave);
                                case 5 -> PDFExporter.exportDeadlineReport(allTasks, allProjects, allUsers, fileToSave);
                                case 6 -> {
                                    List<ActivityLog> logs = mainFrame.getUserService().getActivityLogs();
                                    PDFExporter.exportActivityReport(logs, allUsers, fileToSave);
                                }
                            }
                        } else {
                            switch (selIdx) {
                                case 0 -> CSVExporter.exportAnalyticsReport(summary, allProjects, fileToSave);
                                case 1 -> {
                                    ProjectItem item = (ProjectItem) targetProjCombo.getSelectedItem();
                                    int pId = item != null ? item.id : allProjects.get(0).getId();
                                    ProjectProgressReport pReport = mainFrame.getReportService().getProjectProgressReport(pId);
                                    List<Task> pTasks = mainFrame.getTaskService().getTasksByProject(pId);
                                    CSVExporter.exportProjectReport(pReport, pTasks, allUsers, fileToSave);
                                }
                                case 2 -> CSVExporter.exportTaskReport(allTasks, allProjects, allUsers, fileToSave);
                                case 3 -> CSVExporter.exportUserReport(allUsers, fileToSave);
                                case 4 -> CSVExporter.exportPriorityReport(allTasks, allProjects, allUsers, fileToSave);
                                case 5 -> CSVExporter.exportDeadlineReport(allTasks, allProjects, allUsers, fileToSave);
                                case 6 -> {
                                    List<ActivityLog> logs = mainFrame.getUserService().getActivityLogs();
                                    CSVExporter.exportActivityReport(logs, allUsers, fileToSave);
                                }
                            }
                        }
                        return true;
                    }

                    @Override
                    protected void done() {
                        try {
                            get();
                            JOptionPane.showMessageDialog(ReportsView.this,
                                    "Report generated and saved successfully to:\n" + fileToSave.getAbsolutePath(),
                                    "Export Complete",
                                    JOptionPane.INFORMATION_MESSAGE);
                        } catch (Exception ex) {
                            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                            JOptionPane.showMessageDialog(ReportsView.this,
                                    "Failed to generate report: " + cause.getMessage(),
                                    "Export Error",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                };
                worker.execute();
            }
        });

        btnPanel.add(cancelBtn);
        btnPanel.add(exportBtn);
        dialog.add(btnPanel, gbc);

        dialog.setVisible(true);
    }
}

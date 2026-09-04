package com.intelliflow.ui.views;

import com.intelliflow.context.UserSession;
import com.intelliflow.enums.ProjectStatus;
import com.intelliflow.enums.ProjectHealth;
import com.intelliflow.util.ProjectHealthUtil;
import java.util.Collections;
import com.intelliflow.enums.TaskPriority;
import com.intelliflow.enums.TaskStatus;
import com.intelliflow.enums.Role;
import com.intelliflow.exception.ValidationException;
import com.intelliflow.model.Project;
import com.intelliflow.model.ProjectProgressReport;
import com.intelliflow.model.Task;
import com.intelliflow.model.User;
import com.intelliflow.model.ActivityLog;
import com.intelliflow.model.TeamMemberWorkload;
import com.intelliflow.util.WorkloadUtil;
import com.intelliflow.ui.MainFrame;
import com.intelliflow.ui.ThemeManager;
import com.intelliflow.ui.components.EmptyStatePanel;
import com.intelliflow.ui.components.RoundedPanel;
import com.intelliflow.ui.components.DashboardCard;
import com.intelliflow.ui.components.ModernTable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

public class ProjectManagementView extends BaseView {
    private final MainFrame mainFrame;

    // Layout Cards
    private CardLayout mainCardLayout;
    private JPanel mainCardContainer;

    // --- List View Components ---
    private JPanel listViewPanel;
    private JPanel gridContainer;
    private JScrollPane scrollPane;
    private EmptyStatePanel emptyPanel;
    private JTextField searchField;
    private JComboBox<Object> statusFilterCombo;
    private JComboBox<String> sortCombo;
    private JButton createButton;
    private JButton editButton;
    private JButton deleteButton;

    // --- Workspace View Components ---
    private JPanel workspaceViewPanel;

    // --- Shared Cache State ---
    private List<Project> allProjectsList = new ArrayList<>();
    private List<Project> displayedProjects = new ArrayList<>();
    private Map<Integer, Double> progressMap = new HashMap<>();
    private Map<Integer, Integer> taskCountMap = new HashMap<>();
    private Map<Integer, List<Task>> projectTasksCachedMap = new HashMap<>();
    private Map<Integer, String> managerNamesMap = new HashMap<>();
    private List<User> allUsersList = new ArrayList<>();
    private List<ActivityLog> allActivityLogs = new ArrayList<>();
    private final List<RoundedPanel> cardPanels = new ArrayList<>();
    private int selectedProjectId = -1;
    private Project selectedProject;

    public ProjectManagementView(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(ThemeManager.COLOR_BACKGROUND);

        mainCardLayout = new CardLayout();
        mainCardContainer = new JPanel(mainCardLayout);
        mainCardContainer.setOpaque(false);

        initListView();
        initWorkspaceView();

        mainCardContainer.add(listViewPanel, "list");
        mainCardContainer.add(workspaceViewPanel, "workspace");

        add(mainCardContainer, BorderLayout.CENTER);
        mainCardLayout.show(mainCardContainer, "list");
    }

    private void initListView() {
        listViewPanel = new JPanel(new BorderLayout(15, 15));
        listViewPanel.setOpaque(false);
        listViewPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Top Filters & Actions Bar
        JPanel topBar = new JPanel(new BorderLayout(10, 0));
        topBar.setOpaque(false);

        // Search + Filtering Group (Left side)
        JPanel filtersPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filtersPanel.setOpaque(false);

        filtersPanel.add(new JLabel("Search:"));
        searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(160, 28));
        searchField.putClientProperty("JTextField.placeholderText", "Search projects...");
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                applyFilters();
            }
        });
        filtersPanel.add(searchField);

        filtersPanel.add(new JLabel("Status:"));
        statusFilterCombo = new JComboBox<>();
        statusFilterCombo.setPreferredSize(new Dimension(140, 28));
        statusFilterCombo.addItem("All Statuses");
        for (ProjectStatus s : ProjectStatus.values()) {
            statusFilterCombo.addItem(s);
        }
        statusFilterCombo.addActionListener(e -> applyFilters());
        filtersPanel.add(statusFilterCombo);

        filtersPanel.add(new JLabel("Sort:"));
        sortCombo = new JComboBox<>(new String[]{
            "Sort by Name",
            "Sort by Start Date",
            "Sort by Deadline",
            "Sort by Status"
        });
        sortCombo.setPreferredSize(new Dimension(140, 28));
        sortCombo.addActionListener(e -> applyFilters());
        filtersPanel.add(sortCombo);

        JButton clearFiltersBtn = new JButton("Clear Filters");
        clearFiltersBtn.setBackground(ThemeManager.COLOR_CARD);
        clearFiltersBtn.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        clearFiltersBtn.setFont(ThemeManager.FONT_BOLD_SMALL);
        clearFiltersBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clearFiltersBtn.addActionListener(e -> {
            searchField.setText("");
            statusFilterCombo.setSelectedIndex(0);
            sortCombo.setSelectedIndex(0);
            applyFilters();
        });
        filtersPanel.add(clearFiltersBtn);

        topBar.add(filtersPanel, BorderLayout.WEST);

        // Actions Group (Right side)
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionsPanel.setOpaque(false);

        createButton = new JButton("➕ Create Project");
        createButton.setBackground(ThemeManager.COLOR_PRIMARY);
        createButton.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        createButton.setFont(ThemeManager.FONT_BOLD_SMALL);
        createButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        createButton.addActionListener(e -> showProjectForm(null));

        editButton = new JButton("📝 Edit Project");
        editButton.setBackground(ThemeManager.COLOR_CARD);
        editButton.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        editButton.setFont(ThemeManager.FONT_BOLD_SMALL);
        editButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        editButton.addActionListener(e -> handleEditAction());

        deleteButton = new JButton("🗑️ Delete Project");
        deleteButton.setBackground(ThemeManager.COLOR_DANGER);
        deleteButton.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        deleteButton.setFont(ThemeManager.FONT_BOLD_SMALL);
        deleteButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        deleteButton.addActionListener(e -> handleDeleteAction());

        actionsPanel.add(createButton);
        actionsPanel.add(editButton);
        actionsPanel.add(deleteButton);

        topBar.add(actionsPanel, BorderLayout.EAST);
        listViewPanel.add(topBar, BorderLayout.NORTH);

        // Cards Grid
        gridContainer = new JPanel();
        gridContainer.setOpaque(false);
        gridContainer.setLayout(new GridLayout(0, 3, 20, 20));

        scrollPane = new JScrollPane(gridContainer);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        listViewPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private void initWorkspaceView() {
        workspaceViewPanel = new JPanel(new BorderLayout(15, 15));
        workspaceViewPanel.setOpaque(false);
        workspaceViewPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
    }

    @Override
    public void refresh() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) return;

        boolean canWrite = (currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.MANAGER);
        createButton.setVisible(canWrite);
        editButton.setVisible(canWrite);
        deleteButton.setVisible(canWrite);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private List<Project> projects = new ArrayList<>();
            private final Map<Integer, Double> tempProgressMap = new HashMap<>();
            private final Map<Integer, Integer> tempTaskCountMap = new HashMap<>();
            private final Map<Integer, List<Task>> tempTasksCachedMap = new HashMap<>();
            private final Map<Integer, String> tempNamesMap = new HashMap<>();
            private List<User> tempUsersList = new ArrayList<>();
            private List<ActivityLog> tempActivityLogs = new ArrayList<>();

            @Override
            protected Void doInBackground() throws Exception {
                // Fetch projects based on role
                if (currentUser.getRole() == Role.ADMIN) {
                    projects = mainFrame.getProjectService().getAllProjects();
                } else if (currentUser.getRole() == Role.MANAGER) {
                    projects = mainFrame.getProjectService().getProjectsManagedBy(currentUser.getId());
                } else {
                    projects = mainFrame.getProjectService().getAllProjects();
                }

                tempUsersList = mainFrame.getUserService().getAllUsers();
                try {
                    tempActivityLogs = mainFrame.getUserService().getActivityLogs();
                } catch (Exception ignored) {}

                List<Task> allTasks = mainFrame.getTaskService().getAllTasks();

                // Group tasks by project
                Map<Integer, List<Task>> groupedTasks = allTasks.stream()
                        .collect(Collectors.groupingBy(Task::getProjectId));

                for (Project p : projects) {
                    List<Task> pTasks = groupedTasks.getOrDefault(p.getId(), new ArrayList<>());
                    tempTasksCachedMap.put(p.getId(), pTasks);
                    tempTaskCountMap.put(p.getId(), pTasks.size());

                    // Calculate real progress
                    int total = pTasks.size();
                    int completed = 0;
                    for (Task t : pTasks) {
                        if (t.getStatus() == TaskStatus.COMPLETED) {
                            completed++;
                        }
                    }
                    double progress = total > 0 ? (completed * 100.0 / total) : 0.0;
                    tempProgressMap.put(p.getId(), progress);

                    // Manager Name mapping
                    String managerName = "Unassigned";
                    if (p.getManagerId() != null) {
                        Optional<User> mgrOpt = tempUsersList.stream().filter(u -> u.getId() == p.getManagerId().intValue()).findFirst();
                        if (mgrOpt.isPresent()) {
                            managerName = mgrOpt.get().getFullName();
                        }
                    }
                    tempNamesMap.put(p.getId(), managerName);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    allProjectsList = projects;
                    progressMap = tempProgressMap;
                    taskCountMap = tempTaskCountMap;
                    projectTasksCachedMap = tempTasksCachedMap;
                    managerNamesMap = tempNamesMap;
                    allUsersList = tempUsersList;
                    allActivityLogs = tempActivityLogs;

                    applyFilters();

                    // If a specific project workspace is currently open, refresh it dynamically
                    if (selectedProject != null) {
                        // Retrieve updated project details from the fresh list
                        Optional<Project> updatedProj = allProjectsList.stream()
                                .filter(p -> p.getId() == selectedProject.getId())
                                .findFirst();
                        if (updatedProj.isPresent()) {
                            openWorkspace(updatedProj.get());
                        } else {
                            // Selected project was deleted, return to list
                            mainCardLayout.show(mainCardContainer, "list");
                            selectedProject = null;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to reload projects workspace data: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void applyFilters() {
        String query = searchField.getText().trim().toLowerCase();
        Object selectedStatus = statusFilterCombo.getSelectedItem();

        List<Project> filtered = allProjectsList.stream()
                .filter(p -> p.getName().toLowerCase().contains(query) ||
                        (p.getDescription() != null && p.getDescription().toLowerCase().contains(query)))
                .filter(p -> {
                    if (selectedStatus == null || selectedStatus.equals("All Statuses")) return true;
                    return p.getStatus() == selectedStatus;
                })
                .collect(Collectors.toList());

        // Sorting
        int sortIndex = sortCombo.getSelectedIndex();
        if (sortIndex == 0) {
            filtered.sort((p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()));
        } else if (sortIndex == 1) {
            filtered.sort((p1, p2) -> p1.getStartDate().compareTo(p2.getStartDate()));
        } else if (sortIndex == 2) {
            filtered.sort((p1, p2) -> p1.getDeadline().compareTo(p2.getDeadline()));
        } else if (sortIndex == 3) {
            filtered.sort((p1, p2) -> Integer.compare(getProjectStatusWeight(p1.getStatus()), getProjectStatusWeight(p2.getStatus())));
        }

        displayedProjects = filtered;
        populateGrid();
    }

    private int getProjectStatusWeight(ProjectStatus status) {
        if (status == null) return 0;
        return switch (status) {
            case PLANNED -> 1;
            case ACTIVE -> 2;
            case ON_HOLD -> 3;
            case COMPLETED -> 4;
            case CANCELLED -> 5;
        };
    }

    private void populateGrid() {
        gridContainer.removeAll();
        cardPanels.clear();

        User currentUser = UserSession.getInstance().getCurrentUser();

        if (displayedProjects.isEmpty()) {
            gridContainer.setLayout(new BorderLayout());
            String title = allProjectsList.isEmpty() ? "No projects available." : "No matching projects found.";
            String subtitle = allProjectsList.isEmpty() ? "Organize tasks by creating your first team workspace project." : "Try changing your search or filters.";
            
            boolean isWriteable = currentUser != null && (currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.MANAGER);
            EmptyStatePanel emptyListPanel = new EmptyStatePanel(
                allProjectsList.isEmpty() ? "📁" : "🔍",
                title,
                subtitle,
                (allProjectsList.isEmpty() && isWriteable) ? "➕ Create Project" : (allProjectsList.isEmpty() ? null : "Clear Filters"),
                (allProjectsList.isEmpty() && isWriteable) ? e -> showProjectForm(null) : (allProjectsList.isEmpty() ? null : e -> {
                    searchField.setText("");
                    statusFilterCombo.setSelectedIndex(0);
                    sortCombo.setSelectedIndex(0);
                    applyFilters();
                })
            );
            gridContainer.add(emptyListPanel, BorderLayout.CENTER);
            gridContainer.revalidate();
            gridContainer.repaint();
            return;
        } else {
            gridContainer.setLayout(new GridLayout(0, 3, 20, 20));
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Project p : displayedProjects) {
            RoundedPanel card = new RoundedPanel(12, ThemeManager.COLOR_CARD);
            card.setDrawBorder(true);
            card.setBorderColor(p.getId() == selectedProjectId ? ThemeManager.COLOR_PRIMARY : ThemeManager.COLOR_BORDER);
            card.setLayout(new BorderLayout(10, 10));
            card.setBorder(new EmptyBorder(15, 15, 15, 15));
            card.setCursor(new Cursor(Cursor.HAND_CURSOR));

            card.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (p.getId() != selectedProjectId) {
                        card.setBorderColor(ThemeManager.COLOR_PRIMARY_HOVER);
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (p.getId() != selectedProjectId) {
                        card.setBorderColor(ThemeManager.COLOR_BORDER);
                    } else {
                        card.setBorderColor(ThemeManager.COLOR_PRIMARY);
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    selectedProjectId = p.getId();
                    updateCardSelectionVisuals();

                    if (e.getClickCount() == 2) {
                        openWorkspace(p);
                    }
                }
            });

            // Header (Project Name & Status/Health badges)
            JPanel headerPanel = new JPanel(new BorderLayout(8, 0));
            headerPanel.setOpaque(false);

            JLabel nameLabel = new JLabel(p.getName());
            nameLabel.setFont(ThemeManager.FONT_SUBTITLE);
            nameLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
            headerPanel.add(nameLabel, BorderLayout.CENTER);

            List<Task> pTasks = projectTasksCachedMap.getOrDefault(p.getId(), Collections.emptyList());
            ProjectHealth health = ProjectHealthUtil.calculateProjectHealth(p, pTasks);
            Color healthColor = switch (health) {
                case ON_TRACK -> ThemeManager.COLOR_SUCCESS;
                case AT_RISK -> ThemeManager.COLOR_WARNING;
                case DELAYED -> ThemeManager.COLOR_DANGER;
            };

            JPanel badgesPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            badgesPanel.setOpaque(false);
            badgesPanel.add(new PillBadge(health.getDisplayName(), healthColor, Color.WHITE, 6));
            badgesPanel.add(new PillBadge(p.getStatus().toString(), getStatusColor(p.getStatus()), Color.WHITE, 6));

            headerPanel.add(badgesPanel, BorderLayout.EAST);

            card.add(headerPanel, BorderLayout.NORTH);

            // Center details (Description clamped + real task count & progress)
            JPanel bodyPanel = new JPanel(new GridLayout(3, 1, 5, 5));
            bodyPanel.setOpaque(false);

            String desc = p.getDescription() != null && !p.getDescription().isEmpty() ? p.getDescription() : "No description provided.";
            if (desc.length() > 80) desc = desc.substring(0, 77) + "...";

            JLabel descLabel = new JLabel("<html><p style=\"width:180px;\">" + desc + "</p></html>");
            descLabel.setFont(ThemeManager.FONT_BODY);
            descLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);
            bodyPanel.add(descLabel);

            // Task Count info
            int tCount = taskCountMap.getOrDefault(p.getId(), 0);
            JLabel taskCountLabel = new JLabel("📋 Tasks: " + tCount);
            taskCountLabel.setFont(ThemeManager.FONT_SMALL);
            taskCountLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
            bodyPanel.add(taskCountLabel);

            // Progress panel
            double progress = progressMap.getOrDefault(p.getId(), 0.0);
            JPanel progressContainer = new JPanel(new BorderLayout(5, 2));
            progressContainer.setOpaque(false);

            String progressText = getProgressBlockString(progress);
            JLabel progressLabel = new JLabel(progressText);
            progressLabel.setFont(ThemeManager.FONT_SMALL);
            progressLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);
            progressContainer.add(progressLabel, BorderLayout.NORTH);

            JProgressBar progressBar = new JProgressBar(0, 100);
            progressBar.setValue((int) Math.round(progress));
            progressBar.setForeground(ThemeManager.COLOR_SUCCESS);
            progressContainer.add(progressBar, BorderLayout.CENTER);

            bodyPanel.add(progressContainer);
            card.add(bodyPanel, BorderLayout.CENTER);

            // Footer metadata (Manager + Date timeline)
            JPanel footerPanel = new JPanel(new BorderLayout(5, 5));
            footerPanel.setOpaque(false);

            JLabel managerLabel = new JLabel("Mgr: " + managerNamesMap.getOrDefault(p.getId(), "Unassigned"));
            managerLabel.setFont(ThemeManager.FONT_SMALL);
            managerLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);
            footerPanel.add(managerLabel, BorderLayout.WEST);

            String timeline = p.getStartDate().format(formatter) + " ➔ " + p.getDeadline().format(formatter);
            JLabel dateLabel = new JLabel("📅 " + timeline);
            dateLabel.setFont(ThemeManager.FONT_SMALL);
            dateLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);
            footerPanel.add(dateLabel, BorderLayout.EAST);

            card.add(footerPanel, BorderLayout.SOUTH);

            gridContainer.add(card);
            cardPanels.add(card);
        }

        gridContainer.revalidate();
        gridContainer.repaint();
    }

    private void updateCardSelectionVisuals() {
        for (int i = 0; i < displayedProjects.size(); i++) {
            Project p = displayedProjects.get(i);
            if (i < cardPanels.size()) {
                cardPanels.get(i).setBorderColor(p.getId() == selectedProjectId ? ThemeManager.COLOR_PRIMARY : ThemeManager.COLOR_BORDER);
            }
        }
    }

    private String getProgressBlockString(double progressPercentage) {
        int totalBlocks = 16;
        int filledBlocks = (int) Math.round(progressPercentage * totalBlocks / 100.0);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < totalBlocks; i++) {
            if (i < filledBlocks) {
                sb.append("█");
            } else {
                sb.append("░");
            }
        }
        return sb.toString() + " " + (int) Math.round(progressPercentage) + "%";
    }

    private Color getStatusColor(ProjectStatus status) {
        return switch (status) {
            case PLANNED -> new Color(148, 163, 184); // Slate Grey
            case ACTIVE -> new Color(59, 130, 246);   // Blue
            case COMPLETED -> ThemeManager.COLOR_SUCCESS;
            case ON_HOLD -> ThemeManager.COLOR_WARNING;
            case CANCELLED -> ThemeManager.COLOR_DANGER;
        };
    }

    private Color getTaskStatusColor(TaskStatus status) {
        return switch (status) {
            case TO_DO -> new Color(148, 163, 184);
            case IN_PROGRESS -> new Color(59, 130, 246);
            case TESTING -> new Color(245, 158, 11);
            case COMPLETED -> ThemeManager.COLOR_SUCCESS;
            case BLOCKED -> ThemeManager.COLOR_DANGER;
        };
    }

    private void handleEditAction() {
        if (selectedProjectId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a project card to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Project projectToEdit = allProjectsList.stream().filter(p -> p.getId() == selectedProjectId).findFirst().orElse(null);
        if (projectToEdit != null) {
            showProjectForm(projectToEdit);
        }
    }

    private void handleDeleteAction() {
        if (selectedProjectId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a project card to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Project projectToDelete = allProjectsList.stream().filter(p -> p.getId() == selectedProjectId).findFirst().orElse(null);
        if (projectToDelete == null) return;

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete project: '" + projectToDelete.getName() + "'?\nThis will cascadingly delete all tasks and notifications linked to it.",
                "Confirm Project Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    mainFrame.getProjectService().deleteProject(selectedProjectId);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        selectedProjectId = -1;
                        refresh();
                        mainFrame.updateNotificationCount();
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(ProjectManagementView.this, "Failed to delete project: DB error.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }

    private void openWorkspace(Project p) {
        selectedProject = p;
        workspaceViewPanel.removeAll();

        // 1. Back button & Edit actions row
        JPanel navigationRow = new JPanel(new BorderLayout());
        navigationRow.setOpaque(false);

        JButton backBtn = new JButton("← Back to Projects");
        backBtn.setFont(ThemeManager.FONT_BOLD_SMALL);
        backBtn.setBackground(ThemeManager.COLOR_CARD);
        backBtn.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> {
            selectedProject = null;
            mainCardLayout.show(mainCardContainer, "list");
            refresh();
        });
        navigationRow.add(backBtn, BorderLayout.WEST);

        User currentUser = UserSession.getInstance().getCurrentUser();
        boolean isManagerOrAdmin = currentUser != null && (currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.MANAGER);
        if (isManagerOrAdmin) {
            JPanel editPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            editPanel.setOpaque(false);

            JButton editProjectBtn = new JButton("📝 Edit Project");
            editProjectBtn.setFont(ThemeManager.FONT_BOLD_SMALL);
            editProjectBtn.setBackground(ThemeManager.COLOR_PRIMARY);
            editProjectBtn.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
            editProjectBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            editProjectBtn.addActionListener(e -> showProjectForm(p));

            editPanel.add(editProjectBtn);
            navigationRow.add(editPanel, BorderLayout.EAST);
        }
        workspaceViewPanel.add(navigationRow, BorderLayout.NORTH);

        // 2. Main Workspace Layout
        JPanel workspaceContent = new JPanel(new BorderLayout(15, 15));
        workspaceContent.setOpaque(false);

        // Header section: Name, Desc, Status, Dates, Progress
        JPanel headerPanel = new RoundedPanel(12, ThemeManager.COLOR_CARD);
        headerPanel.setLayout(new GridBagLayout());
        headerPanel.setBorder(new EmptyBorder(18, 20, 18, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 8, 0);

        JLabel workspaceTitle = new JLabel(p.getName());
        workspaceTitle.setFont(ThemeManager.FONT_TITLE);
        workspaceTitle.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        headerPanel.add(workspaceTitle, gbc);

        gbc.gridy++;
        String desc = p.getDescription() != null && !p.getDescription().isEmpty() ? p.getDescription() : "No description provided.";
        JLabel workspaceDesc = new JLabel("<html>" + desc + "</html>");
        workspaceDesc.setFont(ThemeManager.FONT_BODY);
        workspaceDesc.setForeground(ThemeManager.COLOR_TEXT_MUTED);
        headerPanel.add(workspaceDesc, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(8, 0, 8, 0);
        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        statusRow.setOpaque(false);

        statusRow.add(new JLabel("Status:"));
        statusRow.add(new PillBadge(p.getStatus().toString(), getStatusColor(p.getStatus()), Color.WHITE, 8));

        // Progress breakdown calculations
        List<Task> pTasks = projectTasksCachedMap.getOrDefault(p.getId(), new ArrayList<>());
        ProjectHealth health = ProjectHealthUtil.calculateProjectHealth(p, pTasks);
        Color healthColor = switch (health) {
            case ON_TRACK -> ThemeManager.COLOR_SUCCESS;
            case AT_RISK -> ThemeManager.COLOR_WARNING;
            case DELAYED -> ThemeManager.COLOR_DANGER;
        };

        statusRow.add(new JLabel("  Health:"));
        statusRow.add(new PillBadge(health.getDisplayName(), healthColor, Color.WHITE, 8));

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String datesTimeline = p.getStartDate().format(dtf) + "  ➔  " + p.getDeadline().format(dtf);
        statusRow.add(new JLabel("  📅 Dates: " + datesTimeline));
        headerPanel.add(statusRow, gbc);

        int totalTasks = pTasks.size();
        int completedTasks = 0;
        int activeTasks = 0;
        int todoCount = 0;
        int inProgressCount = 0;
        int testingCount = 0;
        int blockedCount = 0;

        for (Task t : pTasks) {
            switch (t.getStatus()) {
                case TO_DO -> { todoCount++; activeTasks++; }
                case IN_PROGRESS -> { inProgressCount++; activeTasks++; }
                case TESTING -> { testingCount++; activeTasks++; }
                case COMPLETED -> { completedTasks++; }
                case BLOCKED -> { blockedCount++; activeTasks++; }
            }
        }
        double progressVal = totalTasks > 0 ? (completedTasks * 100.0 / totalTasks) : 0.0;

        gbc.gridy++;
        gbc.insets = new Insets(8, 0, 0, 0);
        JPanel progressPanel = new JPanel(new BorderLayout(10, 4));
        progressPanel.setOpaque(false);

        JLabel progressLabel = new JLabel("Project Progress: " + getProgressBlockString(progressVal));
        progressLabel.setFont(ThemeManager.FONT_BOLD_SMALL);
        progressLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        progressPanel.add(progressLabel, BorderLayout.NORTH);

        JProgressBar workspaceProgressBar = new JProgressBar(0, 100);
        workspaceProgressBar.setValue((int) Math.round(progressVal));
        workspaceProgressBar.setForeground(ThemeManager.COLOR_SUCCESS);
        progressPanel.add(workspaceProgressBar, BorderLayout.CENTER);
        headerPanel.add(progressPanel, gbc);

        workspaceContent.add(headerPanel, BorderLayout.NORTH);

        // 3. Tabbed Pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(ThemeManager.FONT_BOLD_SMALL);

        // -- Tab 1: Overview --
        JPanel overviewTab = new JPanel(new BorderLayout(15, 15));
        overviewTab.setOpaque(false);
        overviewTab.setBorder(new EmptyBorder(15, 15, 15, 15));

        // KPI Cards Grid (5 columns)
        JPanel kpiPanel = new JPanel(new GridLayout(1, 5, 15, 15));
        kpiPanel.setOpaque(false);
        kpiPanel.setPreferredSize(new Dimension(800, 110));

        String managerName = managerNamesMap.getOrDefault(p.getId(), "Unassigned");
        kpiPanel.add(new DashboardCard("📋", "Total Tasks", String.valueOf(totalTasks), ThemeManager.COLOR_PRIMARY));
        kpiPanel.add(new DashboardCard("✓", "Completed", String.valueOf(completedTasks), ThemeManager.COLOR_SUCCESS));
        kpiPanel.add(new DashboardCard("⏳", "Active Tasks", String.valueOf(activeTasks), ThemeManager.COLOR_WARNING));
        kpiPanel.add(new DashboardCard("🩺", "Project Health", health.getDisplayName(), healthColor));
        kpiPanel.add(new DashboardCard("👤", "Manager", managerName, ThemeManager.COLOR_PRIMARY_HOVER));
        overviewTab.add(kpiPanel, BorderLayout.NORTH);

        // Task Status Breakdown row list
        JPanel breakdownWrapper = new RoundedPanel(12, ThemeManager.COLOR_CARD);
        breakdownWrapper.setLayout(new BorderLayout());
        breakdownWrapper.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel breakdownTitle = new JLabel("Task Status Statistics");
        breakdownTitle.setFont(ThemeManager.FONT_SUBTITLE);
        breakdownTitle.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        breakdownTitle.setBorder(new EmptyBorder(0, 0, 12, 0));
        breakdownWrapper.add(breakdownTitle, BorderLayout.NORTH);

        JPanel breakdownList = new JPanel();
        breakdownList.setOpaque(false);
        breakdownList.setLayout(new BoxLayout(breakdownList, BoxLayout.Y_AXIS));

        breakdownList.add(createStatusBreakdownRow("📋 TO DO", todoCount, totalTasks, new Color(148, 163, 184)));
        breakdownList.add(createStatusBreakdownRow("⚡ IN PROGRESS", inProgressCount, totalTasks, new Color(59, 130, 246)));
        breakdownList.add(createStatusBreakdownRow("🧪 TESTING", testingCount, totalTasks, new Color(245, 158, 11)));
        breakdownList.add(createStatusBreakdownRow("✓ COMPLETED", completedTasks, totalTasks, ThemeManager.COLOR_SUCCESS));
        breakdownList.add(createStatusBreakdownRow("🚫 BLOCKED", blockedCount, totalTasks, ThemeManager.COLOR_DANGER));

        breakdownWrapper.add(breakdownList, BorderLayout.CENTER);
        overviewTab.add(breakdownWrapper, BorderLayout.CENTER);

        tabbedPane.addTab("Overview", overviewTab);

        // -- Tab 2: Tasks --
        JPanel tasksTab = new JPanel(new BorderLayout(10, 10));
        tasksTab.setOpaque(false);
        tasksTab.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Tasks Actions Bar
        JPanel tasksActionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        tasksActionBar.setOpaque(false);

        JButton viewTaskBtn = new JButton("👁️ View Details");
        viewTaskBtn.setBackground(ThemeManager.COLOR_CARD);
        viewTaskBtn.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        viewTaskBtn.setFont(ThemeManager.FONT_BOLD_SMALL);

        JButton addTaskBtn = new JButton("➕ Create Task");
        addTaskBtn.setBackground(ThemeManager.COLOR_PRIMARY);
        addTaskBtn.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        addTaskBtn.setFont(ThemeManager.FONT_BOLD_SMALL);
        addTaskBtn.setVisible(isManagerOrAdmin);

        JButton editTaskBtn = new JButton("✏️ Edit Task");
        editTaskBtn.setBackground(ThemeManager.COLOR_CARD);
        editTaskBtn.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        editTaskBtn.setFont(ThemeManager.FONT_BOLD_SMALL);
        editTaskBtn.setVisible(isManagerOrAdmin);

        JButton deleteTaskBtn = new JButton("🗑️ Delete Task");
        deleteTaskBtn.setBackground(ThemeManager.COLOR_DANGER);
        deleteTaskBtn.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        deleteTaskBtn.setFont(ThemeManager.FONT_BOLD_SMALL);
        deleteTaskBtn.setVisible(isManagerOrAdmin);

        JButton statusTaskBtn = new JButton("🔄 Change Status");
        statusTaskBtn.setBackground(ThemeManager.COLOR_CARD);
        statusTaskBtn.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        statusTaskBtn.setFont(ThemeManager.FONT_BOLD_SMALL);

        tasksActionBar.add(viewTaskBtn);
        tasksActionBar.add(addTaskBtn);
        tasksActionBar.add(editTaskBtn);
        tasksActionBar.add(deleteTaskBtn);
        tasksActionBar.add(statusTaskBtn);
        tasksTab.add(tasksActionBar, BorderLayout.NORTH);

        // Tasks Table
        DefaultTableModel taskModel = new DefaultTableModel(
                new Object[]{"ID", "Task Name", "Priority", "Assigned To", "Deadline", "Status"}, 0
        );
        ModernTable taskTable = new ModernTable();
        taskTable.setPlaceholderText("No tasks associated with this project.");
        taskTable.setModel(taskModel);

        for (Task t : pTasks) {
            String empAssignedName = "Unassigned";
            if (t.getAssignedEmployeeId() != null) {
                Optional<User> uOpt = allUsersList.stream().filter(u -> u.getId() == t.getAssignedEmployeeId().intValue()).findFirst();
                if (uOpt.isPresent()) {
                    empAssignedName = uOpt.get().getFullName();
                }
            }
            taskModel.addRow(new Object[]{
                    t.getId(),
                    t.getName(),
                    t.getPriority().toString(),
                    empAssignedName,
                    t.getDeadline() != null ? t.getDeadline().toString() : "No Deadline",
                    t.getStatus().toString()
            });
        }

        JScrollPane taskScroll = new JScrollPane(taskTable);
        taskScroll.setBorder(BorderFactory.createEmptyBorder());
        tasksTab.add(taskScroll, BorderLayout.CENTER);

        // Action Handlers for Tasks Tab
        viewTaskBtn.addActionListener(e -> {
            int selectedRow = taskTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Select a task row to view details.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int taskId = (int) taskTable.getValueAt(selectedRow, 0);
            pTasks.stream().filter(t -> t.getId() == taskId).findFirst().ifPresent(this::showTaskDetails);
        });

        addTaskBtn.addActionListener(e -> showTaskForm(null, p));

        editTaskBtn.addActionListener(e -> {
            int selectedRow = taskTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Select a task row to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int taskId = (int) taskTable.getValueAt(selectedRow, 0);
            pTasks.stream().filter(t -> t.getId() == taskId).findFirst().ifPresent(t -> showTaskForm(t, p));
        });

        deleteTaskBtn.addActionListener(e -> {
            int selectedRow = taskTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Select a task row to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int taskId = (int) taskTable.getValueAt(selectedRow, 0);
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to delete task ID: " + taskId + "?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                SwingWorker<Void, Void> taskDeleteWorker = new SwingWorker<>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        mainFrame.getTaskService().deleteTask(taskId);
                        return null;
                    }

                    @Override
                    protected void done() {
                        try {
                            get();
                            refresh();
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(ProjectManagementView.this, "Failed to delete task.", "Database Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                };
                taskDeleteWorker.execute();
            }
        });

        statusTaskBtn.addActionListener(e -> {
            int selectedRow = taskTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Select a task row to change status.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int taskId = (int) taskTable.getValueAt(selectedRow, 0);
            Task selectedTask = pTasks.stream().filter(t -> t.getId() == taskId).findFirst().orElse(null);
            if (selectedTask == null) return;

            // Check Employee role restrictions
            boolean isAssignedEmployee = currentUser != null && currentUser.getRole() == Role.EMPLOYEE &&
                    selectedTask.getAssignedEmployeeId() != null && selectedTask.getAssignedEmployeeId() == currentUser.getId();

            if (!isManagerOrAdmin && !isAssignedEmployee) {
                JOptionPane.showMessageDialog(this, "You can only update status for tasks assigned to you.", "Action Denied", JOptionPane.ERROR_MESSAGE);
                return;
            }

            showStatusTransitionMenu(selectedTask, statusTaskBtn);
        });

        tabbedPane.addTab("Tasks", tasksTab);

        // -- Tab 3: Team & Workload --
        JPanel teamTab = new JPanel(new BorderLayout(10, 15));
        teamTab.setOpaque(false);
        teamTab.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel teamHeaderPanel = new JPanel(new BorderLayout());
        teamHeaderPanel.setOpaque(false);
        JLabel teamTitle = new JLabel("Project Team Workload & Performance");
        teamTitle.setFont(ThemeManager.FONT_SUBTITLE);
        teamTitle.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        teamHeaderPanel.add(teamTitle, BorderLayout.WEST);
        teamTab.add(teamHeaderPanel, BorderLayout.NORTH);

        DefaultTableModel teamModel = new DefaultTableModel(
                new Object[]{"Employee Name", "Assigned", "Completed", "In Progress", "Overdue", "Completion %", "Workload Status"}, 0
        );
        ModernTable teamTable = new ModernTable();
        teamTable.setPlaceholderText("No team members assigned to tasks in this project.");
        teamTable.setModel(teamModel);

        LocalDate today = LocalDate.now();
        List<TeamMemberWorkload> memberWorkloads = WorkloadUtil.calculateTeamWorkloadForProject(allUsersList, pTasks, today);

        for (TeamMemberWorkload mw : memberWorkloads) {
            teamModel.addRow(new Object[]{
                    mw.getEmployeeName(),
                    mw.getAssignedTasks() + " assigned",
                    mw.getCompletedTasks() + " completed",
                    mw.getInProgressTasks() + " in progress",
                    mw.getOverdueTasks() > 0 ? "⛔ " + mw.getOverdueTasks() + " overdue" : "0 overdue",
                    mw.getCompletionPercentage() + "%",
                    mw.getWorkloadIndicator()
            });
        }

        JScrollPane teamScroll = new JScrollPane(teamTable);
        teamScroll.setBorder(BorderFactory.createEmptyBorder());
        teamTab.add(teamScroll, BorderLayout.CENTER);

        tabbedPane.addTab("Team & Workload", teamTab);

        // -- Tab 4: Activity --
        JPanel activityTab = new JPanel(new BorderLayout(10, 10));
        activityTab.setOpaque(false);
        activityTab.setBorder(new EmptyBorder(15, 15, 15, 15));

        DefaultTableModel activityModel = new DefaultTableModel(
                new Object[]{"Timestamp", "User", "Action", "Audit Log Description"}, 0
        );
        ModernTable activityTable = new ModernTable();
        activityTable.setPlaceholderText("No activity log entries found for this project.");
        activityTable.setModel(activityModel);

        // Filter logs related to this project or its tasks
        List<ActivityLog> projectLogs = allActivityLogs.stream()
                .filter(log -> {
                    if (log.getDescription() == null) return false;
                    boolean matchesProject = log.getDescription().contains(p.getName()) || log.getDescription().contains("ID: " + p.getId());
                    if (matchesProject) return true;

                    for (Task t : pTasks) {
                        if (log.getDescription().contains(t.getName()) || log.getDescription().contains("ID: " + t.getId())) {
                            return true;
                        }
                    }
                    return false;
                })
                .collect(Collectors.toList());

        DateTimeFormatter auditFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (ActivityLog log : projectLogs) {
            String userRepresentation = "SYSTEM";
            if (log.getUserId() != null) {
                Optional<User> uOpt = allUsersList.stream().filter(u -> u.getId() == log.getUserId().intValue()).findFirst();
                if (uOpt.isPresent()) {
                    userRepresentation = uOpt.get().getUsername() + " (" + uOpt.get().getFullName() + ")";
                } else {
                    userRepresentation = "User ID: " + log.getUserId();
                }
            }
            activityModel.addRow(new Object[]{
                    log.getTimestamp() != null ? log.getTimestamp().format(auditFormatter) : "",
                    userRepresentation,
                    log.getAction(),
                    log.getDescription()
            });
        }

        JScrollPane activityScroll = new JScrollPane(activityTable);
        activityScroll.setBorder(BorderFactory.createEmptyBorder());
        activityTab.add(activityScroll, BorderLayout.CENTER);

        tabbedPane.addTab("Activity", activityTab);

        workspaceContent.add(tabbedPane, BorderLayout.CENTER);
        workspaceViewPanel.add(workspaceContent, BorderLayout.CENTER);

        workspaceViewPanel.revalidate();
        workspaceViewPanel.repaint();
        mainCardLayout.show(mainCardContainer, "workspace");
    }

    private JPanel createStatusBreakdownRow(String statusLabel, int count, int total, Color color) {
        JPanel row = new JPanel(new BorderLayout(15, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(8, 0, 8, 0));

        JLabel nameLabel = new JLabel(statusLabel);
        nameLabel.setFont(ThemeManager.FONT_BOLD_SMALL);
        nameLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        nameLabel.setPreferredSize(new Dimension(140, 20));
        row.add(nameLabel, BorderLayout.WEST);

        int pct = total > 0 ? (int) Math.round(count * 100.0 / total) : 0;
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(pct);
        bar.setForeground(color);
        bar.setPreferredSize(new Dimension(200, 14));
        row.add(bar, BorderLayout.CENTER);

        JLabel countLabel = new JLabel(count + " tasks (" + pct + "%)", SwingConstants.RIGHT);
        countLabel.setFont(ThemeManager.FONT_SMALL);
        countLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);
        countLabel.setPreferredSize(new Dimension(120, 20));
        row.add(countLabel, BorderLayout.EAST);

        return row;
    }

    public void showProjectForm(Project project) {
        boolean isEdit = (project != null);
        JDialog dialog = new JDialog(mainFrame, isEdit ? "Modify Project Details" : "Create New Project", true);
        dialog.setSize(480, 520);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 20, 10, 20);
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // Project Name
        dialog.add(new JLabel("Project Name:"), gbc);
        gbc.gridy++;
        JTextField nameField = new JTextField(isEdit ? project.getName() : "");
        nameField.setPreferredSize(new Dimension(300, 32));
        nameField.putClientProperty("JTextField.placeholderText", "Enter project name");
        dialog.add(nameField, gbc);

        // Description
        gbc.gridy++;
        dialog.add(new JLabel("Description:"), gbc);
        gbc.gridy++;
        JTextArea descArea = new JTextArea(isEdit ? project.getDescription() : "", 4, 20);
        descArea.putClientProperty("JTextField.placeholderText", "Enter project description...");
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(descArea);
        dialog.add(descScroll, gbc);

        // Manager Combo Box
        gbc.gridy++;
        dialog.add(new JLabel("Assigned Manager:"), gbc);
        gbc.gridy++;

        List<User> managers = allUsersList.stream()
                .filter(u -> u.getRole() == Role.MANAGER || u.getRole() == Role.ADMIN)
                .collect(Collectors.toList());

        JComboBox<User> managerCombo = new JComboBox<>(managers.toArray(new User[0]));

        if (isEdit && project.getManagerId() != null) {
            for (int i = 0; i < managerCombo.getItemCount(); i++) {
                if (managerCombo.getItemAt(i).getId() == project.getManagerId().intValue()) {
                    managerCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
        dialog.add(managerCombo, gbc);

        // Dates Grid Layout
        gbc.gridy++;
        JPanel datesPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        datesPanel.setOpaque(false);

        datesPanel.add(new JLabel("Start Date (YYYY-MM-DD):"));
        datesPanel.add(new JLabel("Deadline (YYYY-MM-DD):"));

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        JTextField startField = new JTextField(isEdit ? project.getStartDate().format(dtf) : LocalDate.now().format(dtf));
        JTextField deadlineField = new JTextField(isEdit ? project.getDeadline().format(dtf) : LocalDate.now().plusDays(14).format(dtf));
        startField.putClientProperty("JTextField.placeholderText", "YYYY-MM-DD");
        deadlineField.putClientProperty("JTextField.placeholderText", "YYYY-MM-DD");
        datesPanel.add(startField);
        datesPanel.add(deadlineField);
        dialog.add(datesPanel, gbc);

        // Status Combo Box
        gbc.gridy++;
        dialog.add(new JLabel("Status:"), gbc);
        gbc.gridy++;
        JComboBox<ProjectStatus> statusCombo = new JComboBox<>(ProjectStatus.values());
        if (isEdit) {
            statusCombo.setSelectedItem(project.getStatus());
        } else {
            statusCombo.setSelectedItem(ProjectStatus.PLANNED);
        }
        dialog.add(statusCombo, gbc);

        // Buttons
        gbc.gridy++;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton saveBtn = new JButton(isEdit ? "Update" : "Create");
        saveBtn.setBackground(ThemeManager.COLOR_PRIMARY);
        saveBtn.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);

        saveBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String description = descArea.getText().trim();
            User selectedMgr = (User) managerCombo.getSelectedItem();
            String startStr = startField.getText().trim();
            String deadlineStr = deadlineField.getText().trim();
            ProjectStatus status = (ProjectStatus) statusCombo.getSelectedItem();

            try {
                LocalDate start = LocalDate.parse(startStr, dtf);
                LocalDate deadline = LocalDate.parse(deadlineStr, dtf);

                Project p = isEdit ? project : new Project();
                p.setName(name);
                p.setDescription(description);
                p.setManagerId(selectedMgr != null ? selectedMgr.getId() : null);
                p.setStartDate(start);
                p.setDeadline(deadline);
                p.setStatus(status);

                if (isEdit) {
                    mainFrame.getProjectService().updateProject(p);
                    JOptionPane.showMessageDialog(dialog, "Project updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    mainFrame.getProjectService().createProject(p);
                    JOptionPane.showMessageDialog(dialog, "Project created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                }

                dialog.dispose();
                refresh();
            } catch (java.time.format.DateTimeParseException ex) {
                JOptionPane.showMessageDialog(dialog, "Dates must follow the format YYYY-MM-DD (e.g. 2026-09-15).", "Date Format Error", JOptionPane.ERROR_MESSAGE);
            } catch (ValidationException ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Validation Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "An unexpected database connection error occurred.", "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        buttonPanel.add(cancelBtn);
        buttonPanel.add(saveBtn);
        dialog.add(buttonPanel, gbc);

        dialog.setVisible(true);
    }

    private void showTaskForm(Task task, Project defaultProject) {
        boolean isEdit = (task != null);
        JDialog dialog = new JDialog(mainFrame, isEdit ? "Modify Task Details" : "Create New Task", true);
        dialog.setSize(480, 560);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = gbc.HORIZONTAL;
        gbc.insets = new Insets(10, 20, 10, 20);
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // Task Name
        dialog.add(new JLabel("Task Name:"), gbc);
        gbc.gridy++;
        JTextField nameField = new JTextField(isEdit ? task.getName() : "");
        nameField.setPreferredSize(new Dimension(300, 32));
        nameField.putClientProperty("JTextField.placeholderText", "Enter task name");
        dialog.add(nameField, gbc);

        // Description
        gbc.gridy++;
        dialog.add(new JLabel("Description:"), gbc);
        gbc.gridy++;
        JTextArea descArea = new JTextArea(isEdit ? task.getDescription() : "", 4, 20);
        descArea.putClientProperty("JTextField.placeholderText", "Enter task description...");
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(descArea);
        dialog.add(descScroll, gbc);

        // Project Combo Box (Locked to current project)
        gbc.gridy++;
        dialog.add(new JLabel("Associated Project:"), gbc);
        gbc.gridy++;
        JComboBox<Project> projectFormCombo = new JComboBox<>(new Project[]{defaultProject});
        projectFormCombo.setEnabled(false);
        dialog.add(projectFormCombo, gbc);

        // Assignee Combo Box
        gbc.gridy++;
        dialog.add(new JLabel("Assign Employee:"), gbc);
        gbc.gridy++;

        List<User> employees = allUsersList.stream()
                .filter(u -> u.getRole() == Role.EMPLOYEE)
                .collect(Collectors.toList());

        JComboBox<User> assigneeFormCombo = new JComboBox<>(employees.toArray(new User[0]));
        if (isEdit && task.getAssignedEmployeeId() != null) {
            for (int i = 0; i < assigneeFormCombo.getItemCount(); i++) {
                if (assigneeFormCombo.getItemAt(i).getId() == task.getAssignedEmployeeId().intValue()) {
                    assigneeFormCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
        dialog.add(assigneeFormCombo, gbc);

        // Priority and Deadline Grid
        gbc.gridy++;
        JPanel detailsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        detailsPanel.setOpaque(false);

        detailsPanel.add(new JLabel("Task Priority:"));
        detailsPanel.add(new JLabel("Deadline (YYYY-MM-DD):"));

        JComboBox<TaskPriority> priorityCombo = new JComboBox<>(TaskPriority.values());
        if (isEdit) {
            priorityCombo.setSelectedItem(task.getPriority());
        } else {
            priorityCombo.setSelectedItem(TaskPriority.MEDIUM);
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        JTextField deadlineField = new JTextField(isEdit && task.getDeadline() != null ? task.getDeadline().format(dtf) : LocalDate.now().plusDays(7).format(dtf));
        deadlineField.putClientProperty("JTextField.placeholderText", "YYYY-MM-DD");

        detailsPanel.add(priorityCombo);
        detailsPanel.add(deadlineField);
        dialog.add(detailsPanel, gbc);

        // Status Combo Box (Only visible when editing)
        if (isEdit) {
            gbc.gridy++;
            dialog.add(new JLabel("Status:"), gbc);
            gbc.gridy++;
            JComboBox<TaskStatus> statusCombo = new JComboBox<>(TaskStatus.values());
            statusCombo.setSelectedItem(task.getStatus());
            dialog.add(statusCombo, gbc);
        }

        // Save & Cancel Buttons
        gbc.gridy++;
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonsPanel.setOpaque(false);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton saveBtn = new JButton(isEdit ? "Update" : "Create");
        saveBtn.setBackground(ThemeManager.COLOR_PRIMARY);
        saveBtn.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);

        saveBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String description = descArea.getText().trim();
            Project prj = (Project) projectFormCombo.getSelectedItem();
            User emp = (User) assigneeFormCombo.getSelectedItem();
            TaskPriority priority = (TaskPriority) priorityCombo.getSelectedItem();
            String deadlineStr = deadlineField.getText().trim();

            if (prj == null) {
                JOptionPane.showMessageDialog(dialog, "Each task must belong to a project.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                LocalDate deadline = null;
                if (!deadlineStr.isEmpty()) {
                    deadline = LocalDate.parse(deadlineStr, dtf);
                }

                Task t = isEdit ? task : new Task();
                t.setName(name);
                t.setDescription(description);
                t.setProjectId(prj.getId());
                t.setAssignedEmployeeId(emp != null && emp.getId() > 0 ? emp.getId() : null);
                t.setPriority(priority);
                t.setDeadline(deadline);

                if (isEdit) {
                    t.setStatus(task.getStatus());
                    mainFrame.getTaskService().updateTask(t);
                    JOptionPane.showMessageDialog(dialog, "Task updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    t.setStatus(TaskStatus.TO_DO);
                    mainFrame.getTaskService().createTask(t);
                    JOptionPane.showMessageDialog(dialog, "Task created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                }

                dialog.dispose();
                refresh();
                mainFrame.updateNotificationCount();
            } catch (java.time.format.DateTimeParseException ex) {
                JOptionPane.showMessageDialog(dialog, "Deadline date format must be YYYY-MM-DD.", "Date Parse Error", JOptionPane.ERROR_MESSAGE);
            } catch (ValidationException ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Validation Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "A database connection error occurred.", "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        buttonsPanel.add(cancelBtn);
        buttonsPanel.add(saveBtn);
        dialog.add(buttonsPanel, gbc);

        dialog.setVisible(true);
    }

    private void showTaskDetails(Task task) {
        JDialog dialog = new JDialog(mainFrame, "Task Details", true);
        dialog.setSize(460, 480);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(ThemeManager.COLOR_BACKGROUND);

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(25, 25, 25, 25));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // Title
        JLabel titleLabel = new JLabel(task.getName());
        titleLabel.setFont(ThemeManager.FONT_TITLE);
        titleLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        contentPanel.add(titleLabel, gbc);

        // Project Info
        gbc.gridy++;
        String prjName = selectedProject != null ? selectedProject.getName() : "Unassigned";
        JLabel prjLabel = new JLabel("📁 Project:  " + prjName);
        prjLabel.setFont(ThemeManager.FONT_BODY);
        prjLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        contentPanel.add(prjLabel, gbc);

        // Assignee Info
        gbc.gridy++;
        String empName = "Unassigned";
        if (task.getAssignedEmployeeId() != null) {
            Optional<User> uOpt = allUsersList.stream().filter(u -> u.getId() == task.getAssignedEmployeeId().intValue()).findFirst();
            if (uOpt.isPresent()) {
                empName = uOpt.get().getFullName();
            }
        }
        JLabel empLabel = new JLabel("👤 Assignee:  " + empName);
        empLabel.setFont(ThemeManager.FONT_BODY);
        empLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        contentPanel.add(empLabel, gbc);

        // Priority & Status Badge Row
        gbc.gridy++;
        JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        badgeRow.setOpaque(false);

        badgeRow.add(new JLabel("Priority:"));
        badgeRow.add(new PillBadge(task.getPriority().toString(), getPriorityColor(task.getPriority()), Color.WHITE, 8));

        badgeRow.add(new JLabel("Status:"));
        Color statusBg = getTaskStatusColor(task.getStatus());
        badgeRow.add(new PillBadge(task.getStatus().toString(), statusBg, Color.WHITE, 8));
        contentPanel.add(badgeRow, gbc);

        // Due Date
        gbc.gridy++;
        boolean isOverdue = task.getStatus() != TaskStatus.COMPLETED && task.getDeadline() != null && task.getDeadline().isBefore(LocalDate.now());
        JLabel dateLabel = new JLabel("📅 Due Date:  " + (task.getDeadline() != null ? task.getDeadline().toString() : "No Deadline"));
        dateLabel.setFont(ThemeManager.FONT_BODY);
        dateLabel.setForeground(isOverdue ? ThemeManager.COLOR_DANGER : ThemeManager.COLOR_TEXT_PRIMARY);
        contentPanel.add(dateLabel, gbc);

        // Description
        gbc.gridy++;
        JLabel descHeader = new JLabel("Description:");
        descHeader.setFont(ThemeManager.FONT_BOLD_SMALL);
        descHeader.setForeground(ThemeManager.COLOR_TEXT_MUTED);
        contentPanel.add(descHeader, gbc);

        gbc.gridy++;
        JTextArea descArea = new JTextArea(task.getDescription() != null ? task.getDescription() : "No description provided.");
        descArea.setFont(ThemeManager.FONT_BODY);
        descArea.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        descArea.setBackground(ThemeManager.COLOR_CARD);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setEditable(false);
        descArea.setFocusable(true);

        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setPreferredSize(new Dimension(380, 100));
        descScroll.setBorder(BorderFactory.createLineBorder(ThemeManager.COLOR_BORDER));
        contentPanel.add(descScroll, gbc);

        dialog.add(contentPanel, BorderLayout.CENTER);

        // Action Panel
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        actionPanel.setBackground(ThemeManager.COLOR_SIDEBAR);

        JButton closeBtn = new JButton("Close");
        closeBtn.setBackground(ThemeManager.COLOR_CARD);
        closeBtn.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        closeBtn.setFont(ThemeManager.FONT_BOLD_SMALL);
        closeBtn.addActionListener(e -> dialog.dispose());
        actionPanel.add(closeBtn);

        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null && (currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.MANAGER)) {
            JButton editBtn = new JButton("Edit Task");
            editBtn.setBackground(ThemeManager.COLOR_PRIMARY);
            editBtn.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
            editBtn.setFont(ThemeManager.FONT_BOLD_SMALL);
            editBtn.addActionListener(e -> {
                dialog.dispose();
                showTaskForm(task, selectedProject);
            });
            actionPanel.add(editBtn);
        }

        dialog.add(actionPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private Color getPriorityColor(TaskPriority priority) {
        return switch (priority) {
            case LOW -> new Color(71, 85, 105);
            case MEDIUM -> new Color(79, 70, 229);
            case HIGH -> new Color(249, 115, 22);
            case CRITICAL -> new Color(220, 38, 38);
        };
    }

    private void showStatusTransitionMenu(Task task, JComponent triggerComponent) {
        JPopupMenu menu = new JPopupMenu();
        for (TaskStatus status : TaskStatus.values()) {
            JMenuItem item = new JMenuItem(status.toString());
            item.addActionListener(e -> {
                SwingWorker<Void, Void> worker = new SwingWorker<>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        mainFrame.getTaskService().updateTaskStatus(task.getId(), status);
                        return null;
                    }

                    @Override
                    protected void done() {
                        try {
                            get();
                            refresh();
                            mainFrame.updateNotificationCount();
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(ProjectManagementView.this, "Invalid Status Transition: " + ex.getCause().getMessage(), "Action Blocked", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                };
                worker.execute();
            });
            menu.add(item);
        }
        menu.show(triggerComponent, 0, triggerComponent.getHeight());
    }

    // --- Styled Badge Subcomponents ---

    public static class PillBadge extends JPanel {
        private final JLabel label;
        private final int arc;

        public PillBadge(String text, Color bgColor, Color fgColor) {
            this(text, bgColor, fgColor, 12);
        }

        public PillBadge(String text, Color bgColor, Color fgColor, int arc) {
            this.arc = arc;
            setOpaque(false);
            setLayout(new FlowLayout(FlowLayout.CENTER, 8, 3));
            label = new JLabel(text);
            label.setFont(new Font("SansSerif", Font.BOLD, 10));
            label.setForeground(fgColor);
            add(label);
            setBackground(bgColor);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static class CountBadge extends JPanel {
        private final JLabel label;

        public CountBadge() {
            setOpaque(false);
            setLayout(new FlowLayout(FlowLayout.CENTER, 6, 2));
            label = new JLabel("0");
            label.setFont(new Font("SansSerif", Font.BOLD, 10));
            label.setForeground(ThemeManager.COLOR_TEXT_MUTED);
            add(label);
            setBackground(new Color(255, 255, 255, 15));
        }

        public void setCount(int count) {
            label.setText(String.valueOf(count));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}

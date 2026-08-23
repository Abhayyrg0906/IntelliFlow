package com.intelliflow.ui.views;

import com.intelliflow.context.UserSession;
import com.intelliflow.enums.Role;
import com.intelliflow.enums.TaskPriority;
import com.intelliflow.enums.TaskStatus;
import com.intelliflow.exception.ValidationException;
import com.intelliflow.model.Project;
import com.intelliflow.model.Task;
import com.intelliflow.model.User;
import com.intelliflow.ui.MainFrame;
import com.intelliflow.ui.ThemeManager;
import com.intelliflow.ui.components.EmptyStatePanel;
import com.intelliflow.ui.components.ModernTable;
import com.intelliflow.ui.components.RoundedPanel;

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
import java.util.stream.Collectors;

public class TaskManagementView extends BaseView {
    private final MainFrame mainFrame;

    // View State
    private boolean isKanbanMode = true;
    private int selectedTaskId = -1;
    private List<Task> displayedTasks = new ArrayList<>();
    private final List<RoundedPanel> cardPanels = new ArrayList<>();
    
    // Mapping caches
    private final Map<Integer, String> projectNamesMap = new HashMap<>();
    private final Map<Integer, String> employeeNamesMap = new HashMap<>();

    // Action Bar Widgets
    private JComboBox<Project> projectCombo;
    private JComboBox<User> assigneeCombo;
    private JButton viewToggleBtn;
    private JButton createButton;
    private JButton editButton;
    private JButton deleteButton;

    // Layout Containers
    private JPanel viewContainer;
    private CardLayout cardLayout;
    
    // Table View Components
    private ModernTable taskTable;
    private DefaultTableModel tableModel;
    private JScrollPane tableScrollPane;

    // Kanban View Components
    private JScrollPane kanbanScroll;
    private JPanel kanbanBoardPanel;
    
    // Kanban Columns & Headers
    private JPanel todoColPanel;
    private JPanel progressColPanel;
    private JPanel testingColPanel;
    private JPanel completedColPanel;
    private JPanel blockedColPanel;
    
    private CountBadge todoCountBadge;
    private CountBadge progressCountBadge;
    private CountBadge testingCountBadge;
    private CountBadge completedCountBadge;
    private CountBadge blockedCountBadge;

    // Filters and Sorting
    private JTextField searchTasksField;
    private JComboBox<Object> statusCombo;
    private JComboBox<Object> priorityCombo;
    private JComboBox<String> sortCombo;
    private EmptyStatePanel emptySearchPanel;
    private List<Task> allTasksList = new ArrayList<>();

    // Empty state Panel fallback
    private EmptyStatePanel emptyPanel;

    public TaskManagementView(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(15, 15));
        setBackground(ThemeManager.COLOR_BACKGROUND);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        initActionBar();
        initViewContainer();
        initEmptyPanel();
    }

    private void initActionBar() {
        JPanel actionBar = new JPanel(new BorderLayout(10, 0));
        actionBar.setOpaque(false);

        JLabel titleLabel = new JLabel("Task Workspace");
        titleLabel.setFont(ThemeManager.FONT_SUBTITLE);
        titleLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        actionBar.add(titleLabel, BorderLayout.WEST);

        // Buttons Panel (Right side)
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonsPanel.setOpaque(false);

        viewToggleBtn = new JButton("📋 Show Table View");
        viewToggleBtn.setBackground(ThemeManager.COLOR_CARD);
        viewToggleBtn.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        viewToggleBtn.setFont(ThemeManager.FONT_BOLD_SMALL);
        viewToggleBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        viewToggleBtn.addActionListener(e -> toggleViewMode());

        createButton = new JButton("➕ Create Task");
        createButton.setBackground(ThemeManager.COLOR_PRIMARY);
        createButton.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        createButton.setFont(ThemeManager.FONT_BOLD_SMALL);
        createButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        createButton.addActionListener(e -> showTaskForm(null));

        editButton = new JButton("📝 Edit Task");
        editButton.setBackground(ThemeManager.COLOR_CARD);
        editButton.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        editButton.setFont(ThemeManager.FONT_BOLD_SMALL);
        editButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        editButton.addActionListener(e -> handleEditAction());

        deleteButton = new JButton("🗑️ Delete Task");
        deleteButton.setBackground(ThemeManager.COLOR_DANGER);
        deleteButton.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        deleteButton.setFont(ThemeManager.FONT_BOLD_SMALL);
        deleteButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        deleteButton.addActionListener(e -> handleDeleteAction());

        buttonsPanel.add(viewToggleBtn);
        buttonsPanel.add(createButton);
        buttonsPanel.add(editButton);
        buttonsPanel.add(deleteButton);
        actionBar.add(buttonsPanel, BorderLayout.EAST);

        // --- Row 2: Filters Bar Panel ---
        JPanel filtersBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filtersBar.setOpaque(false);

        // Search Field
        searchTasksField = new JTextField();
        searchTasksField.setPreferredSize(new Dimension(140, 28));
        searchTasksField.putClientProperty("JTextField.placeholderText", "Search tasks...");
        searchTasksField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                applyFilters();
            }
        });
        filtersBar.add(searchTasksField);

        // Project Filter
        filtersBar.add(new JLabel("Project:"));
        projectCombo = new JComboBox<>();
        projectCombo.setPreferredSize(new Dimension(130, 28));
        projectCombo.addActionListener(e -> applyFilters());
        filtersBar.add(projectCombo);

        // Assignee Filter
        filtersBar.add(new JLabel("Assignee:"));
        assigneeCombo = new JComboBox<>();
        assigneeCombo.setPreferredSize(new Dimension(130, 28));
        assigneeCombo.addActionListener(e -> applyFilters());
        filtersBar.add(assigneeCombo);

        // Status Filter
        filtersBar.add(new JLabel("Status:"));
        statusCombo = new JComboBox<>();
        statusCombo.setPreferredSize(new Dimension(120, 28));
        statusCombo.addItem("All Statuses");
        for (TaskStatus s : TaskStatus.values()) {
            statusCombo.addItem(s);
        }
        statusCombo.addActionListener(e -> applyFilters());
        filtersBar.add(statusCombo);

        // Priority Filter
        filtersBar.add(new JLabel("Priority:"));
        priorityCombo = new JComboBox<>();
        priorityCombo.setPreferredSize(new Dimension(120, 28));
        priorityCombo.addItem("All Priorities");
        for (TaskPriority p : TaskPriority.values()) {
            priorityCombo.addItem(p);
        }
        priorityCombo.addActionListener(e -> applyFilters());
        filtersBar.add(priorityCombo);

        // Sort Combo
        filtersBar.add(new JLabel("Sort:"));
        sortCombo = new JComboBox<>(new String[]{
            "Sort by Name",
            "Sort by Date (Due)",
            "Sort by Priority",
            "Sort by Status"
        });
        sortCombo.setPreferredSize(new Dimension(140, 28));
        sortCombo.addActionListener(e -> applyFilters());
        filtersBar.add(sortCombo);

        // Clear Filters Button
        JButton clearFiltersBtn = new JButton("Clear Filters");
        clearFiltersBtn.setBackground(ThemeManager.COLOR_CARD);
        clearFiltersBtn.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        clearFiltersBtn.setFont(ThemeManager.FONT_BOLD_SMALL);
        clearFiltersBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clearFiltersBtn.addActionListener(e -> {
            searchTasksField.setText("");
            projectCombo.setSelectedIndex(0);
            assigneeCombo.setSelectedIndex(0);
            statusCombo.setSelectedIndex(0);
            priorityCombo.setSelectedIndex(0);
            sortCombo.setSelectedIndex(0);
            applyFilters();
        });
        filtersBar.add(clearFiltersBtn);

        // Combined top header panel
        JPanel northContainer = new JPanel();
        northContainer.setLayout(new BoxLayout(northContainer, BoxLayout.Y_AXIS));
        northContainer.setOpaque(false);
        northContainer.add(actionBar);
        northContainer.add(Box.createVerticalStrut(10));
        northContainer.add(filtersBar);
        northContainer.add(Box.createVerticalStrut(10));

        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        legendPanel.setOpaque(false);
        
        JLabel pipelineLabel = new JLabel("Workflow Pipeline: ");
        pipelineLabel.setFont(ThemeManager.FONT_BOLD_SMALL);
        pipelineLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);
        legendPanel.add(pipelineLabel);

        legendPanel.add(new PillBadge("TO DO", new Color(148, 163, 184), Color.WHITE, 8));
        
        JLabel arr1 = new JLabel(" ➔ ");
        arr1.setForeground(ThemeManager.COLOR_TEXT_MUTED);
        legendPanel.add(arr1);

        legendPanel.add(new PillBadge("IN PROGRESS", new Color(59, 130, 246), Color.WHITE, 8));
        
        JLabel arr2 = new JLabel(" ➔ ");
        arr2.setForeground(ThemeManager.COLOR_TEXT_MUTED);
        legendPanel.add(arr2);

        legendPanel.add(new PillBadge("TESTING", new Color(245, 158, 11), Color.WHITE, 8));
        
        JLabel arr3 = new JLabel(" ➔ ");
        arr3.setForeground(ThemeManager.COLOR_TEXT_MUTED);
        legendPanel.add(arr3);

        legendPanel.add(new PillBadge("COMPLETED", ThemeManager.COLOR_SUCCESS, Color.WHITE, 8));

        JLabel suffixLabel = new JLabel("   (Blocked tasks 🚫 can resolve to any status)");
        suffixLabel.setFont(ThemeManager.FONT_SMALL);
        suffixLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);
        legendPanel.add(suffixLabel);

        northContainer.add(legendPanel);

        add(northContainer, BorderLayout.NORTH);
    }

    private void initViewContainer() {
        cardLayout = new CardLayout();
        viewContainer = new JPanel(cardLayout);
        viewContainer.setOpaque(false);

        // 1. Table View Setup
        tableModel = new DefaultTableModel(
                new Object[]{"ID", "Task Name", "Project", "Assigned To", "Priority", "Deadline", "Status"}, 0
        );
        taskTable = new ModernTable();
        taskTable.setPlaceholderText("No tasks assigned.");
        taskTable.setModel(tableModel);

        tableScrollPane = new JScrollPane(taskTable);
        tableScrollPane.setBorder(BorderFactory.createEmptyBorder());
        tableScrollPane.setOpaque(false);
        tableScrollPane.getViewport().setOpaque(false);

        viewContainer.add(tableScrollPane, "table");

        // 2. Kanban Board View Setup
        kanbanBoardPanel = new JPanel(new GridLayout(1, 5, 12, 0));
        kanbanBoardPanel.setOpaque(false);

        // Initialize 5 Columns
        todoColPanel = createKanbanColContainer();
        progressColPanel = createKanbanColContainer();
        testingColPanel = createKanbanColContainer();
        completedColPanel = createKanbanColContainer();
        blockedColPanel = createKanbanColContainer();

        todoCountBadge = new CountBadge();
        progressCountBadge = new CountBadge();
        testingCountBadge = new CountBadge();
        completedCountBadge = new CountBadge();
        blockedCountBadge = new CountBadge();

        kanbanBoardPanel.add(createKanbanColumn("TO DO", new Color(148, 163, 184), todoCountBadge, todoColPanel));
        kanbanBoardPanel.add(createKanbanColumn("IN PROGRESS", new Color(59, 130, 246), progressCountBadge, progressColPanel));
        kanbanBoardPanel.add(createKanbanColumn("TESTING", new Color(245, 158, 11), testingCountBadge, testingColPanel));
        kanbanBoardPanel.add(createKanbanColumn("COMPLETED", ThemeManager.COLOR_SUCCESS, completedCountBadge, completedColPanel));
        kanbanBoardPanel.add(createKanbanColumn("BLOCKED", ThemeManager.COLOR_DANGER, blockedCountBadge, blockedColPanel));

        kanbanScroll = new JScrollPane(kanbanBoardPanel);
        kanbanScroll.setBorder(BorderFactory.createEmptyBorder());
        kanbanScroll.setOpaque(false);
        kanbanScroll.getViewport().setOpaque(false);

        viewContainer.add(kanbanScroll, "kanban");

        add(viewContainer, BorderLayout.CENTER);
        cardLayout.show(viewContainer, "kanban"); // default view is Kanban Board
    }

    private void initEmptyPanel() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        boolean isWriteable = currentUser != null && (currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.MANAGER);
        emptyPanel = new EmptyStatePanel(
                "📝",
                "No tasks assigned.",
                "Set goals by assigning tasks to your team members.",
                isWriteable ? "➕ Create Task" : null,
                isWriteable ? e -> showTaskForm(null) : null
        );
        viewContainer.add(emptyPanel, "empty");

        emptySearchPanel = new EmptyStatePanel(
                "🔍",
                "No matching tasks found.",
                "Try changing your search or filters.",
                "Clear Filters",
                e -> {
                    searchTasksField.setText("");
                    projectCombo.setSelectedIndex(0);
                    assigneeCombo.setSelectedIndex(0);
                    statusCombo.setSelectedIndex(0);
                    priorityCombo.setSelectedIndex(0);
                    sortCombo.setSelectedIndex(0);
                    applyFilters();
                }
        );
        viewContainer.add(emptySearchPanel, "emptySearch");
    }

    private JPanel createKanbanColContainer() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    private JPanel createColumnHeaderPanel(String title, Color accentColor, CountBadge countBadge) {
        JPanel headerPanel = new JPanel(new BorderLayout(8, 0));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(5, 5, 12, 5));

        JPanel accentBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accentColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                g2.dispose();
            }
        };
        accentBar.setPreferredSize(new Dimension(5, 16));
        
        JPanel titleGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleGroup.setOpaque(false);
        titleGroup.add(accentBar);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(ThemeManager.FONT_BOLD_SMALL);
        titleLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        titleGroup.add(titleLabel);
        
        headerPanel.add(titleGroup, BorderLayout.WEST);
        headerPanel.add(countBadge, BorderLayout.EAST);
        
        return headerPanel;
    }

    private JPanel createKanbanColumn(String title, Color accentColor, CountBadge countBadge, JPanel cardsPanel) {
        RoundedPanel wrapper = new RoundedPanel(12, ThemeManager.COLOR_CARD);
        wrapper.setDrawBorder(true);
        wrapper.setBorderColor(ThemeManager.COLOR_BORDER);
        wrapper.setLayout(new BorderLayout(5, 5));
        wrapper.setBorder(new EmptyBorder(10, 10, 10, 10));

        wrapper.add(createColumnHeaderPanel(title, accentColor, countBadge), BorderLayout.NORTH);

        JPanel contentScrollWrapper = new JPanel(new BorderLayout());
        contentScrollWrapper.setOpaque(false);
        contentScrollWrapper.add(cardsPanel, BorderLayout.NORTH);

        JScrollPane colScroll = new JScrollPane(contentScrollWrapper);
        colScroll.setBorder(BorderFactory.createEmptyBorder());
        colScroll.setOpaque(false);
        colScroll.getViewport().setOpaque(false);
        colScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        wrapper.add(colScroll, BorderLayout.CENTER);
        return wrapper;
    }

    private void toggleViewMode() {
        isKanbanMode = !isKanbanMode;
        if (isKanbanMode) {
            viewToggleBtn.setText("📋 Show Table View");
        } else {
            viewToggleBtn.setText("🗂️ Show Kanban Board");
        }
        applyFilters();
    }

    @Override
    public void refresh() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // Toggle creation permissions
        boolean isWriteable = currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.MANAGER;
        createButton.setVisible(isWriteable);
        editButton.setVisible(isWriteable);
        deleteButton.setVisible(isWriteable);

        // Load project and employee options
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private List<Project> projects = new ArrayList<>();
            private List<User> employees = new ArrayList<>();
            private List<Task> tasksList = new ArrayList<>();

            @Override
            protected Void doInBackground() throws Exception {
                projects = mainFrame.getProjectService().getAllProjects();
                employees = mainFrame.getUserService().getAllUsers();

                // Rebuild caches
                projectNamesMap.clear();
                for (Project p : projects) {
                    projectNamesMap.put(p.getId(), p.getName());
                }
                employeeNamesMap.clear();
                for (User u : employees) {
                    employeeNamesMap.put(u.getId(), u.getFullName());
                }

                if (currentUser.getRole() == Role.EMPLOYEE) {
                    tasksList = mainFrame.getTaskService().getTasksByEmployee(currentUser.getId());
                } else {
                    tasksList = mainFrame.getTaskService().getAllTasks();
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();

                    // Rebuild combo options safely
                    Project lastPrj = (Project) projectCombo.getSelectedItem();
                    User lastEmp = (User) assigneeCombo.getSelectedItem();

                    // Temporarily remove action listeners to prevent trigger loops
                    ActionListener[] prjListeners = projectCombo.getActionListeners();
                    ActionListener[] empListeners = assigneeCombo.getActionListeners();
                    for (ActionListener al : prjListeners) projectCombo.removeActionListener(al);
                    for (ActionListener al : empListeners) assigneeCombo.removeActionListener(al);

                    projectCombo.removeAllItems();
                    assigneeCombo.removeAllItems();

                    // Seed default "All" option
                    Project dummyPrj = new Project();
                    dummyPrj.setId(-1);
                    dummyPrj.setName("All Projects");
                    projectCombo.addItem(dummyPrj);

                    User dummyEmp = new User();
                    dummyEmp.setId(-1);
                    dummyEmp.setFullName("All Assigned Employees");
                    assigneeCombo.addItem(dummyEmp);

                    for (Project p : projects) projectCombo.addItem(p);
                    for (User u : employees) {
                        if (u.getRole() == Role.EMPLOYEE) {
                            assigneeCombo.addItem(u);
                        }
                    }

                    // Restore selection
                    if (lastPrj != null) {
                        for (int i = 0; i < projectCombo.getItemCount(); i++) {
                            if (projectCombo.getItemAt(i).getId() == lastPrj.getId()) {
                                projectCombo.setSelectedIndex(i);
                                break;
                            }
                        }
                    }
                    if (lastEmp != null) {
                        for (int i = 0; i < assigneeCombo.getItemCount(); i++) {
                            if (assigneeCombo.getItemAt(i).getId() == lastEmp.getId()) {
                                assigneeCombo.setSelectedIndex(i);
                                break;
                            }
                        }
                    }

                    // Re-register action listeners
                    for (ActionListener al : prjListeners) projectCombo.addActionListener(al);
                    for (ActionListener al : empListeners) assigneeCombo.addActionListener(al);

                    // Hide combo filters if Employee
                    boolean showFilters = currentUser.getRole() != Role.EMPLOYEE;
                    projectCombo.setVisible(showFilters);
                    assigneeCombo.setVisible(showFilters);

                    allTasksList = tasksList;
                    applyFilters();

                } catch (Exception e) {
                    System.err.println("Failed to reload tasks lists: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void applyFilters() {
        if (allTasksList == null) return;
        
        String query = searchTasksField.getText().trim().toLowerCase();
        
        Project selectedPrj = (Project) projectCombo.getSelectedItem();
        User selectedEmp = (User) assigneeCombo.getSelectedItem();
        Object selectedStatus = statusCombo.getSelectedItem();
        Object selectedPriority = priorityCombo.getSelectedItem();
        
        List<Task> filtered = allTasksList.stream()
                .filter(t -> {
                    if (query.isEmpty()) return true;
                    String taskName = t.getName().toLowerCase();
                    String taskDesc = t.getDescription() != null ? t.getDescription().toLowerCase() : "";
                    String prjName = projectNamesMap.getOrDefault(t.getProjectId(), "").toLowerCase();
                    String empName = employeeNamesMap.getOrDefault(t.getAssignedEmployeeId(), "").toLowerCase();
                    
                    return taskName.contains(query) || taskDesc.contains(query) || prjName.contains(query) || empName.contains(query);
                })
                .filter(t -> {
                    if (selectedPrj == null || selectedPrj.getId() == -1) return true;
                    return t.getProjectId() == selectedPrj.getId();
                })
                .filter(t -> {
                    if (selectedEmp == null || selectedEmp.getId() == -1) return true;
                    return t.getAssignedEmployeeId() != null && t.getAssignedEmployeeId().intValue() == selectedEmp.getId();
                })
                .filter(t -> {
                    if (selectedStatus == null || selectedStatus.equals("All Statuses")) return true;
                    return t.getStatus() == selectedStatus;
                })
                .filter(t -> {
                    if (selectedPriority == null || selectedPriority.equals("All Priorities")) return true;
                    return t.getPriority() == selectedPriority;
                })
                .collect(Collectors.toList());
        
        // Sorting
        int sortIndex = sortCombo.getSelectedIndex();
        if (sortIndex == 0) {
            filtered.sort((t1, t2) -> t1.getName().compareToIgnoreCase(t2.getName()));
        } else if (sortIndex == 1) {
            filtered.sort((t1, t2) -> t1.getDeadline().compareTo(t2.getDeadline()));
        } else if (sortIndex == 2) {
            filtered.sort((t1, t2) -> Integer.compare(getPriorityWeight(t2.getPriority()), getPriorityWeight(t1.getPriority())));
        } else if (sortIndex == 3) {
            filtered.sort((t1, t2) -> Integer.compare(getStatusWeight(t1.getStatus()), getStatusWeight(t2.getStatus())));
        }
        
        displayedTasks = filtered;
        
        // Handle UI switching based on list content
        if (allTasksList.isEmpty()) {
            cardLayout.show(viewContainer, "empty");
        } else if (displayedTasks.isEmpty()) {
            if (isKanbanMode) {
                cardLayout.show(viewContainer, "emptySearch");
            } else {
                cardLayout.show(viewContainer, "table");
                taskTable.setPlaceholderText("No matching tasks found. Try changing your search or filters.");
            }
        } else {
            cardLayout.show(viewContainer, isKanbanMode ? "kanban" : "table");
        }
        
        populateTableView();
        populateKanbanBoard();
    }

    private int getPriorityWeight(TaskPriority p) {
        if (p == null) return 0;
        return switch (p) {
            case CRITICAL -> 4;
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }

    private int getStatusWeight(TaskStatus s) {
        if (s == null) return 0;
        return switch (s) {
            case TO_DO -> 1;
            case IN_PROGRESS -> 2;
            case TESTING -> 3;
            case COMPLETED -> 4;
            case BLOCKED -> 5;
        };
    }

    private void populateTableView() {
        tableModel.setRowCount(0);
        for (Task t : displayedTasks) {
            String prjName = projectNamesMap.getOrDefault(t.getProjectId(), "Unassigned");
            String empName = employeeNamesMap.getOrDefault(t.getAssignedEmployeeId(), "Unassigned");
            tableModel.addRow(new Object[]{
                    t.getId(),
                    t.getName(),
                    prjName,
                    empName,
                    t.getPriority().toString(),
                    t.getDeadline().toString(),
                    t.getStatus().toString()
            });
        }
    }

    private void populateKanbanBoard() {
        todoColPanel.removeAll();
        progressColPanel.removeAll();
        testingColPanel.removeAll();
        completedColPanel.removeAll();
        blockedColPanel.removeAll();
        
        cardPanels.clear();

        cardLayout.show(viewContainer, isKanbanMode ? "kanban" : "table");

        int todoCount = 0, progressCount = 0, testingCount = 0, completedCount = 0, blockedCount = 0;

        for (Task t : displayedTasks) {
            JPanel card = createTaskCard(t);
            cardPanels.add((RoundedPanel) card);

            switch (t.getStatus()) {
                case TO_DO -> { todoColPanel.add(card); todoColPanel.add(Box.createVerticalStrut(10)); todoCount++; }
                case IN_PROGRESS -> { progressColPanel.add(card); progressColPanel.add(Box.createVerticalStrut(10)); progressCount++; }
                case TESTING -> { testingColPanel.add(card); testingColPanel.add(Box.createVerticalStrut(10)); testingCount++; }
                case COMPLETED -> { completedColPanel.add(card); completedColPanel.add(Box.createVerticalStrut(10)); completedCount++; }
                case BLOCKED -> { blockedColPanel.add(card); blockedColPanel.add(Box.createVerticalStrut(10)); blockedCount++; }
            }
        }

        // Add empty states for empty columns
        if (todoCount == 0) {
            todoColPanel.add(createColumnEmptyState());
        }
        if (progressCount == 0) {
            progressColPanel.add(createColumnEmptyState());
        }
        if (testingCount == 0) {
            testingColPanel.add(createColumnEmptyState());
        }
        if (completedCount == 0) {
            completedColPanel.add(createColumnEmptyState());
        }
        if (blockedCount == 0) {
            blockedColPanel.add(createColumnEmptyState());
        }

        // Update column header badges
        todoCountBadge.setCount(todoCount);
        progressCountBadge.setCount(progressCount);
        testingCountBadge.setCount(testingCount);
        completedCountBadge.setCount(completedCount);
        blockedCountBadge.setCount(blockedCount);

        // Revalidate layout
        todoColPanel.revalidate(); todoColPanel.repaint();
        progressColPanel.revalidate(); progressColPanel.repaint();
        testingColPanel.revalidate(); testingColPanel.repaint();
        completedColPanel.revalidate(); completedColPanel.repaint();
        blockedColPanel.revalidate(); blockedColPanel.repaint();
    }

    private JPanel createColumnEmptyState() {
        JPanel emptyCard = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ThemeManager.COLOR_BORDER);
                float[] dash = {4f, 4f};
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, dash, 0.0f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        emptyCard.setOpaque(false);
        emptyCard.setPreferredSize(new Dimension(150, 70));
        
        JLabel msgLabel = new JLabel("No tasks in this stage", SwingConstants.CENTER);
        msgLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        msgLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);
        emptyCard.add(msgLabel);
        
        return emptyCard;
    }

    private JPanel createTaskCard(Task t) {
        RoundedPanel card = new RoundedPanel(10, ThemeManager.COLOR_CARD);
        card.setDrawBorder(true);
        card.setBorderColor(t.getId() == selectedTaskId ? ThemeManager.COLOR_PRIMARY : ThemeManager.COLOR_BORDER);
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(12, 14, 12, 14));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        User currentUser = UserSession.getInstance().getCurrentUser();

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (t.getId() != selectedTaskId) {
                    card.setBorderColor(ThemeManager.COLOR_PRIMARY_HOVER);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (t.getId() != selectedTaskId) {
                    card.setBorderColor(ThemeManager.COLOR_BORDER);
                } else {
                    card.setBorderColor(ThemeManager.COLOR_PRIMARY);
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                selectedTaskId = t.getId();
                updateTaskCardSelectionVisuals();
                
                if (e.getClickCount() == 2) {
                    if (currentUser != null && (currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.MANAGER)) {
                        showTaskForm(t);
                    } else {
                        showTaskDetails(t);
                    }
                }
            }
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 8, 0);

        // Header Panel: Project Name & Priority badge
        JPanel headerPanel = new JPanel(new BorderLayout(5, 0));
        headerPanel.setOpaque(false);

        String prjName = projectNamesMap.getOrDefault(t.getProjectId(), "Unassigned");
        JLabel prjLabel = new JLabel(prjName);
        prjLabel.setFont(ThemeManager.FONT_SMALL);
        prjLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);
        headerPanel.add(prjLabel, BorderLayout.CENTER);

        PillBadge priorityPill = new PillBadge(t.getPriority().toString(), getPriorityColor(t.getPriority()), Color.WHITE, 6);
        headerPanel.add(priorityPill, BorderLayout.EAST);
        
        card.add(headerPanel, gbc);

        // Task Name
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 4, 0);
        JLabel titleLabel = new JLabel(t.getName());
        titleLabel.setFont(ThemeManager.FONT_BOLD_SMALL);
        titleLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        card.add(titleLabel, gbc);

        // Description (truncated)
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 8, 0);
        String descText = t.getDescription() != null ? t.getDescription() : "";
        if (descText.length() > 60) {
            descText = descText.substring(0, 57) + "...";
        }
        JLabel descLabel = new JLabel(descText.isEmpty() ? "No description." : descText);
        descLabel.setFont(ThemeManager.FONT_SMALL);
        descLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);
        card.add(descLabel, gbc);

        // Assignee
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 4, 0);
        String empName = employeeNamesMap.getOrDefault(t.getAssignedEmployeeId(), "Unassigned");
        JLabel empLabel = new JLabel("👤 " + empName);
        empLabel.setFont(ThemeManager.FONT_SMALL);
        empLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        card.add(empLabel, gbc);

        // Due date
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 10, 0);
        boolean isOverdue = t.getStatus() != TaskStatus.COMPLETED && t.getDeadline().isBefore(LocalDate.now());
        JLabel dateLabel = new JLabel("📅 " + t.getDeadline().toString());
        dateLabel.setFont(ThemeManager.FONT_SMALL);
        dateLabel.setForeground(isOverdue ? ThemeManager.COLOR_DANGER : ThemeManager.COLOR_TEXT_MUTED);
        card.add(dateLabel, gbc);

        // Separator divider
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 8, 0);
        JSeparator sep = new JSeparator();
        sep.setForeground(ThemeManager.COLOR_BORDER);
        card.add(sep, gbc);

        // Footer Actions panel
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 0, 0);
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setOpaque(false);

        PillBadge statusBadge = new PillBadge(t.getStatus().toString(), getStatusColor(t.getStatus()), Color.WHITE, 6);
        footerPanel.add(statusBadge, BorderLayout.WEST);

        JPanel actionsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        actionsRow.setOpaque(false);

        // 👁️ View details button
        JButton viewBtn = createCardActionButton("👁️", "View details", e -> showTaskDetails(t));
        actionsRow.add(viewBtn);

        boolean isManagerOrAdmin = currentUser != null && (currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.MANAGER);
        boolean isAssignedEmployee = currentUser != null && currentUser.getRole() == Role.EMPLOYEE && t.getAssignedEmployeeId() != null && t.getAssignedEmployeeId() == currentUser.getId();

        if (isManagerOrAdmin) {
            // ✏️ Edit button
            JButton editBtn = createCardActionButton("✏️", "Edit task", e -> showTaskForm(t));
            actionsRow.add(editBtn);

            // 🗑️ Delete button
            JButton deleteBtn = createCardActionButton("🗑️", "Delete task", e -> {
                selectedTaskId = t.getId();
                handleDeleteAction();
            });
            actionsRow.add(deleteBtn);
        }

        // 🔄 Status transition (Manager/Admin OR assigned Employee)
        if (isManagerOrAdmin || isAssignedEmployee) {
            JButton statusBtn = createCardActionButton("🔄", "Update status", e -> showStatusTransitionMenu(t, (JButton) e.getSource()));
            actionsRow.add(statusBtn);
        }

        footerPanel.add(actionsRow, BorderLayout.EAST);
        card.add(footerPanel, gbc);

        return card;
    }

    private JButton createCardActionButton(String icon, String tooltip, ActionListener listener) {
        JButton btn = new JButton(icon);
        btn.setToolTipText(tooltip);
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(28, 28));
        btn.addActionListener(listener);
        
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setContentAreaFilled(true);
                btn.setBackground(new Color(255, 255, 255, 25));
                btn.repaint();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setContentAreaFilled(false);
                btn.repaint();
            }
        });
        return btn;
    }

    private void updateTaskCardSelectionVisuals() {
        for (int i = 0; i < displayedTasks.size(); i++) {
            Task t = displayedTasks.get(i);
            if (i < cardPanels.size()) {
                cardPanels.get(i).setBorderColor(t.getId() == selectedTaskId ? ThemeManager.COLOR_PRIMARY : ThemeManager.COLOR_BORDER);
            }
        }
    }

    private Color getPriorityColor(TaskPriority priority) {
        return switch (priority) {
            case LOW -> new Color(71, 85, 105);
            case MEDIUM -> new Color(79, 70, 229);
            case HIGH -> new Color(249, 115, 22);
            case CRITICAL -> new Color(220, 38, 38);
        };
    }

    private int getSelectedTaskId() {
        if (isKanbanMode) {
            return selectedTaskId;
        } else {
            int selectedRow = taskTable.getSelectedRow();
            if (selectedRow >= 0) {
                return (int) taskTable.getValueAt(selectedRow, 0);
            }
            return -1;
        }
    }

    private void handleEditAction() {
        int taskId = getSelectedTaskId();
        if (taskId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a task card to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Task taskToEdit = displayedTasks.stream().filter(t -> t.getId() == taskId).findFirst().orElse(null);
        if (taskToEdit != null) {
            showTaskForm(taskToEdit);
        }
    }

    private void handleDeleteAction() {
        int taskId = getSelectedTaskId();
        if (taskId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a task card to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Task taskToDelete = displayedTasks.stream().filter(t -> t.getId() == taskId).findFirst().orElse(null);
        if (taskToDelete == null) return;

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete task: '" + taskToDelete.getName() + "'?\nThis will remove the assignment data.",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    mainFrame.getTaskService().deleteTask(taskId);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        selectedTaskId = -1; // clear selection
                        refresh();
                        mainFrame.updateNotificationCount();
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(TaskManagementView.this, "Failed to delete task.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
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
                            JOptionPane.showMessageDialog(TaskManagementView.this, "Invalid Status Transition: " + ex.getCause().getMessage(), "Action Blocked", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                };
                worker.execute();
            });
            menu.add(item);
        }
        menu.show(triggerComponent, 0, triggerComponent.getHeight());
    }

    public void showTaskForm(Task task) {
        boolean isEdit = (task != null);
        JDialog dialog = new JDialog(mainFrame, isEdit ? "Modify Task Details" : "Create New Task", true);
        dialog.setSize(480, 560);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
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

        // Project Combo Box
        gbc.gridy++;
        dialog.add(new JLabel("Associated Project:"), gbc);
        gbc.gridy++;

        List<Project> projects = new ArrayList<>();
        try {
            projects = mainFrame.getProjectService().getAllProjects();
        } catch (Exception ignored) {}

        JComboBox<Project> projectFormCombo = new JComboBox<>(projects.toArray(new Project[0]));
        if (isEdit) {
            for (int i = 0; i < projectFormCombo.getItemCount(); i++) {
                if (projectFormCombo.getItemAt(i).getId() == task.getProjectId()) {
                    projectFormCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
        dialog.add(projectFormCombo, gbc);

        // Assignee Combo Box
        gbc.gridy++;
        dialog.add(new JLabel("Assign Employee:"), gbc);
        gbc.gridy++;

        List<User> employees = new ArrayList<>();
        try {
            employees = mainFrame.getUserService().getAllUsers().stream()
                    .filter(u -> u.getRole() == Role.EMPLOYEE)
                    .collect(Collectors.toList());
        } catch (Exception ignored) {}

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
                LocalDate deadline = LocalDate.parse(deadlineStr, dtf);

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

    private Color getStatusColor(TaskStatus status) {
        return switch (status) {
            case TO_DO -> new Color(148, 163, 184);
            case IN_PROGRESS -> new Color(59, 130, 246);
            case TESTING -> new Color(245, 158, 11);
            case COMPLETED -> ThemeManager.COLOR_SUCCESS;
            case BLOCKED -> ThemeManager.COLOR_DANGER;
        };
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
        String prjName = projectNamesMap.getOrDefault(task.getProjectId(), "Unassigned");
        JLabel prjLabel = new JLabel("📁 Project:  " + prjName);
        prjLabel.setFont(ThemeManager.FONT_BODY);
        prjLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        contentPanel.add(prjLabel, gbc);

        // Assignee Info
        gbc.gridy++;
        String empName = employeeNamesMap.getOrDefault(task.getAssignedEmployeeId(), "Unassigned");
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
        Color statusBg = getStatusColor(task.getStatus());
        badgeRow.add(new PillBadge(task.getStatus().toString(), statusBg, Color.WHITE, 8));
        contentPanel.add(badgeRow, gbc);

        // Due Date
        gbc.gridy++;
        boolean isOverdue = task.getStatus() != TaskStatus.COMPLETED && task.getDeadline().isBefore(LocalDate.now());
        JLabel dateLabel = new JLabel("📅 Due Date:  " + task.getDeadline().toString());
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
                showTaskForm(task);
            });
            actionPanel.add(editBtn);
        }

        dialog.add(actionPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

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


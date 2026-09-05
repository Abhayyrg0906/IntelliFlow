package com.intelliflow.ui.views;

import com.intelliflow.context.UserSession;
import com.intelliflow.enums.Role;
import com.intelliflow.enums.TaskPriority;
import com.intelliflow.enums.TaskStatus;
import com.intelliflow.model.Project;
import com.intelliflow.model.Task;
import com.intelliflow.model.User;
import com.intelliflow.ui.MainFrame;
import com.intelliflow.ui.ThemeManager;
import com.intelliflow.ui.components.EmptyStatePanel;
import com.intelliflow.ui.components.RoundedPanel;
import com.intelliflow.util.DeadlineTimelineUtil;
import com.intelliflow.util.DeadlineTimelineUtil.DeadlineSection;
import com.intelliflow.util.DeadlineUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class DeadlinesView extends BaseView {
    private final MainFrame mainFrame;

    // Data Caches
    private List<Task> allTasks = new ArrayList<>();
    private final Map<Integer, String> projectNamesMap = new HashMap<>();
    private final Map<Integer, String> employeeNamesMap = new HashMap<>();

    // UI Components
    private JTextField searchField;
    private JComboBox<Project> projectCombo;
    private JComboBox<String> statusFilterCombo;
    private JComboBox<Object> priorityFilterCombo;

    private JLabel overdueCountLabel;
    private JLabel todayCountLabel;
    private JLabel tomorrowCountLabel;
    private JLabel upcomingCountLabel;

    private JPanel timelineContainer;
    private JScrollPane timelineScrollPane;
    private EmptyStatePanel emptyPanel;
    private EmptyStatePanel emptySearchPanel;
    private CardLayout cardLayout;
    private JPanel centerWrapper;

    public DeadlinesView(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(15, 15));
        setBackground(ThemeManager.COLOR_BACKGROUND);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        initHeader();
        initCenter();
    }

    private void initHeader() {
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.setOpaque(false);

        // Title row
        JPanel titleRow = new JPanel(new BorderLayout(10, 0));
        titleRow.setOpaque(false);

        JLabel titleLabel = new JLabel("📅 Upcoming Deadlines & Schedule");
        titleLabel.setFont(ThemeManager.FONT_SUBTITLE);
        titleLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        titleRow.add(titleLabel, BorderLayout.WEST);

        JButton refreshBtn = new JButton("🔄 Refresh");
        refreshBtn.setBackground(ThemeManager.COLOR_CARD);
        refreshBtn.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        refreshBtn.setFont(ThemeManager.FONT_BOLD_SMALL);
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> refresh());
        titleRow.add(refreshBtn, BorderLayout.EAST);

        northPanel.add(titleRow);
        northPanel.add(Box.createVerticalStrut(10));

        // Filters row
        JPanel filtersBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filtersBar.setOpaque(false);

        searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(160, 28));
        searchField.putClientProperty("JTextField.placeholderText", "Search tasks or dates...");
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                applyFilters();
            }
        });
        filtersBar.add(searchField);

        filtersBar.add(new JLabel("Project:"));
        projectCombo = new JComboBox<>();
        projectCombo.setPreferredSize(new Dimension(140, 28));
        projectCombo.addActionListener(e -> applyFilters());
        filtersBar.add(projectCombo);

        filtersBar.add(new JLabel("Status:"));
        statusFilterCombo = new JComboBox<>(new String[]{"Incomplete Only", "All Tasks", "Completed Only"});
        statusFilterCombo.setPreferredSize(new Dimension(130, 28));
        statusFilterCombo.addActionListener(e -> applyFilters());
        filtersBar.add(statusFilterCombo);

        filtersBar.add(new JLabel("Priority:"));
        priorityFilterCombo = new JComboBox<>();
        priorityFilterCombo.setPreferredSize(new Dimension(120, 28));
        priorityFilterCombo.addItem("All Priorities");
        for (TaskPriority p : TaskPriority.values()) {
            priorityFilterCombo.addItem(p);
        }
        priorityFilterCombo.addActionListener(e -> applyFilters());
        filtersBar.add(priorityFilterCombo);

        JButton clearBtn = new JButton("Clear Filters");
        clearBtn.setBackground(ThemeManager.COLOR_CARD);
        clearBtn.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        clearBtn.setFont(ThemeManager.FONT_BOLD_SMALL);
        clearBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            if (projectCombo.getItemCount() > 0) projectCombo.setSelectedIndex(0);
            statusFilterCombo.setSelectedIndex(0);
            priorityFilterCombo.setSelectedIndex(0);
            applyFilters();
        });
        filtersBar.add(clearBtn);

        northPanel.add(filtersBar);
        northPanel.add(Box.createVerticalStrut(10));

        // Summary Statistics Ribbon
        RoundedPanel statsRibbon = new RoundedPanel(10, ThemeManager.COLOR_CARD);
        statsRibbon.setDrawBorder(true);
        statsRibbon.setBorderColor(ThemeManager.COLOR_BORDER);
        statsRibbon.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 8));
        statsRibbon.setBorder(new EmptyBorder(6, 16, 6, 16));

        JLabel ribbonTitle = new JLabel("Schedule Breakdown:");
        ribbonTitle.setFont(ThemeManager.FONT_BOLD_SMALL);
        ribbonTitle.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        statsRibbon.add(ribbonTitle);

        overdueCountLabel = new JLabel("⛔ Overdue: 0");
        overdueCountLabel.setFont(ThemeManager.FONT_BOLD_SMALL);
        overdueCountLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);
        statsRibbon.add(overdueCountLabel);

        todayCountLabel = new JLabel("🔥 Due Today: 0");
        todayCountLabel.setFont(ThemeManager.FONT_BOLD_SMALL);
        todayCountLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);
        statsRibbon.add(todayCountLabel);

        tomorrowCountLabel = new JLabel("⚠️ Due Tomorrow: 0");
        tomorrowCountLabel.setFont(ThemeManager.FONT_BOLD_SMALL);
        tomorrowCountLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);
        statsRibbon.add(tomorrowCountLabel);

        upcomingCountLabel = new JLabel("📅 Upcoming: 0");
        upcomingCountLabel.setFont(ThemeManager.FONT_BOLD_SMALL);
        upcomingCountLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);
        statsRibbon.add(upcomingCountLabel);

        northPanel.add(statsRibbon);

        add(northPanel, BorderLayout.NORTH);
    }

    private void initCenter() {
        cardLayout = new CardLayout();
        centerWrapper = new JPanel(cardLayout);
        centerWrapper.setOpaque(false);

        timelineContainer = new JPanel();
        timelineContainer.setLayout(new BoxLayout(timelineContainer, BoxLayout.Y_AXIS));
        timelineContainer.setOpaque(false);
        timelineContainer.setBorder(new EmptyBorder(10, 0, 10, 0));

        timelineScrollPane = new JScrollPane(timelineContainer);
        timelineScrollPane.setBorder(BorderFactory.createEmptyBorder());
        timelineScrollPane.setOpaque(false);
        timelineScrollPane.getViewport().setOpaque(false);
        timelineScrollPane.getVerticalScrollBar().setUnitIncrement(14);

        centerWrapper.add(timelineScrollPane, "timeline");

        emptyPanel = new EmptyStatePanel(
                "🎉",
                "No upcoming deadlines!",
                "All tasks are on schedule or completed.",
                null,
                null
        );
        centerWrapper.add(emptyPanel, "empty");

        emptySearchPanel = new EmptyStatePanel(
                "🔍",
                "No matching tasks found for schedule.",
                "Try clearing or adjusting your search filters.",
                "Clear Filters",
                e -> {
                    searchField.setText("");
                    if (projectCombo.getItemCount() > 0) projectCombo.setSelectedIndex(0);
                    statusFilterCombo.setSelectedIndex(0);
                    priorityFilterCombo.setSelectedIndex(0);
                    applyFilters();
                }
        );
        centerWrapper.add(emptySearchPanel, "emptySearch");

        add(centerWrapper, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) return;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private List<Project> projects = new ArrayList<>();
            private List<User> employees = new ArrayList<>();
            private List<Task> tasksList = new ArrayList<>();

            @Override
            protected Void doInBackground() throws Exception {
                projects = mainFrame.getProjectService().getAllProjects();
                employees = mainFrame.getUserService().getAllUsers();

                projectNamesMap.clear();
                for (Project p : projects) projectNamesMap.put(p.getId(), p.getName());

                employeeNamesMap.clear();
                for (User u : employees) employeeNamesMap.put(u.getId(), u.getFullName());

                if (currentUser.getRole() == Role.EMPLOYEE) {
                    tasksList = mainFrame.getTaskService().getTasksByEmployee(currentUser.getId());
                } else if (currentUser.getRole() == Role.MANAGER) {
                    // Fetch tasks for projects managed by this manager
                    tasksList = mainFrame.getTaskService().getAllTasks().stream()
                            .filter(t -> {
                                Project p = projects.stream().filter(prj -> prj.getId() == t.getProjectId()).findFirst().orElse(null);
                                return p != null && p.getManagerId() != null && p.getManagerId().equals(currentUser.getId());
                            })
                            .collect(Collectors.toList());
                } else {
                    tasksList = mainFrame.getTaskService().getAllTasks();
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();

                    Project lastPrj = (Project) projectCombo.getSelectedItem();
                    ActionListener[] listeners = projectCombo.getActionListeners();
                    for (ActionListener al : listeners) projectCombo.removeActionListener(al);

                    projectCombo.removeAllItems();
                    Project dummy = new Project();
                    dummy.setId(-1);
                    dummy.setName("All Projects");
                    projectCombo.addItem(dummy);

                    for (Project p : projects) {
                        if (currentUser.getRole() == Role.ADMIN || (currentUser.getRole() == Role.MANAGER && p.getManagerId() != null && p.getManagerId().equals(currentUser.getId()))) {
                            projectCombo.addItem(p);
                        }
                    }

                    if (lastPrj != null) {
                        for (int i = 0; i < projectCombo.getItemCount(); i++) {
                            if (projectCombo.getItemAt(i).getId() == lastPrj.getId()) {
                                projectCombo.setSelectedIndex(i);
                                break;
                            }
                        }
                    }

                    for (ActionListener al : listeners) projectCombo.addActionListener(al);

                    allTasks = tasksList;
                    applyFilters();

                } catch (Exception e) {
                    System.err.println("Failed to refresh DeadlinesView: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void applyFilters() {
        if (allTasks == null) return;

        String query = searchField.getText().trim().toLowerCase();
        Project selectedPrj = (Project) projectCombo.getSelectedItem();
        String statusMode = (String) statusFilterCombo.getSelectedItem();
        Object selectedPrio = priorityFilterCombo.getSelectedItem();

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        // Update counts on raw list
        int overdueCount = 0, todayCount = 0, tomorrowCount = 0, upcomingCount = 0;
        for (Task t : allTasks) {
            if (t.getStatus() != TaskStatus.COMPLETED && t.getDeadline() != null) {
                if (t.getDeadline().isBefore(today)) {
                    overdueCount++;
                } else if (t.getDeadline().isEqual(today)) {
                    todayCount++;
                } else if (t.getDeadline().isEqual(tomorrow)) {
                    tomorrowCount++;
                } else {
                    upcomingCount++;
                }
            }
        }

        overdueCountLabel.setText("⛔ Overdue: " + overdueCount);
        overdueCountLabel.setForeground(overdueCount > 0 ? ThemeManager.COLOR_DANGER : ThemeManager.COLOR_TEXT_MUTED);

        todayCountLabel.setText("🔥 Due Today: " + todayCount);
        todayCountLabel.setForeground(todayCount > 0 ? new Color(249, 115, 22) : ThemeManager.COLOR_TEXT_MUTED);

        tomorrowCountLabel.setText("⚠️ Due Tomorrow: " + tomorrowCount);
        tomorrowCountLabel.setForeground(tomorrowCount > 0 ? ThemeManager.COLOR_WARNING : ThemeManager.COLOR_TEXT_MUTED);

        upcomingCountLabel.setText("📅 Upcoming: " + upcomingCount);
        upcomingCountLabel.setForeground(upcomingCount > 0 ? new Color(59, 130, 246) : ThemeManager.COLOR_TEXT_MUTED);

        // Filter list
        List<Task> filtered = allTasks.stream()
                .filter(t -> {
                    if (query.isEmpty()) return true;
                    String name = t.getName().toLowerCase();
                    String desc = t.getDescription() != null ? t.getDescription().toLowerCase() : "";
                    String prj = projectNamesMap.getOrDefault(t.getProjectId(), "").toLowerCase();
                    String emp = employeeNamesMap.getOrDefault(t.getAssignedEmployeeId(), "").toLowerCase();
                    String dl = t.getDeadline() != null ? t.getDeadline().toString() : "";
                    return name.contains(query) || desc.contains(query) || prj.contains(query) || emp.contains(query) || dl.contains(query);
                })
                .filter(t -> {
                    if (selectedPrj == null || selectedPrj.getId() == -1) return true;
                    return t.getProjectId() == selectedPrj.getId();
                })
                .filter(t -> {
                    if ("Incomplete Only".equals(statusMode)) {
                        return t.getStatus() != TaskStatus.COMPLETED;
                    } else if ("Completed Only".equals(statusMode)) {
                        return t.getStatus() == TaskStatus.COMPLETED;
                    }
                    return true;
                })
                .filter(t -> {
                    if (selectedPrio == null || "All Priorities".equals(selectedPrio)) return true;
                    return t.getPriority() == selectedPrio;
                })
                .collect(Collectors.toList());

        if (allTasks.isEmpty()) {
            cardLayout.show(centerWrapper, "empty");
        } else if (filtered.isEmpty()) {
            cardLayout.show(centerWrapper, "emptySearch");
        } else {
            cardLayout.show(centerWrapper, "timeline");
            renderTimeline(filtered, today);
        }
    }

    private void renderTimeline(List<Task> tasks, LocalDate today) {
        timelineContainer.removeAll();

        List<DeadlineSection> sections = DeadlineTimelineUtil.groupTasksByDeadline(tasks, today);

        for (DeadlineSection section : sections) {
            JPanel sectionWrapper = new JPanel();
            sectionWrapper.setLayout(new BoxLayout(sectionWrapper, BoxLayout.Y_AXIS));
            sectionWrapper.setOpaque(false);
            sectionWrapper.setBorder(new EmptyBorder(0, 0, 15, 0));

            // Section Header Bar
            RoundedPanel headerCard = new RoundedPanel(10, ThemeManager.COLOR_CARD);
            headerCard.setDrawBorder(true);
            headerCard.setBorderColor(section.isOverdue() ? ThemeManager.COLOR_DANGER : (section.isToday() ? new Color(249, 115, 22) : ThemeManager.COLOR_BORDER));
            headerCard.setLayout(new BorderLayout(10, 0));
            headerCard.setBorder(new EmptyBorder(10, 14, 10, 14));

            JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            titleBox.setOpaque(false);

            JLabel iconLbl = new JLabel(section.getBadge());
            iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
            titleBox.add(iconLbl);

            JLabel titleLbl = new JLabel(section.getTitle());
            titleLbl.setFont(ThemeManager.FONT_SUBTITLE);
            titleLbl.setForeground(section.isOverdue() ? ThemeManager.COLOR_DANGER : ThemeManager.COLOR_TEXT_PRIMARY);
            titleBox.add(titleLbl);

            if (section.getSubtitle() != null && !section.getSubtitle().isEmpty()) {
                JLabel subLbl = new JLabel("—  " + section.getSubtitle());
                subLbl.setFont(ThemeManager.FONT_SMALL);
                subLbl.setForeground(ThemeManager.COLOR_TEXT_MUTED);
                titleBox.add(subLbl);
            }

            headerCard.add(titleBox, BorderLayout.WEST);

            TaskManagementView.CountBadge badge = new TaskManagementView.CountBadge();
            badge.setCount(section.getTasks().size());
            headerCard.add(badge, BorderLayout.EAST);

            sectionWrapper.add(headerCard);
            sectionWrapper.add(Box.createVerticalStrut(6));

            // Task Cards within section
            for (Task t : section.getTasks()) {
                JPanel taskCard = createTaskItemCard(t);
                sectionWrapper.add(taskCard);
                sectionWrapper.add(Box.createVerticalStrut(6));
            }

            timelineContainer.add(sectionWrapper);
        }

        timelineContainer.revalidate();
        timelineContainer.repaint();
    }

    private JPanel createTaskItemCard(Task t) {
        RoundedPanel card = new RoundedPanel(8, ThemeManager.COLOR_SIDEBAR);
        card.setDrawBorder(true);
        card.setBorderColor(ThemeManager.COLOR_BORDER);
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(new EmptyBorder(10, 14, 10, 14));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Hover Effect
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBorderColor(ThemeManager.COLOR_PRIMARY_HOVER);
                card.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBorderColor(ThemeManager.COLOR_BORDER);
                card.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 1) {
                    openTaskDetails(t);
                }
            }
        });

        // Left info
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        // Top line: Task Name + Priority Pill
        JPanel topLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        topLine.setOpaque(false);

        JLabel nameLabel = new JLabel(t.getName());
        nameLabel.setFont(ThemeManager.FONT_BOLD_SMALL);
        nameLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        topLine.add(nameLabel);

        TaskManagementView.PillBadge prioBadge = new TaskManagementView.PillBadge(
                t.getPriority().toString(),
                getPriorityColor(t.getPriority()),
                Color.WHITE,
                6
        );
        topLine.add(prioBadge);

        TaskManagementView.PillBadge statusBadge = new TaskManagementView.PillBadge(
                t.getStatus().toString(),
                getStatusColor(t.getStatus()),
                Color.WHITE,
                6
        );
        topLine.add(statusBadge);

        infoPanel.add(topLine);
        infoPanel.add(Box.createVerticalStrut(4));

        // Bottom line: Project + Assignee + Deadline text
        String prj = projectNamesMap.getOrDefault(t.getProjectId(), "Unassigned");
        String emp = employeeNamesMap.getOrDefault(t.getAssignedEmployeeId(), "Unassigned");
        String deadlineStr = DeadlineUtil.formatDeadlineDisplay(t);

        JLabel detailsLabel = new JLabel("📁 " + prj + "   •   👤 " + emp + "   •   " + deadlineStr);
        detailsLabel.setFont(ThemeManager.FONT_SMALL);
        detailsLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);
        infoPanel.add(detailsLabel);

        card.add(infoPanel, BorderLayout.CENTER);

        // Right actions
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actionsPanel.setOpaque(false);

        JButton viewBtn = createActionButton("👁️", "View details", e -> openTaskDetails(t));
        actionsPanel.add(viewBtn);

        JButton commentBtn = createActionButton("💬", "Comments & Collaboration", e -> openTaskComments(t));
        actionsPanel.add(commentBtn);

        JButton attachBtn = createActionButton("📎", "Attachments & Files", e -> openTaskAttachments(t));
        actionsPanel.add(attachBtn);

        card.add(actionsPanel, BorderLayout.EAST);

        return card;
    }

    private JButton createActionButton(String icon, String tooltip, ActionListener listener) {
        JButton btn = new JButton(icon);
        btn.setToolTipText(tooltip);
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(28, 28));
        btn.addActionListener(listener);
        return btn;
    }

    private void openTaskDetails(Task task) {
        TaskManagementView taskView = (TaskManagementView) mainFrame.getView("tasks");
        if (taskView != null) {
            taskView.showTaskDetails(task);
        }
    }

    private void openTaskComments(Task task) {
        TaskManagementView taskView = (TaskManagementView) mainFrame.getView("tasks");
        if (taskView != null) {
            taskView.showTaskCommentsDialog(task);
        }
    }

    private void openTaskAttachments(Task task) {
        TaskManagementView taskView = (TaskManagementView) mainFrame.getView("tasks");
        if (taskView != null) {
            taskView.showTaskAttachmentsDialog(task);
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

    private Color getStatusColor(TaskStatus status) {
        return switch (status) {
            case TO_DO -> new Color(148, 163, 184);
            case IN_PROGRESS -> new Color(59, 130, 246);
            case TESTING -> new Color(245, 158, 11);
            case COMPLETED -> ThemeManager.COLOR_SUCCESS;
            case BLOCKED -> ThemeManager.COLOR_DANGER;
        };
    }
}

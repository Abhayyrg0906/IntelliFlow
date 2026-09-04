package com.intelliflow.ui.views;

import com.intelliflow.context.UserSession;
import com.intelliflow.model.ActivityLog;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ActivityLogsView extends BaseView {
    private final MainFrame mainFrame;

    private ModernTable logsTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> actionFilterCombo;
    private JTextField searchField;
    private List<User> allUsers = new ArrayList<>();
    private List<ActivityLog> fetchedLogs = new ArrayList<>();
    private JPanel tableContainer;
    private JScrollPane scrollPane;
    private EmptyStatePanel emptyPanel;

    public ActivityLogsView(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(15, 15));
        setBackground(ThemeManager.COLOR_BACKGROUND);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        initToolbar();
        initTable();
    }

    private void initToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);

        // Left title & description
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);

        JLabel title = new JLabel("📜 Activity & Audit Timeline");
        title.setFont(ThemeManager.FONT_SUBTITLE);
        title.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        leftPanel.add(title);

        // Right search & filter
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);
        searchLabel.setFont(ThemeManager.FONT_BODY);

        searchField = new JTextField(15);
        searchField.setBackground(ThemeManager.COLOR_CARD);
        searchField.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        searchField.setFont(ThemeManager.FONT_BODY);
        searchField.putClientProperty("JTextField.placeholderText", "Filter activity...");
        searchField.addActionListener(e -> applyFilters());

        JLabel filterLabel = new JLabel("Action:");
        filterLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);
        filterLabel.setFont(ThemeManager.FONT_BODY);

        actionFilterCombo = new JComboBox<>(new String[]{
                "All Actions",
                "TASK_CREATE",
                "TASK_ASSIGN",
                "TASK_PRIORITY_CHANGE",
                "TASK_DEADLINE_CHANGE",
                "TASK_STATUS_CHANGE",
                "PROJECT_CREATE",
                "PROJECT_STATUS_CHANGE",
                "PROJECT_DEADLINE_CHANGE",
                "PROJECT_MANAGER_ASSIGN",
                "USER_CREATE",
                "USER_ROLE_CHANGE"
        });
        actionFilterCombo.setBackground(ThemeManager.COLOR_CARD);
        actionFilterCombo.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        actionFilterCombo.setFont(ThemeManager.FONT_BODY);
        actionFilterCombo.addActionListener(e -> applyFilters());

        JButton filterBtn = new JButton("🔍 Filter");
        filterBtn.setBackground(ThemeManager.COLOR_PRIMARY);
        filterBtn.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        filterBtn.setFont(ThemeManager.FONT_BOLD_SMALL);
        filterBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        filterBtn.addActionListener(e -> applyFilters());

        rightPanel.add(searchLabel);
        rightPanel.add(searchField);
        rightPanel.add(filterLabel);
        rightPanel.add(actionFilterCombo);
        rightPanel.add(filterBtn);

        toolbar.add(leftPanel, BorderLayout.WEST);
        toolbar.add(rightPanel, BorderLayout.EAST);

        add(toolbar, BorderLayout.NORTH);
    }

    private void initTable() {
        tableContainer = new RoundedPanel(12, ThemeManager.COLOR_CARD);
        tableContainer.setLayout(new BorderLayout());
        tableContainer.setBorder(new EmptyBorder(15, 15, 15, 15));

        tableModel = new DefaultTableModel(
                new Object[]{"Time", "Actor / User", "Action Event", "Timeline Description"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        logsTable = new ModernTable();
        logsTable.setPlaceholderText("No activity timeline events found.");
        logsTable.setModel(tableModel);

        scrollPane = new JScrollPane(logsTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        tableContainer.add(scrollPane, BorderLayout.CENTER);

        add(tableContainer, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) return;

        SwingWorker<List<ActivityLog>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<ActivityLog> doInBackground() throws Exception {
                // Fetch users first for name mapping
                allUsers = mainFrame.getUserService().getAllUsers();
                return mainFrame.getUserService().getActivityLogsForUser(currentUser);
            }

            @Override
            protected void done() {
                try {
                    fetchedLogs = get();
                    applyFilters();
                } catch (Exception e) {
                    System.err.println("Failed to reload system activity timeline: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void applyFilters() {
        String filterAction = (String) actionFilterCombo.getSelectedItem();
        String searchText = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";

        List<ActivityLog> filtered = fetchedLogs.stream().filter(log -> {
            if (filterAction != null && !"All Actions".equals(filterAction)) {
                if (!filterAction.equalsIgnoreCase(log.getAction())) {
                    return false;
                }
            }
            if (!searchText.isEmpty()) {
                String desc = log.getDescription() != null ? log.getDescription().toLowerCase() : "";
                String action = log.getAction() != null ? log.getAction().toLowerCase() : "";
                if (!desc.contains(searchText) && !action.contains(searchText)) {
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toList());

        tableModel.setRowCount(0);
        tableContainer.removeAll();

        if (filtered.isEmpty()) {
            emptyPanel = new EmptyStatePanel(
                    "📜",
                    "No activity timeline events found.",
                    "No audit events matched the selected filters or permission scope.",
                    null,
                    null
            );
            tableContainer.add(emptyPanel, BorderLayout.CENTER);
        } else {
            tableContainer.add(scrollPane, BorderLayout.CENTER);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");

            for (ActivityLog log : filtered) {
                String userRepresentation = "SYSTEM";
                if (log.getUserId() != null) {
                    Optional<User> uOpt = allUsers.stream()
                            .filter(u -> u.getId() == log.getUserId())
                            .findFirst();
                    if (uOpt.isPresent()) {
                        userRepresentation = uOpt.get().getFullName() + " (" + uOpt.get().getRole() + ")";
                    } else {
                        userRepresentation = "User ID: " + log.getUserId();
                    }
                }

                String actionDisplay = formatActionDisplay(log.getAction());

                tableModel.addRow(new Object[]{
                        log.getTimestamp() != null ? log.getTimestamp().format(formatter) : "",
                        userRepresentation,
                        actionDisplay,
                        log.getDescription()
                });
            }
        }
        tableContainer.revalidate();
        tableContainer.repaint();
    }

    private String formatActionDisplay(String action) {
        if (action == null) return "";
        return switch (action) {
            case "TASK_CREATE" -> "📝 TASK CREATED";
            case "TASK_ASSIGN" -> "👤 TASK ASSIGNED";
            case "TASK_PRIORITY_CHANGE" -> "⚡ PRIORITY CHANGED";
            case "TASK_DEADLINE_CHANGE" -> "📅 DEADLINE CHANGED";
            case "TASK_STATUS_CHANGE" -> "🔄 STATUS CHANGED";
            case "PROJECT_CREATE" -> "📂 PROJECT CREATED";
            case "PROJECT_STATUS_CHANGE" -> "📊 PROJECT STATUS";
            case "PROJECT_DEADLINE_CHANGE" -> "📅 PROJECT DEADLINE";
            case "PROJECT_MANAGER_ASSIGN" -> "👔 MANAGER ASSIGNED";
            case "USER_CREATE", "USER_REGISTER" -> "✨ USER CREATED";
            case "USER_ROLE_CHANGE" -> "🔑 ROLE CHANGED";
            case "USER_DELETE" -> "🗑️ USER DELETED";
            default -> action;
        };
    }
}

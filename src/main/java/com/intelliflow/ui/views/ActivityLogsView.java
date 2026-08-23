package com.intelliflow.ui.views;

import com.intelliflow.model.ActivityLog;
import com.intelliflow.model.User;
import com.intelliflow.ui.MainFrame;
import com.intelliflow.ui.ThemeManager;
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

public class ActivityLogsView extends BaseView {
    private final MainFrame mainFrame;

    private ModernTable logsTable;
    private DefaultTableModel tableModel;
    private List<User> allUsers = new ArrayList<>();
    private JPanel tableContainer;
    private JScrollPane scrollPane;
    private com.intelliflow.ui.components.EmptyStatePanel emptyPanel;

    public ActivityLogsView(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(15, 15));
        setBackground(ThemeManager.COLOR_BACKGROUND);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        initTable();
    }

    private void initTable() {
        tableContainer = new RoundedPanel(12, ThemeManager.COLOR_CARD);
        tableContainer.setLayout(new BorderLayout());
        tableContainer.setBorder(new EmptyBorder(15, 15, 15, 15));

        tableModel = new DefaultTableModel(
                new Object[]{"ID", "Timestamp", "User Account", "Action", "Audit Description"}, 0
        );
        logsTable = new ModernTable();
        logsTable.setPlaceholderText("No system activity logs found.");
        logsTable.setModel(tableModel);

        scrollPane = new JScrollPane(logsTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        tableContainer.add(scrollPane, BorderLayout.CENTER);

        add(tableContainer, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        SwingWorker<List<ActivityLog>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<ActivityLog> doInBackground() throws Exception {
                // Fetch users first for name mapping
                allUsers = mainFrame.getUserService().getAllUsers();
                return mainFrame.getUserService().getActivityLogs();
            }

            @Override
            protected void done() {
                try {
                    List<ActivityLog> logs = get();
                    tableModel.setRowCount(0);
                    
                    tableContainer.removeAll();
                    if (logs.isEmpty()) {
                        emptyPanel = new com.intelliflow.ui.components.EmptyStatePanel(
                                "🚫",
                                "No system activity logs found.",
                                "Audit database has not recorded any lifecycle operations yet.",
                                null,
                                null
                        );
                        tableContainer.add(emptyPanel, BorderLayout.CENTER);
                    } else {
                        tableContainer.add(scrollPane, BorderLayout.CENTER);
                        
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        for (ActivityLog log : logs) {
                            String userRepresentation = "SYSTEM";
                            if (log.getUserId() != null) {
                                Optional<User> uOpt = allUsers.stream()
                                        .filter(u -> u.getId() == log.getUserId())
                                        .findFirst();
                                if (uOpt.isPresent()) {
                                    userRepresentation = uOpt.get().getUsername() + " (" + uOpt.get().getFullName() + ")";
                                } else {
                                    userRepresentation = "User ID: " + log.getUserId();
                                }
                            }

                            tableModel.addRow(new Object[]{
                                    log.getId(),
                                    log.getTimestamp() != null ? log.getTimestamp().format(formatter) : "",
                                    userRepresentation,
                                    log.getAction(),
                                    log.getDescription()
                            });
                        }
                    }
                    tableContainer.revalidate();
                    tableContainer.repaint();
                } catch (Exception e) {
                    System.err.println("Failed to reload system activity logs: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
}

package com.intelliflow.ui.views;

import com.intelliflow.context.UserSession;
import com.intelliflow.model.Notification;
import com.intelliflow.model.User;
import com.intelliflow.ui.MainFrame;
import com.intelliflow.ui.ThemeManager;
import com.intelliflow.ui.components.ModernTable;
import com.intelliflow.ui.components.RoundedPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NotificationsView extends BaseView {
    private final MainFrame mainFrame;

    private ModernTable notificationTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> filterCombo;
    private JButton readBtn;
    private JButton readAllBtn;
    private JButton deleteBtn;
    private JButton clearAllBtn;

    private List<Notification> allNotifications = new ArrayList<>();

    public NotificationsView(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(15, 15));
        setBackground(ThemeManager.COLOR_BACKGROUND);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        initActionBar();
        initTable();
    }

    private void initActionBar() {
        JPanel actionBar = new JPanel(new BorderLayout());
        actionBar.setOpaque(false);

        // Left buttons
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);

        readBtn = new JButton("✔ Mark Read");
        readBtn.setBackground(ThemeManager.COLOR_CARD);
        readBtn.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        readBtn.setFont(ThemeManager.FONT_BOLD_SMALL);
        readBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        readBtn.addActionListener(e -> handleMarkRead());

        readAllBtn = new JButton("✔ Mark All Read");
        readAllBtn.setBackground(ThemeManager.COLOR_PRIMARY);
        readAllBtn.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        readAllBtn.setFont(ThemeManager.FONT_BOLD_SMALL);
        readAllBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        readAllBtn.addActionListener(e -> handleMarkAllRead());

        deleteBtn = new JButton("🗑️ Delete");
        deleteBtn.setBackground(ThemeManager.COLOR_DANGER);
        deleteBtn.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        deleteBtn.setFont(ThemeManager.FONT_BOLD_SMALL);
        deleteBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        deleteBtn.addActionListener(e -> handleDelete());

        clearAllBtn = new JButton("🧹 Clear All");
        clearAllBtn.setBackground(new Color(185, 28, 28));
        clearAllBtn.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        clearAllBtn.setFont(ThemeManager.FONT_BOLD_SMALL);
        clearAllBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clearAllBtn.addActionListener(e -> handleClearAll());

        leftPanel.add(readBtn);
        leftPanel.add(readAllBtn);
        leftPanel.add(deleteBtn);
        leftPanel.add(clearAllBtn);

        // Right filter
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        JLabel filterLabel = new JLabel("Filter:");
        filterLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);
        filterLabel.setFont(ThemeManager.FONT_BODY);

        filterCombo = new JComboBox<>(new String[]{"All Notifications", "Unread Only"});
        filterCombo.setBackground(ThemeManager.COLOR_CARD);
        filterCombo.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        filterCombo.setFont(ThemeManager.FONT_BODY);
        filterCombo.addActionListener(e -> applyFilter());

        rightPanel.add(filterLabel);
        rightPanel.add(filterCombo);

        actionBar.add(leftPanel, BorderLayout.WEST);
        actionBar.add(rightPanel, BorderLayout.EAST);

        add(actionBar, BorderLayout.NORTH);
    }

    private void initTable() {
        JPanel tableContainer = new RoundedPanel(12, ThemeManager.COLOR_CARD);
        tableContainer.setLayout(new BorderLayout());
        tableContainer.setBorder(new EmptyBorder(15, 15, 15, 15));

        tableModel = new DefaultTableModel(
                new Object[]{"ID", "Notification Alert Message", "Status", "Received Timestamp"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        notificationTable = new ModernTable();
        notificationTable.setModel(tableModel);

        JScrollPane scrollPane = new JScrollPane(notificationTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        tableContainer.add(scrollPane, BorderLayout.CENTER);

        add(tableContainer, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) return;

        SwingWorker<List<Notification>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Notification> doInBackground() throws Exception {
                // Check and trigger deadline notifications dynamically
                try {
                    mainFrame.getNotificationService().checkAndGenerateDeadlineNotifications(LocalDate.now());
                } catch (Exception ignored) {}

                return mainFrame.getNotificationService().getNotificationsForUser(currentUser.getId());
            }

            @Override
            protected void done() {
                try {
                    allNotifications = get();
                    applyFilter();
                    mainFrame.updateNotificationCount();
                } catch (Exception e) {
                    System.err.println("Failed to reload notifications: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void applyFilter() {
        tableModel.setRowCount(0);
        String selected = (String) filterCombo.getSelectedItem();
        boolean unreadOnly = "Unread Only".equals(selected);

        List<Notification> filtered = unreadOnly
                ? allNotifications.stream().filter(n -> !n.isRead()).collect(Collectors.toList())
                : allNotifications;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (Notification n : filtered) {
            tableModel.addRow(new Object[]{
                    n.getId(),
                    n.getMessage(),
                    n.isRead() ? "Read" : "🔵 Unread (New)",
                    n.getCreatedAt() != null ? n.getCreatedAt().format(formatter) : ""
            });
        }
    }

    private void handleMarkRead() {
        int selectedRow = notificationTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a notification row to mark as read.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int notifId = (int) tableModel.getValueAt(selectedRow, 0);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                mainFrame.getNotificationService().markAsRead(notifId);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    refresh();
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private void handleMarkAllRead() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) return;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                mainFrame.getNotificationService().markAllAsRead(currentUser.getId());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    refresh();
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private void handleDelete() {
        int selectedRow = notificationTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a notification to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int notifId = (int) tableModel.getValueAt(selectedRow, 0);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                mainFrame.getNotificationService().deleteNotification(notifId);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    refresh();
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private void handleClearAll() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) return;

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to clear all your notifications?",
                "Confirm Clear All",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) return;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                mainFrame.getNotificationService().deleteAllNotificationsForUser(currentUser.getId());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    refresh();
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }
}


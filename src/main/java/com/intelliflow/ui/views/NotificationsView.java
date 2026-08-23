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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class NotificationsView extends BaseView {
    private final MainFrame mainFrame;

    private ModernTable notificationTable;
    private DefaultTableModel tableModel;
    private JButton readBtn;
    private JButton readAllBtn;
    private JButton deleteBtn;

    private List<Notification> displayedNotifications = new ArrayList<>();

    public NotificationsView(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(15, 15));
        setBackground(ThemeManager.COLOR_BACKGROUND);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        initActionBar();
        initTable();
    }

    private void initActionBar() {
        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionBar.setOpaque(false);

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

        actionBar.add(readBtn);
        actionBar.add(readAllBtn);
        actionBar.add(deleteBtn);

        add(actionBar, BorderLayout.NORTH);
    }

    private void initTable() {
        JPanel tableContainer = new RoundedPanel(12, ThemeManager.COLOR_CARD);
        tableContainer.setLayout(new BorderLayout());
        tableContainer.setBorder(new EmptyBorder(15, 15, 15, 15));

        tableModel = new DefaultTableModel(
                new Object[]{"ID", "Alert Message", "Status", "Received Date"}, 0
        );
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
                return mainFrame.getNotificationService().getNotificationsForUser(currentUser.getId());
            }

            @Override
            protected void done() {
                try {
                    displayedNotifications = get();
                    tableModel.setRowCount(0);
                    
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

                    for (Notification n : displayedNotifications) {
                        tableModel.addRow(new Object[]{
                                n.getId(),
                                n.getMessage(),
                                n.isRead() ? "Read" : "Unread (New)",
                                n.getCreatedAt() != null ? n.getCreatedAt().format(formatter) : ""
                        });
                    }
                } catch (Exception e) {
                    System.err.println("Failed to reload notifications: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void handleMarkRead() {
        int selectedRow = notificationTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select an alert row to mark as read.", "Selection Required", JOptionPane.WARNING_MESSAGE);
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
                    mainFrame.updateNotificationCount();
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
                    mainFrame.updateNotificationCount();
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private void handleDelete() {
        int selectedRow = notificationTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select an alert to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
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
                    mainFrame.updateNotificationCount();
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }
}

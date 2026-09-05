package com.intelliflow.ui.views;

import com.intelliflow.context.UserSession;
import com.intelliflow.enums.Role;
import com.intelliflow.exception.UnauthorizedException;
import com.intelliflow.exception.ValidationException;
import com.intelliflow.model.User;
import com.intelliflow.ui.MainFrame;
import com.intelliflow.ui.ThemeManager;
import com.intelliflow.ui.components.EmptyStatePanel;
import com.intelliflow.ui.components.ModernTable;
import com.intelliflow.ui.components.RoundedPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class UserManagementView extends BaseView {
    private final MainFrame mainFrame;

    private ModernTable userTable;
    private DefaultTableModel tableModel;
    private JPanel tableContainer;
    private JScrollPane scrollPane;
    private EmptyStatePanel emptyPanel;

    // Search and Filters
    private JTextField searchField;
    private JComboBox<String> roleFilterCombo;
    private JComboBox<String> statusFilterCombo;

    // Action Buttons
    private JButton createButton;
    private JButton editButton;
    private JButton toggleStatusButton;
    private JButton deleteButton;
    private JButton refreshButton;

    private List<User> displayedUsers = new ArrayList<>();

    public UserManagementView(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(15, 15));
        setBackground(ThemeManager.COLOR_BACKGROUND);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        initToolbar();
        initTable();
    }

    private void initToolbar() {
        JPanel northPanel = new JPanel(new BorderLayout(10, 10));
        northPanel.setOpaque(false);

        // Top Row: Action Buttons
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionRow.setOpaque(false);

        createButton = new JButton("➕ Register New User");
        createButton.setBackground(ThemeManager.COLOR_PRIMARY);
        createButton.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        createButton.setFont(ThemeManager.FONT_BOLD_SMALL);
        createButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        createButton.addActionListener(e -> showRegisterUserForm());

        editButton = new JButton("✏️ Edit User");
        editButton.setBackground(ThemeManager.COLOR_PRIMARY_HOVER);
        editButton.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        editButton.setFont(ThemeManager.FONT_BOLD_SMALL);
        editButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        editButton.addActionListener(e -> handleEditAction());

        toggleStatusButton = new JButton("⚡ Toggle Status");
        toggleStatusButton.setBackground(ThemeManager.COLOR_WARNING);
        toggleStatusButton.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        toggleStatusButton.setFont(ThemeManager.FONT_BOLD_SMALL);
        toggleStatusButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggleStatusButton.addActionListener(e -> handleToggleStatusAction());

        deleteButton = new JButton("🗑️ Delete User");
        deleteButton.setBackground(ThemeManager.COLOR_DANGER);
        deleteButton.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        deleteButton.setFont(ThemeManager.FONT_BOLD_SMALL);
        deleteButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        deleteButton.addActionListener(e -> handleDeleteAction());

        refreshButton = new JButton("🔄 Refresh");
        refreshButton.setBackground(ThemeManager.COLOR_BORDER);
        refreshButton.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        refreshButton.setFont(ThemeManager.FONT_BOLD_SMALL);
        refreshButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshButton.addActionListener(e -> refresh());

        actionRow.add(createButton);
        actionRow.add(editButton);
        actionRow.add(toggleStatusButton);
        actionRow.add(deleteButton);
        actionRow.add(refreshButton);

        // Bottom Row: Search & Filters Bar
        JPanel filterRow = new RoundedPanel(10, ThemeManager.COLOR_CARD);
        filterRow.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 8));

        JLabel searchIcon = new JLabel("🔍");
        searchIcon.setFont(new Font("SansSerif", Font.PLAIN, 14));
        searchIcon.setForeground(ThemeManager.COLOR_TEXT_MUTED);

        searchField = new JTextField(22);
        searchField.setPreferredSize(new Dimension(240, 30));
        searchField.putClientProperty("JTextField.placeholderText", "Search name, username, or email...");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { refresh(); }
            @Override
            public void removeUpdate(DocumentEvent e) { refresh(); }
            @Override
            public void changedUpdate(DocumentEvent e) { refresh(); }
        });

        JLabel roleLabel = new JLabel("Role:");
        roleLabel.setFont(ThemeManager.FONT_BOLD_SMALL);
        roleLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);

        roleFilterCombo = new JComboBox<>(new String[]{"All Roles", "ADMIN", "MANAGER", "EMPLOYEE"});
        roleFilterCombo.setPreferredSize(new Dimension(130, 30));
        roleFilterCombo.addActionListener(e -> refresh());

        JLabel statusLabel = new JLabel("Status:");
        statusLabel.setFont(ThemeManager.FONT_BOLD_SMALL);
        statusLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);

        statusFilterCombo = new JComboBox<>(new String[]{"All Statuses", "Active", "Inactive"});
        statusFilterCombo.setPreferredSize(new Dimension(130, 30));
        statusFilterCombo.addActionListener(e -> refresh());

        JButton clearBtn = new JButton("Clear Filters");
        clearBtn.setFont(ThemeManager.FONT_SMALL);
        clearBtn.setBackground(ThemeManager.COLOR_BORDER);
        clearBtn.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        clearBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            roleFilterCombo.setSelectedIndex(0);
            statusFilterCombo.setSelectedIndex(0);
            refresh();
        });

        filterRow.add(searchIcon);
        filterRow.add(searchField);
        filterRow.add(Box.createHorizontalStrut(5));
        filterRow.add(roleLabel);
        filterRow.add(roleFilterCombo);
        filterRow.add(Box.createHorizontalStrut(5));
        filterRow.add(statusLabel);
        filterRow.add(statusFilterCombo);
        filterRow.add(Box.createHorizontalStrut(5));
        filterRow.add(clearBtn);

        northPanel.add(actionRow, BorderLayout.NORTH);
        northPanel.add(filterRow, BorderLayout.SOUTH);

        add(northPanel, BorderLayout.NORTH);
    }

    private void initTable() {
        tableContainer = new RoundedPanel(12, ThemeManager.COLOR_CARD);
        tableContainer.setLayout(new BorderLayout());
        tableContainer.setBorder(new EmptyBorder(15, 15, 15, 15));

        tableModel = new DefaultTableModel(
                new Object[]{"ID", "Username", "Full Name", "Email Address", "Role", "Account Status", "Created Date"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        userTable = new ModernTable();
        userTable.setPlaceholderText("No matching users found.");
        userTable.setModel(tableModel);

        // Custom column styling
        userTable.getColumnModel().getColumn(0).setMaxWidth(60);
        userTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        userTable.getColumnModel().getColumn(5).setPreferredWidth(120);

        // Status Renderer
        userTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value != null) {
                    String str = value.toString();
                    if (str.contains("Active")) {
                        c.setForeground(new Color(46, 204, 113)); // Bright Green
                    } else if (str.contains("Inactive")) {
                        c.setForeground(new Color(231, 76, 60)); // Red
                    }
                }
                if (isSelected) {
                    c.setForeground(Color.WHITE);
                }
                return c;
            }
        });

        // Double click to edit user
        userTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && userTable.getSelectedRow() >= 0) {
                    handleEditAction();
                }
            }
        });

        scrollPane = new JScrollPane(userTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        tableContainer.add(scrollPane, BorderLayout.CENTER);

        add(tableContainer, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        String query = searchField != null ? searchField.getText().trim() : "";
        
        Role roleFilter = null;
        if (roleFilterCombo != null && roleFilterCombo.getSelectedIndex() > 0) {
            String sel = (String) roleFilterCombo.getSelectedItem();
            try {
                roleFilter = Role.fromString(sel);
            } catch (Exception ignored) {}
        }

        Boolean statusFilter = null;
        if (statusFilterCombo != null && statusFilterCombo.getSelectedIndex() > 0) {
            String sel = (String) statusFilterCombo.getSelectedItem();
            if ("Active".equalsIgnoreCase(sel)) {
                statusFilter = true;
            } else if ("Inactive".equalsIgnoreCase(sel)) {
                statusFilter = false;
            }
        }

        final Role finalRole = roleFilter;
        final Boolean finalStatus = statusFilter;
        final String finalQuery = query;

        SwingWorker<List<User>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<User> doInBackground() throws Exception {
                return mainFrame.getUserService().searchUsers(finalQuery, finalRole, finalStatus);
            }

            @Override
            protected void done() {
                try {
                    displayedUsers = get();
                    tableModel.setRowCount(0);

                    tableContainer.removeAll();
                    if (displayedUsers.isEmpty()) {
                        User currentUser = UserSession.getInstance().getCurrentUser();
                        boolean isWriteable = currentUser != null && currentUser.getRole() == Role.ADMIN;
                        emptyPanel = new EmptyStatePanel(
                                "👥",
                                "No users found.",
                                "No user accounts match the selected search criteria.",
                                isWriteable ? "➕ Register User" : null,
                                isWriteable ? e -> showRegisterUserForm() : null
                        );
                        tableContainer.add(emptyPanel, BorderLayout.CENTER);
                    } else {
                        tableContainer.add(scrollPane, BorderLayout.CENTER);

                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                        for (User u : displayedUsers) {
                            String statusStr = u.isActive() ? "🟢 Active" : "🔴 Inactive";
                            tableModel.addRow(new Object[]{
                                    u.getId(),
                                    u.getUsername(),
                                    u.getFullName(),
                                    u.getEmail(),
                                    u.getRole().toString(),
                                    statusStr,
                                    u.getCreatedAt() != null ? u.getCreatedAt().format(formatter) : ""
                            });
                        }
                    }
                    tableContainer.revalidate();
                    tableContainer.repaint();
                } catch (Exception e) {
                    System.err.println("Failed to reload user lists: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private User getSelectedUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow < 0) {
            return null;
        }
        int userId = (int) userTable.getValueAt(selectedRow, 0);
        return displayedUsers.stream().filter(u -> u.getId() == userId).findFirst().orElse(null);
    }

    private void handleEditAction() {
        User user = getSelectedUser();
        if (user == null) {
            JOptionPane.showMessageDialog(this, "Please select a user to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        showEditUserForm(user);
    }

    private void handleToggleStatusAction() {
        User user = getSelectedUser();
        if (user == null) {
            JOptionPane.showMessageDialog(this, "Please select a user to toggle status.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        User current = UserSession.getInstance().getCurrentUser();
        if (current != null && current.getId() == user.getId() && user.isActive()) {
            JOptionPane.showMessageDialog(this, "You cannot deactivate your own active administrator account.", "Invalid Action", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String actionWord = user.isActive() ? "deactivate" : "activate";
        String nextStatus = user.isActive() ? "INACTIVE" : "ACTIVE";

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to " + actionWord + " user '" + user.getUsername() + "' (ID: " + user.getId() + ")?\n"
                        + "Account status will be set to: " + nextStatus,
                "Confirm Status Change",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            boolean targetStatus = !user.isActive();
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    mainFrame.getUserService().setUserActiveStatus(user.getId(), targetStatus);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        refresh();
                    } catch (Exception e) {
                        String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                        JOptionPane.showMessageDialog(UserManagementView.this, "Failed to update user status: " + msg, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }

    private void handleDeleteAction() {
        User user = getSelectedUser();
        if (user == null) {
            JOptionPane.showMessageDialog(this, "Please select a user to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        User current = UserSession.getInstance().getCurrentUser();
        if (current != null && current.getId() == user.getId()) {
            JOptionPane.showMessageDialog(this, "You cannot delete your own active administrator account.", "Invalid Action", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to permanently delete user: '" + user.getUsername() + "'?\nThis will nullify their project/task assignments.",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    mainFrame.getUserService().deleteUser(user.getId());
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        refresh();
                    } catch (Exception e) {
                        String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                        JOptionPane.showMessageDialog(UserManagementView.this, "Failed to delete user: " + msg, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }

    private void showRegisterUserForm() {
        JDialog dialog = new JDialog(mainFrame, "Register New Account", true);
        dialog.setSize(440, 520);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 20, 8, 20);
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // Username
        dialog.add(new JLabel("Username:"), gbc);
        gbc.gridy++;
        JTextField userField = new JTextField();
        userField.setPreferredSize(new Dimension(300, 32));
        userField.putClientProperty("JTextField.placeholderText", "Enter username");
        dialog.add(userField, gbc);

        // Email
        gbc.gridy++;
        dialog.add(new JLabel("Email Address:"), gbc);
        gbc.gridy++;
        JTextField emailField = new JTextField();
        emailField.setPreferredSize(new Dimension(300, 32));
        emailField.putClientProperty("JTextField.placeholderText", "Enter email address");
        dialog.add(emailField, gbc);

        // Full Name
        gbc.gridy++;
        dialog.add(new JLabel("Full Name:"), gbc);
        gbc.gridy++;
        JTextField nameField = new JTextField();
        nameField.setPreferredSize(new Dimension(300, 32));
        nameField.putClientProperty("JTextField.placeholderText", "Enter full name");
        dialog.add(nameField, gbc);

        // Role Combobox (Strictly ADMIN, MANAGER, EMPLOYEE)
        gbc.gridy++;
        dialog.add(new JLabel("Security Role:"), gbc);
        gbc.gridy++;
        JComboBox<Role> roleCombo = new JComboBox<>(new Role[]{Role.ADMIN, Role.MANAGER, Role.EMPLOYEE});
        dialog.add(roleCombo, gbc);

        // Password
        gbc.gridy++;
        dialog.add(new JLabel("Temp Password:"), gbc);
        gbc.gridy++;
        JTextField passField = new JTextField("TempPass123!");
        passField.setPreferredSize(new Dimension(300, 32));
        dialog.add(passField, gbc);

        // Buttons
        gbc.gridy++;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton saveBtn = new JButton("Register");
        saveBtn.setBackground(ThemeManager.COLOR_PRIMARY);
        saveBtn.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);

        saveBtn.addActionListener(e -> {
            String username = userField.getText().trim();
            String email = emailField.getText().trim();
            String fullName = nameField.getText().trim();
            Role role = (Role) roleCombo.getSelectedItem();
            String password = passField.getText().trim();

            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setFullName(fullName);
            user.setRole(role);
            user.setActive(true);

            try {
                mainFrame.getUserService().register(user, password);
                dialog.dispose();
                refresh();
            } catch (ValidationException | UnauthorizedException ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Validation Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Failed to register user due to database error.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        buttonPanel.add(cancelBtn);
        buttonPanel.add(saveBtn);
        dialog.add(buttonPanel, gbc);

        dialog.setVisible(true);
    }

    private void showEditUserForm(User user) {
        JDialog dialog = new JDialog(mainFrame, "Edit User Account: " + user.getUsername(), true);
        dialog.setSize(440, 480);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 20, 8, 20);
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // Username (Immutable / Display-only)
        dialog.add(new JLabel("Username:"), gbc);
        gbc.gridy++;
        JTextField userField = new JTextField(user.getUsername());
        userField.setPreferredSize(new Dimension(300, 32));
        userField.setEditable(false);
        userField.setBackground(ThemeManager.COLOR_BACKGROUND);
        dialog.add(userField, gbc);

        // Full Name
        gbc.gridy++;
        dialog.add(new JLabel("Full Name:"), gbc);
        gbc.gridy++;
        JTextField nameField = new JTextField(user.getFullName());
        nameField.setPreferredSize(new Dimension(300, 32));
        dialog.add(nameField, gbc);

        // Email
        gbc.gridy++;
        dialog.add(new JLabel("Email Address:"), gbc);
        gbc.gridy++;
        JTextField emailField = new JTextField(user.getEmail());
        emailField.setPreferredSize(new Dimension(300, 32));
        dialog.add(emailField, gbc);

        // Role Combobox (Strictly ADMIN, MANAGER, EMPLOYEE)
        gbc.gridy++;
        dialog.add(new JLabel("Security Role:"), gbc);
        gbc.gridy++;
        JComboBox<Role> roleCombo = new JComboBox<>(new Role[]{Role.ADMIN, Role.MANAGER, Role.EMPLOYEE});
        roleCombo.setSelectedItem(user.getRole());
        dialog.add(roleCombo, gbc);

        // Account Status
        gbc.gridy++;
        dialog.add(new JLabel("Account Status:"), gbc);
        gbc.gridy++;
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Active (Enabled)", "Inactive (Deactivated)"});
        statusCombo.setSelectedIndex(user.isActive() ? 0 : 1);
        dialog.add(statusCombo, gbc);

        // Self modification warning if editing own active admin account
        User current = UserSession.getInstance().getCurrentUser();
        boolean isSelf = (current != null && current.getId() == user.getId());
        if (isSelf) {
            roleCombo.setEnabled(false);
            statusCombo.setEnabled(false);
        }

        // Buttons
        gbc.gridy++;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton saveBtn = new JButton("Save Changes");
        saveBtn.setBackground(ThemeManager.COLOR_PRIMARY);
        saveBtn.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);

        saveBtn.addActionListener(e -> {
            String email = emailField.getText().trim();
            String fullName = nameField.getText().trim();
            Role role = isSelf ? user.getRole() : (Role) roleCombo.getSelectedItem();
            boolean active = isSelf ? user.isActive() : (statusCombo.getSelectedIndex() == 0);

            User updatedUser = new User();
            updatedUser.setId(user.getId());
            updatedUser.setUsername(user.getUsername());
            updatedUser.setEmail(email);
            updatedUser.setFullName(fullName);
            updatedUser.setRole(role);
            updatedUser.setActive(active);
            updatedUser.setPasswordHash(user.getPasswordHash());
            updatedUser.setCreatedAt(user.getCreatedAt());

            try {
                mainFrame.getUserService().updateUser(updatedUser);
                dialog.dispose();
                refresh();
            } catch (ValidationException | UnauthorizedException ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Validation Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                JOptionPane.showMessageDialog(dialog, "Failed to update user: " + msg, "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        buttonPanel.add(cancelBtn);
        buttonPanel.add(saveBtn);
        dialog.add(buttonPanel, gbc);

        dialog.setVisible(true);
    }
}

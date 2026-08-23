package com.intelliflow.ui.views;

import com.intelliflow.enums.Role;
import com.intelliflow.exception.ValidationException;
import com.intelliflow.model.User;
import com.intelliflow.ui.MainFrame;
import com.intelliflow.ui.ThemeManager;
import com.intelliflow.ui.components.ModernTable;
import com.intelliflow.ui.components.RoundedPanel;
import com.intelliflow.ui.components.EmptyStatePanel;
import com.intelliflow.context.UserSession;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class UserManagementView extends BaseView {
    private final MainFrame mainFrame;

    private ModernTable userTable;
    private DefaultTableModel tableModel;
    private JButton createButton;
    private JButton deleteButton;
    private JPanel tableContainer;
    private JScrollPane scrollPane;
    private EmptyStatePanel emptyPanel;
    
    private List<User> displayedUsers = new ArrayList<>();

    public UserManagementView(MainFrame mainFrame) {
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

        createButton = new JButton("➕ Register New User");
        createButton.setBackground(ThemeManager.COLOR_PRIMARY);
        createButton.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        createButton.setFont(ThemeManager.FONT_BOLD_SMALL);
        createButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        createButton.addActionListener(e -> showRegisterUserForm());

        deleteButton = new JButton("🗑️ Delete User");
        deleteButton.setBackground(ThemeManager.COLOR_DANGER);
        deleteButton.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        deleteButton.setFont(ThemeManager.FONT_BOLD_SMALL);
        deleteButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        deleteButton.addActionListener(e -> handleDeleteAction());

        actionBar.add(createButton);
        actionBar.add(deleteButton);
        add(actionBar, BorderLayout.NORTH);
    }

    private void initTable() {
        tableContainer = new RoundedPanel(12, ThemeManager.COLOR_CARD);
        tableContainer.setLayout(new BorderLayout());
        tableContainer.setBorder(new EmptyBorder(15, 15, 15, 15));

        tableModel = new DefaultTableModel(
                new Object[]{"ID", "Username", "Email Address", "Role", "Full Name", "Created Date"}, 0
        );
        userTable = new ModernTable();
        userTable.setPlaceholderText("No users found.");
        userTable.setModel(tableModel);

        scrollPane = new JScrollPane(userTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        tableContainer.add(scrollPane, BorderLayout.CENTER);

        add(tableContainer, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        SwingWorker<List<User>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<User> doInBackground() throws Exception {
                return mainFrame.getUserService().getAllUsers();
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
                                "Platform user directory is empty.",
                                isWriteable ? "➕ Register User" : null,
                                isWriteable ? e -> showRegisterUserForm() : null
                        );
                        tableContainer.add(emptyPanel, BorderLayout.CENTER);
                    } else {
                        tableContainer.add(scrollPane, BorderLayout.CENTER);
                        
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                        for (User u : displayedUsers) {
                            tableModel.addRow(new Object[]{
                                    u.getId(),
                                    u.getUsername(),
                                    u.getEmail(),
                                    u.getRole().toString(),
                                    u.getFullName(),
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

    private void handleDeleteAction() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a user to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int userId = (int) userTable.getValueAt(selectedRow, 0);
        String username = (String) userTable.getValueAt(selectedRow, 1);

        // Prevent admin from deleting themselves
        User current = com.intelliflow.context.UserSession.getInstance().getCurrentUser();
        if (current != null && current.getId() == userId) {
            JOptionPane.showMessageDialog(this, "You cannot delete your own active administrator account.", "Invalid Action", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete user: '" + username + "'?\nThis will nullify their project/task assignments.",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    mainFrame.getUserService().deleteUser(userId);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        refresh();
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(UserManagementView.this, "Failed to delete user.", "Error", JOptionPane.ERROR_MESSAGE);
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

        // Role Combobox
        gbc.gridy++;
        dialog.add(new JLabel("Security Role:"), gbc);
        gbc.gridy++;
        JComboBox<Role> roleCombo = new JComboBox<>(Role.values());
        dialog.add(roleCombo, gbc);

        // Password
        gbc.gridy++;
        dialog.add(new JLabel("Temp Password:"), gbc);
        gbc.gridy++;
        JTextField passField = new JTextField("TempPass123!"); // Autopopulate a default
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

            try {
                mainFrame.getUserService().register(user, password);
                dialog.dispose();
                refresh();
            } catch (ValidationException ex) {
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
}

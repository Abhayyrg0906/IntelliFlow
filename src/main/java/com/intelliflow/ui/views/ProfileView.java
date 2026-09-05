package com.intelliflow.ui.views;

import com.intelliflow.context.UserSession;
import com.intelliflow.exception.AuthenticationException;
import com.intelliflow.exception.ValidationException;
import com.intelliflow.model.User;
import com.intelliflow.ui.MainFrame;
import com.intelliflow.ui.ThemeManager;
import com.intelliflow.ui.components.RoundedPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;

public class ProfileView extends BaseView {
    private final MainFrame mainFrame;

    private JTextField usernameField;
    private JTextField roleField;
    private JTextField statusField;
    private JTextField memberSinceField;
    private JTextField fullNameField;
    private JTextField emailField;
    private JButton saveProfileBtn;

    private JPasswordField currentPassField;
    private JPasswordField newPassField;
    private JPasswordField confirmPassField;
    private JButton changePassBtn;

    public ProfileView(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new GridBagLayout());
        setBackground(ThemeManager.COLOR_BACKGROUND);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        initComponents();
    }

    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0.5;
        gbc.weighty = 1.0;

        // Left Panel - Profile Details Card
        JPanel detailsCard = createProfileDetailsCard();
        add(detailsCard, gbc);

        // Right Panel - Security / Password Card
        gbc.gridx = 1;
        JPanel securityCard = createSecurityCard();
        add(securityCard, gbc);
    }

    private JPanel createProfileDetailsCard() {
        RoundedPanel card = new RoundedPanel(12, ThemeManager.COLOR_CARD);
        card.setDrawBorder(true);
        card.setBorderColor(ThemeManager.COLOR_BORDER);
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(25, 25, 25, 25));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;

        JLabel title = new JLabel("👤 Account Profile Information");
        title.setFont(ThemeManager.FONT_SUBTITLE);
        title.setForeground(ThemeManager.COLOR_PRIMARY_HOVER);
        card.add(title, gbc);

        gbc.gridy++;
        card.add(new JSeparator(JSeparator.HORIZONTAL), gbc);

        // Username (Read-Only)
        gbc.gridy++;
        card.add(new JLabel("Username (Immutable):"), gbc);
        gbc.gridy++;
        usernameField = new JTextField();
        usernameField.setPreferredSize(new Dimension(280, 32));
        usernameField.setEditable(false);
        usernameField.setBackground(ThemeManager.COLOR_BACKGROUND);
        card.add(usernameField, gbc);

        // Security Role (Read-Only)
        gbc.gridy++;
        card.add(new JLabel("Assigned Security Role:"), gbc);
        gbc.gridy++;
        roleField = new JTextField();
        roleField.setPreferredSize(new Dimension(280, 32));
        roleField.setEditable(false);
        roleField.setBackground(ThemeManager.COLOR_BACKGROUND);
        card.add(roleField, gbc);

        // Account Status (Read-Only)
        gbc.gridy++;
        card.add(new JLabel("Account Status:"), gbc);
        gbc.gridy++;
        statusField = new JTextField();
        statusField.setPreferredSize(new Dimension(280, 32));
        statusField.setEditable(false);
        statusField.setBackground(ThemeManager.COLOR_BACKGROUND);
        card.add(statusField, gbc);

        // Member Since (Read-Only)
        gbc.gridy++;
        card.add(new JLabel("Member Since:"), gbc);
        gbc.gridy++;
        memberSinceField = new JTextField();
        memberSinceField.setPreferredSize(new Dimension(280, 32));
        memberSinceField.setEditable(false);
        memberSinceField.setBackground(ThemeManager.COLOR_BACKGROUND);
        card.add(memberSinceField, gbc);

        // Full Name (Editable)
        gbc.gridy++;
        card.add(new JLabel("Full Name:"), gbc);
        gbc.gridy++;
        fullNameField = new JTextField();
        fullNameField.setPreferredSize(new Dimension(280, 32));
        fullNameField.putClientProperty("JTextField.placeholderText", "Enter your full name");
        card.add(fullNameField, gbc);

        // Email (Editable)
        gbc.gridy++;
        card.add(new JLabel("Email Address:"), gbc);
        gbc.gridy++;
        emailField = new JTextField();
        emailField.setPreferredSize(new Dimension(280, 32));
        emailField.putClientProperty("JTextField.placeholderText", "Enter your email address");
        card.add(emailField, gbc);

        gbc.gridy++;
        card.add(Box.createVerticalStrut(10), gbc);

        // Save Button
        gbc.gridy++;
        saveProfileBtn = new JButton("💾 Save Profile Changes");
        saveProfileBtn.setBackground(ThemeManager.COLOR_PRIMARY);
        saveProfileBtn.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        saveProfileBtn.setFont(ThemeManager.FONT_BOLD_SMALL);
        saveProfileBtn.setPreferredSize(new Dimension(280, 36));
        saveProfileBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveProfileBtn.addActionListener(e -> handleSaveProfile());
        card.add(saveProfileBtn, gbc);

        return card;
    }

    private JPanel createSecurityCard() {
        RoundedPanel card = new RoundedPanel(12, ThemeManager.COLOR_CARD);
        card.setDrawBorder(true);
        card.setBorderColor(ThemeManager.COLOR_BORDER);
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(25, 25, 25, 25));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;

        JLabel title = new JLabel("🔒 Account Security & Password");
        title.setFont(ThemeManager.FONT_SUBTITLE);
        title.setForeground(ThemeManager.COLOR_PRIMARY_HOVER);
        card.add(title, gbc);

        gbc.gridy++;
        card.add(new JSeparator(JSeparator.HORIZONTAL), gbc);

        // Policy Hint
        gbc.gridy++;
        JLabel policyHint = new JLabel("<html><small style='color:#94A3B8;'>Password must have 8+ characters, including uppercase, lowercase, number, & special symbol.</small></html>");
        card.add(policyHint, gbc);

        // Current Password
        gbc.gridy++;
        card.add(new JLabel("Current Password:"), gbc);
        gbc.gridy++;
        currentPassField = new JPasswordField();
        currentPassField.setPreferredSize(new Dimension(280, 32));
        card.add(currentPassField, gbc);

        // New Password
        gbc.gridy++;
        card.add(new JLabel("New Password:"), gbc);
        gbc.gridy++;
        newPassField = new JPasswordField();
        newPassField.setPreferredSize(new Dimension(280, 32));
        card.add(newPassField, gbc);

        // Confirm Password
        gbc.gridy++;
        card.add(new JLabel("Confirm New Password:"), gbc);
        gbc.gridy++;
        confirmPassField = new JPasswordField();
        confirmPassField.setPreferredSize(new Dimension(280, 32));
        card.add(confirmPassField, gbc);

        gbc.gridy++;
        card.add(Box.createVerticalStrut(10), gbc);

        // Change Password Button
        gbc.gridy++;
        changePassBtn = new JButton("🔑 Change Password");
        changePassBtn.setBackground(ThemeManager.COLOR_SUCCESS);
        changePassBtn.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        changePassBtn.setFont(ThemeManager.FONT_BOLD_SMALL);
        changePassBtn.setPreferredSize(new Dimension(280, 36));
        changePassBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        changePassBtn.addActionListener(e -> handleChangePassword());
        card.add(changePassBtn, gbc);

        return card;
    }

    @Override
    public void refresh() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null) {
            usernameField.setText(currentUser.getUsername());
            roleField.setText(currentUser.getRole().name());
            statusField.setText(currentUser.isActive() ? "🟢 Active (Enabled)" : "🔴 Inactive (Deactivated)");
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            memberSinceField.setText(currentUser.getCreatedAt() != null ? currentUser.getCreatedAt().format(formatter) : "N/A");
            
            fullNameField.setText(currentUser.getFullName());
            emailField.setText(currentUser.getEmail());
        }

        currentPassField.setText("");
        newPassField.setText("");
        confirmPassField.setText("");
    }

    private void handleSaveProfile() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String email = emailField.getText().trim();
        String fullName = fullNameField.getText().trim();

        // Create updated payload preserving existing credentials & role
        User updatePayload = new User();
        updatePayload.setId(currentUser.getId());
        updatePayload.setUsername(currentUser.getUsername());
        updatePayload.setEmail(email);
        updatePayload.setFullName(fullName);
        updatePayload.setRole(currentUser.getRole());
        updatePayload.setActive(currentUser.isActive());
        updatePayload.setPasswordHash(currentUser.getPasswordHash());
        updatePayload.setCreatedAt(currentUser.getCreatedAt());

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                mainFrame.getUserService().updateUser(updatePayload);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    currentUser.setEmail(email);
                    currentUser.setFullName(fullName);
                    JOptionPane.showMessageDialog(ProfileView.this, "Profile updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    mainFrame.showView("profile");
                } catch (Exception ex) {
                    Throwable cause = ex.getCause();
                    JOptionPane.showMessageDialog(ProfileView.this,
                            cause != null ? cause.getMessage() : "Failed to update profile details.",
                            "Update Error",
                            JOptionPane.ERROR_MESSAGE);
                    refresh();
                }
            }
        };
        worker.execute();
    }

    private void handleChangePassword() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String currentPassword = new String(currentPassField.getPassword());
        String newPassword = new String(newPassField.getPassword());
        String confirmPassword = new String(confirmPassField.getPassword());

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                mainFrame.getUserService().changePassword(currentUser.getId(), currentPassword, newPassword, confirmPassword);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(ProfileView.this, "Password updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    currentPassField.setText("");
                    newPassField.setText("");
                    confirmPassField.setText("");
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String title = (cause instanceof AuthenticationException) ? "Authentication Error" : "Validation Error";
                    JOptionPane.showMessageDialog(ProfileView.this, cause.getMessage(), title, JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
}

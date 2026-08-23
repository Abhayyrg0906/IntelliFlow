package com.intelliflow.ui.views;

import com.intelliflow.context.UserSession;
import com.intelliflow.exception.ValidationException;
import com.intelliflow.model.User;
import com.intelliflow.ui.MainFrame;
import com.intelliflow.ui.ThemeManager;
import com.intelliflow.ui.components.RoundedPanel;
import com.intelliflow.util.PasswordUtil;
import com.intelliflow.util.ValidationUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ProfileView extends BaseView {
    private final MainFrame mainFrame;

    private JTextField usernameField;
    private JTextField roleField;
    private JTextField emailField;
    private JTextField fullNameField;
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
        card.setBorder(new EmptyBorder(30, 30, 30, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;

        JLabel title = new JLabel("Profile Settings");
        title.setFont(ThemeManager.FONT_SUBTITLE);
        title.setForeground(ThemeManager.COLOR_PRIMARY_HOVER);
        card.add(title, gbc);

        gbc.gridy++;
        card.add(new JSeparator(JSeparator.HORIZONTAL), gbc);

        // Username
        gbc.gridy++;
        card.add(new JLabel("Username (Read-Only):"), gbc);
        gbc.gridy++;
        usernameField = new JTextField();
        usernameField.setPreferredSize(new Dimension(280, 32));
        usernameField.setEditable(false);
        usernameField.setEnabled(false);
        card.add(usernameField, gbc);

        // Role
        gbc.gridy++;
        card.add(new JLabel("Security Role Permission:"), gbc);
        gbc.gridy++;
        roleField = new JTextField();
        roleField.setPreferredSize(new Dimension(280, 32));
        roleField.setEditable(false);
        roleField.setEnabled(false);
        card.add(roleField, gbc);

        // Full Name
        gbc.gridy++;
        card.add(new JLabel("Full Name:"), gbc);
        gbc.gridy++;
        fullNameField = new JTextField();
        fullNameField.setPreferredSize(new Dimension(280, 32));
        card.add(fullNameField, gbc);

        // Email
        gbc.gridy++;
        card.add(new JLabel("Email Address:"), gbc);
        gbc.gridy++;
        emailField = new JTextField();
        emailField.setPreferredSize(new Dimension(280, 32));
        card.add(emailField, gbc);

        gbc.gridy++;
        card.add(Box.createVerticalStrut(15), gbc);

        // Save Button
        gbc.gridy++;
        saveProfileBtn = new JButton("Save Changes");
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
        card.setBorder(new EmptyBorder(30, 30, 30, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;

        JLabel title = new JLabel("Account Security");
        title.setFont(ThemeManager.FONT_SUBTITLE);
        title.setForeground(ThemeManager.COLOR_PRIMARY_HOVER);
        card.add(title, gbc);

        gbc.gridy++;
        card.add(new JSeparator(JSeparator.HORIZONTAL), gbc);

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
        card.add(Box.createVerticalStrut(15), gbc);

        // Change Password Button
        gbc.gridy++;
        changePassBtn = new JButton("Update Password");
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

        currentUser.setEmail(email);
        currentUser.setFullName(fullName);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                mainFrame.getUserService().updateUser(currentUser);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(ProfileView.this, "Profile updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    mainFrame.showView("profile"); // Refresh
                } catch (Exception ex) {
                    Throwable cause = ex.getCause();
                    JOptionPane.showMessageDialog(ProfileView.this,
                            cause != null ? cause.getMessage() : "Failed to update profile details.",
                            "Validation Error",
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

        if (!PasswordUtil.verify(currentPassword, currentUser.getPasswordHash())) {
            JOptionPane.showMessageDialog(this, "Incorrect current password.", "Verification Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "New passwords do not match.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!ValidationUtil.isValidPassword(newPassword)) {
            JOptionPane.showMessageDialog(this,
                    "Password must contain at least 8 characters, including 1 uppercase, 1 lowercase, 1 number, and 1 special symbol.",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        currentUser.setPasswordHash(PasswordUtil.hash(newPassword));

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                mainFrame.getUserService().updateUser(currentUser);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(ProfileView.this, "Password updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    refresh();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ProfileView.this, "Failed to update password. Database error.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
}

package com.intelliflow.ui.views;

import com.intelliflow.enums.Role;
import com.intelliflow.exception.ValidationException;
import com.intelliflow.model.User;
import com.intelliflow.ui.MainFrame;
import com.intelliflow.ui.ThemeManager;
import com.intelliflow.ui.components.RoundedPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class RegisterView extends BaseView {
    private final MainFrame mainFrame;
    
    private JTextField usernameField;
    private JTextField emailField;
    private JTextField fullNameField;
    private JPasswordField passwordField;
    private JComboBox<Role> roleComboBox;
    private JButton registerButton;
    private JButton loginLink;
    private JLabel errorLabel;

    public RegisterView(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new GridBagLayout());
        setBackground(ThemeManager.COLOR_BACKGROUND);
        
        initComponents();
    }

    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 10, 10);

        // Center card panel
        RoundedPanel card = new RoundedPanel(16, ThemeManager.COLOR_CARD);
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(30, 40, 30, 40));
        card.setPreferredSize(new Dimension(420, 580));
        card.setDrawBorder(true);
        card.setBorderColor(ThemeManager.COLOR_BORDER);

        GridBagConstraints cardGbc = new GridBagConstraints();
        cardGbc.gridx = 0;
        cardGbc.gridy = 0;
        cardGbc.fill = GridBagConstraints.HORIZONTAL;
        cardGbc.insets = new Insets(6, 0, 6, 0);
        cardGbc.weightx = 1.0;

        // Title
        JLabel titleLabel = new JLabel("Create Account", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setForeground(ThemeManager.COLOR_PRIMARY_HOVER);
        card.add(titleLabel, cardGbc);

        // Subtitle
        cardGbc.gridy++;
        JLabel subtitleLabel = new JLabel("Join IntelliFlow Platform", SwingConstants.CENTER);
        subtitleLabel.setFont(ThemeManager.FONT_SMALL);
        subtitleLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);
        card.add(subtitleLabel, cardGbc);

        cardGbc.gridy++;
        card.add(Box.createVerticalStrut(10), cardGbc);

        // Username
        cardGbc.gridy++;
        JLabel usernameLabel = new JLabel("Username");
        usernameLabel.setFont(ThemeManager.FONT_BOLD_SMALL);
        usernameLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        card.add(usernameLabel, cardGbc);

        cardGbc.gridy++;
        usernameField = new JTextField();
        usernameField.setPreferredSize(new Dimension(340, 32));
        usernameField.putClientProperty("JTextField.placeholderText", "Enter username");
        card.add(usernameField, cardGbc);

        // Email
        cardGbc.gridy++;
        JLabel emailLabel = new JLabel("Email Address");
        emailLabel.setFont(ThemeManager.FONT_BOLD_SMALL);
        emailLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        card.add(emailLabel, cardGbc);

        cardGbc.gridy++;
        emailField = new JTextField();
        emailField.setPreferredSize(new Dimension(340, 32));
        emailField.putClientProperty("JTextField.placeholderText", "Enter email address");
        card.add(emailField, cardGbc);

        // Full Name
        cardGbc.gridy++;
        JLabel nameLabel = new JLabel("Full Name");
        nameLabel.setFont(ThemeManager.FONT_BOLD_SMALL);
        nameLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        card.add(nameLabel, cardGbc);

        cardGbc.gridy++;
        fullNameField = new JTextField();
        fullNameField.setPreferredSize(new Dimension(340, 32));
        fullNameField.putClientProperty("JTextField.placeholderText", "Enter full name");
        card.add(fullNameField, cardGbc);

        // Role Selector
        cardGbc.gridy++;
        JLabel roleLabel = new JLabel("Register As");
        roleLabel.setFont(ThemeManager.FONT_BOLD_SMALL);
        roleLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        card.add(roleLabel, cardGbc);

        cardGbc.gridy++;
        roleComboBox = new JComboBox<>(Role.values());
        roleComboBox.setPreferredSize(new Dimension(340, 32));
        card.add(roleComboBox, cardGbc);

        // Password
        cardGbc.gridy++;
        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setFont(ThemeManager.FONT_BOLD_SMALL);
        passwordLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        card.add(passwordLabel, cardGbc);

        cardGbc.gridy++;
        passwordField = new JPasswordField();
        passwordField.setPreferredSize(new Dimension(340, 32));
        passwordField.putClientProperty("JTextField.placeholderText", "Enter password");
        card.add(passwordField, cardGbc);

        // Error message label
        cardGbc.gridy++;
        errorLabel = new JLabel(" ");
        errorLabel.setFont(ThemeManager.FONT_SMALL);
        errorLabel.setForeground(ThemeManager.COLOR_DANGER);
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(errorLabel, cardGbc);

        // Register Button
        cardGbc.gridy++;
        registerButton = new JButton("Register");
        registerButton.setFont(ThemeManager.FONT_SUBTITLE);
        registerButton.setBackground(ThemeManager.COLOR_PRIMARY);
        registerButton.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        registerButton.setPreferredSize(new Dimension(340, 38));
        registerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerButton.addActionListener(e -> handleRegister());
        card.add(registerButton, cardGbc);

        // Back to Login Link
        cardGbc.gridy++;
        JPanel loginPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        loginPanel.setOpaque(false);
        
        JLabel accountPrompt = new JLabel("Already registered?");
        accountPrompt.setFont(ThemeManager.FONT_SMALL);
        accountPrompt.setForeground(ThemeManager.COLOR_TEXT_MUTED);
        
        loginLink = new JButton("Sign In");
        loginLink.setFont(ThemeManager.FONT_BOLD_SMALL);
        loginLink.setForeground(ThemeManager.COLOR_PRIMARY_HOVER);
        loginLink.setContentAreaFilled(false);
        loginLink.setBorderPainted(false);
        loginLink.setFocusPainted(false);
        loginLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginLink.addActionListener(e -> mainFrame.showView("login"));
        
        loginPanel.add(accountPrompt);
        loginPanel.add(loginLink);
        card.add(loginPanel, cardGbc);

        add(card, gbc);
    }

    private void handleRegister() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String fullName = fullNameField.getText().trim();
        Role role = (Role) roleComboBox.getSelectedItem();
        String password = new String(passwordField.getPassword());

        errorLabel.setText(" "); // Reset

        // Create User entity
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(role);

        registerButton.setEnabled(false);
        registerButton.setText("Registering...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                mainFrame.getUserService().register(user, password);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(RegisterView.this,
                            "Registration successful! You can now log in.",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                    mainFrame.showView("login");
                } catch (Exception ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof ValidationException) {
                        errorLabel.setText(cause.getMessage());
                    } else {
                        errorLabel.setText("Failed to register: Database error.");
                    }
                    registerButton.setEnabled(true);
                    registerButton.setText("Register");
                }
            }
        };
        worker.execute();
    }

    @Override
    public void refresh() {
        usernameField.setText("");
        emailField.setText("");
        fullNameField.setText("");
        passwordField.setText("");
        roleComboBox.setSelectedIndex(0);
        errorLabel.setText(" ");
        registerButton.setEnabled(true);
        registerButton.setText("Register");
    }
}

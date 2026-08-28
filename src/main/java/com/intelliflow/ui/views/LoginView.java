package com.intelliflow.ui.views;

import com.intelliflow.context.UserSession;
import com.intelliflow.exception.AuthenticationException;
import com.intelliflow.model.User;
import com.intelliflow.ui.MainFrame;
import com.intelliflow.ui.ThemeManager;
import com.intelliflow.ui.components.RoundedPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginView extends BaseView {
    private final MainFrame mainFrame;
    
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerLink;
    private JLabel errorLabel;

    public LoginView(MainFrame mainFrame) {
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
        card.setLayout(new GridLayout(1, 2, 0, 0));
        card.setPreferredSize(new Dimension(800, 480));
        card.setDrawBorder(true);
        card.setBorderColor(ThemeManager.COLOR_BORDER);

        // Left Branding Panel
        JPanel leftPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Fill left side with primary theme color, rounding the left corners
                g2.setColor(ThemeManager.COLOR_PRIMARY);
                int r = 16;
                g2.fillRoundRect(0, 0, getWidth() + r, getHeight(), r, r);
                g2.fillRect(getWidth() - r, 0, r * 2, getHeight());
                g2.dispose();
            }
        };
        leftPanel.setLayout(new GridBagLayout());
        leftPanel.setOpaque(false);

        GridBagConstraints leftGbc = new GridBagConstraints();
        leftGbc.gridx = 0;
        leftGbc.gridy = 0;
        leftGbc.fill = GridBagConstraints.HORIZONTAL;
        leftGbc.insets = new Insets(10, 30, 10, 30);

        JLabel leftTitle = new JLabel("⚡ IntelliFlow", SwingConstants.CENTER);
        leftTitle.setFont(new Font("SansSerif", Font.BOLD, 36));
        leftTitle.setForeground(Color.WHITE);
        leftPanel.add(leftTitle, leftGbc);

        leftGbc.gridy++;
        JLabel leftSubtitle = new JLabel("Smart Workflow Automation", SwingConstants.CENTER);
        leftSubtitle.setFont(ThemeManager.FONT_SUBTITLE);
        leftSubtitle.setForeground(new Color(224, 231, 255));
        leftPanel.add(leftSubtitle, leftGbc);

        leftGbc.gridy++;
        leftPanel.add(Box.createVerticalStrut(20), leftGbc);

        leftGbc.gridy++;
        JLabel leftDesc = new JLabel("<html><center>An enterprise-ready platform<br>for tracking projects, workflow lifecycles,<br>and team performance metrics.</center></html>", SwingConstants.CENTER);
        leftDesc.setFont(ThemeManager.FONT_BODY);
        leftDesc.setForeground(new Color(199, 210, 254));
        leftPanel.add(leftDesc, leftGbc);

        card.add(leftPanel);

        // Right Form Panel
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setOpaque(false);
        rightPanel.setBorder(new EmptyBorder(30, 40, 30, 40));

        GridBagConstraints cardGbc = new GridBagConstraints();
        cardGbc.gridx = 0;
        cardGbc.gridy = 0;
        cardGbc.fill = GridBagConstraints.HORIZONTAL;
        cardGbc.insets = new Insets(8, 0, 8, 0);
        cardGbc.weightx = 1.0;

        // Title inside form
        JLabel titleLabel = new JLabel("Welcome Back", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        rightPanel.add(titleLabel, cardGbc);

        cardGbc.gridy++;
        JLabel subtitleLabel = new JLabel("Sign in to your account to continue", SwingConstants.CENTER);
        subtitleLabel.setFont(ThemeManager.FONT_SMALL);
        subtitleLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);
        rightPanel.add(subtitleLabel, cardGbc);

        cardGbc.gridy++;
        rightPanel.add(Box.createVerticalStrut(15), cardGbc);

        // Username
        cardGbc.gridy++;
        JLabel usernameLabel = new JLabel("Username");
        usernameLabel.setFont(ThemeManager.FONT_BOLD_SMALL);
        usernameLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        rightPanel.add(usernameLabel, cardGbc);

        cardGbc.gridy++;
        usernameField = new JTextField();
        usernameField.setPreferredSize(new Dimension(300, 36));
        usernameField.putClientProperty("JTextField.placeholderText", "Enter your username");
        rightPanel.add(usernameField, cardGbc);

        // Password
        cardGbc.gridy++;
        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setFont(ThemeManager.FONT_BOLD_SMALL);
        passwordLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        rightPanel.add(passwordLabel, cardGbc);

        cardGbc.gridy++;
        passwordField = new JPasswordField();
        passwordField.setPreferredSize(new Dimension(300, 36));
        passwordField.putClientProperty("JTextField.placeholderText", "Enter your password");
        rightPanel.add(passwordField, cardGbc);

        // Error message placeholder
        cardGbc.gridy++;
        errorLabel = new JLabel(" ");
        errorLabel.setFont(ThemeManager.FONT_SMALL);
        errorLabel.setForeground(ThemeManager.COLOR_DANGER);
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        rightPanel.add(errorLabel, cardGbc);

        // Login Button
        cardGbc.gridy++;
        loginButton = new JButton("Sign In");
        loginButton.setFont(ThemeManager.FONT_SUBTITLE);
        loginButton.setBackground(ThemeManager.COLOR_PRIMARY);
        loginButton.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        loginButton.setPreferredSize(new Dimension(300, 40));
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginButton.addActionListener(e -> handleLogin());
        rightPanel.add(loginButton, cardGbc);

        // Register Link
        cardGbc.gridy++;
        JPanel registerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        registerPanel.setOpaque(false);
        
        JLabel accountPrompt = new JLabel("New to platform?");
        accountPrompt.setFont(ThemeManager.FONT_SMALL);
        accountPrompt.setForeground(ThemeManager.COLOR_TEXT_MUTED);
        
        registerLink = new JButton("Create an account");
        registerLink.setFont(ThemeManager.FONT_BOLD_SMALL);
        registerLink.setForeground(ThemeManager.COLOR_PRIMARY_HOVER);
        registerLink.setContentAreaFilled(false);
        registerLink.setBorderPainted(false);
        registerLink.setFocusPainted(false);
        registerLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerLink.addActionListener(e -> mainFrame.showView("register"));
        
        registerPanel.add(accountPrompt);
        registerPanel.add(registerLink);
        rightPanel.add(registerPanel, cardGbc);

        card.add(rightPanel);

        add(card, gbc);
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() && password.isEmpty()) {
            errorLabel.setText("Please enter both username and password.");
            return;
        } else if (username.isEmpty()) {
            errorLabel.setText("Please enter your username.");
            return;
        } else if (password.isEmpty()) {
            errorLabel.setText("Please enter your password.");
            return;
        }

        errorLabel.setText(" "); // Reset

        // Run authentication in a SwingWorker background thread to prevent GUI freezing
        loginButton.setEnabled(false);
        loginButton.setText("Signing In...");

        SwingWorker<User, Void> worker = new SwingWorker<>() {
            @Override
            protected User doInBackground() throws Exception {
                // Returns user if authenticated successfully, throws exception otherwise
                return mainFrame.getUserService().authenticate(username, password);
            }

            @Override
            protected void done() {
                try {
                    User user = get();
                    // Store active user session
                    UserSession.getInstance().startSession(user);
                    // Navigate to Dashboard
                    mainFrame.showView("dashboard");
                } catch (Exception ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof AuthenticationException) {
                        errorLabel.setText(cause.getMessage());
                    } else {
                        errorLabel.setText("Database connection error. Try again.");
                    }
                    loginButton.setEnabled(true);
                    loginButton.setText("Sign In");
                }
            }
        };
        worker.execute();
    }

    @Override
    public void refresh() {
        usernameField.setText("");
        passwordField.setText("");
        errorLabel.setText(" ");
        loginButton.setEnabled(true);
        loginButton.setText("Sign In");
    }
}

package com.intelliflow.ui;

import com.intelliflow.context.UserSession;
import com.intelliflow.enums.Role;
import com.intelliflow.model.User;
import com.intelliflow.service.impl.*;
import com.intelliflow.service.interfaces.*;
import com.intelliflow.ui.components.SidebarButton;
import com.intelliflow.ui.views.*;
import com.intelliflow.util.DBUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

public class MainFrame extends JFrame {

    // Services
    private final UserService userService;
    private final ProjectService projectService;
    private final TaskService taskService;
    private final NotificationService notificationService;
    private final ReportService reportService;
    private final CommentService commentService;
    private final AttachmentService attachmentService;

    // UI Layout Components
    private CardLayout cardLayout;
    private JPanel contentCards;
    private JPanel sidebarPanel;
    private JPanel headerPanel;
    private JLabel headerTitleLabel;
    private JLabel notificationBadge;
    private JLabel userProfileBadge;

    // View Mapping
    private final Map<String, BaseView> viewInstances = new HashMap<>();
    private final Map<String, SidebarButton> sidebarButtons = new HashMap<>();

    public MainFrame() {
        super("IntelliFlow – Smart Workflow Automation Platform");
        
        // 1. Initialize modern look-and-feel theme
        ThemeManager.initializeTheme();

        // 2. Initialize Service Layer
        this.userService = new UserServiceImpl();
        this.projectService = new ProjectServiceImpl();
        this.taskService = new TaskServiceImpl();
        this.notificationService = new NotificationServiceImpl();
        this.reportService = new ReportServiceImpl();
        this.commentService = new CommentServiceImpl();
        this.attachmentService = new AttachmentServiceImpl();

        // Bootstrap default users on database if they don't exist
        try {
            this.userService.bootstrapDefaultUsers();
        } catch (Exception e) {
            System.err.println("Warning: Default users bootstrap failed. MySQL server might be offline: " + e.getMessage());
        }

        // 3. Setup window configuration
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setLocationRelativeTo(null); // Center on screen
        getContentPane().setBackground(ThemeManager.COLOR_BACKGROUND);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Graceful cleanup of database connections
                DBUtil.shutdown();
            }
        });

        initComponents();
        showView("login");
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // CardLayout Panel
        cardLayout = new CardLayout();
        contentCards = new JPanel(cardLayout);
        contentCards.setBackground(ThemeManager.COLOR_BACKGROUND);

        // Instantiate cards (stubs initially, populated later)
        viewInstances.put("login", new LoginView(this));
        viewInstances.put("register", new RegisterView(this));
        viewInstances.put("dashboard", new DashboardView(this));
        viewInstances.put("projects", new ProjectManagementView(this));
        viewInstances.put("tasks", new TaskManagementView(this));
        viewInstances.put("deadlines", new DeadlinesView(this));
        viewInstances.put("notifications", new NotificationsView(this));
        viewInstances.put("reports", new ReportsView(this));
        viewInstances.put("users", new UserManagementView(this));
        viewInstances.put("logs", new ActivityLogsView(this));
        viewInstances.put("profile", new ProfileView(this));

        // Add cards to CardLayout container
        for (Map.Entry<String, BaseView> entry : viewInstances.entrySet()) {
            contentCards.add(entry.getValue(), entry.getKey());
        }

        // Top Header Panel
        headerPanel = createHeaderPanel();

        // Sidebar Navigation Panel (Initially hidden, shown only after login)
        sidebarPanel = createSidebarPanel();

        // Add main elements to Frame layout
        add(contentCards, BorderLayout.CENTER);
    }

    private JPanel createSidebarPanel() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(ThemeManager.COLOR_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(240, getHeight()));
        sidebar.setBorder(new MatteBorder(0, 0, 0, 1, ThemeManager.COLOR_BORDER));

        // Logo Header
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 20));
        logoPanel.setOpaque(false);
        JLabel logoLabel = new JLabel("⚡ IntelliFlow");
        logoLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        logoLabel.setForeground(ThemeManager.COLOR_PRIMARY_HOVER);
        logoPanel.add(logoLabel);
        sidebar.add(logoPanel);
        sidebar.add(Box.createVerticalStrut(15));

        // Navigation Buttons mapping
        createNavigationItem(sidebar, "📊 Dashboard", "dashboard");
        createNavigationItem(sidebar, "📂 Projects", "projects");
        createNavigationItem(sidebar, "📝 Tasks", "tasks");
        createNavigationItem(sidebar, "📅 Deadlines", "deadlines");
        createNavigationItem(sidebar, "🔔 Notifications", "notifications");
        createNavigationItem(sidebar, "📈 Reports", "reports");
        createNavigationItem(sidebar, "👥 Users", "users");
        createNavigationItem(sidebar, "⚙️ System Logs", "logs");
        createNavigationItem(sidebar, "👤 My Profile", "profile");

        sidebar.add(Box.createVerticalGlue());

        // Logout Button
        SidebarButton logoutBtn = new SidebarButton("🚪 Logout");
        logoutBtn.addActionListener(e -> handleLogout());
        sidebar.add(logoutBtn);
        sidebar.add(Box.createVerticalStrut(20));

        return sidebar;
    }

    private void createNavigationItem(JPanel sidebar, String label, String viewName) {
        SidebarButton btn = new SidebarButton(label);
        btn.addActionListener(e -> showView(viewName));
        sidebarButtons.put(viewName, btn);
        sidebar.add(btn);
        sidebar.add(Box.createVerticalStrut(5));
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ThemeManager.COLOR_SIDEBAR);
        header.setPreferredSize(new Dimension(getWidth(), 60));
        header.setBorder(new MatteBorder(0, 0, 1, 0, ThemeManager.COLOR_BORDER));
        header.setBorder(BorderFactory.createCompoundBorder(
                header.getBorder(),
                new EmptyBorder(10, 20, 10, 20)
        ));

        // Title on Left
        headerTitleLabel = new JLabel("Dashboard");
        headerTitleLabel.setFont(ThemeManager.FONT_SUBTITLE);
        headerTitleLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        header.add(headerTitleLabel, BorderLayout.WEST);

        // Badge container on Right
        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightControls.setOpaque(false);

        // Notification Bell Icon + Badge
        JPanel bellPanel = new JPanel(new BorderLayout());
        bellPanel.setOpaque(false);
        bellPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JLabel bellIcon = new JLabel("🔔");
        bellIcon.setFont(new Font("SansSerif", Font.PLAIN, 18));
        bellIcon.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        bellPanel.add(bellIcon, BorderLayout.CENTER);

        notificationBadge = new JLabel("0");
        notificationBadge.setFont(ThemeManager.FONT_BOLD_SMALL);
        notificationBadge.setForeground(ThemeManager.COLOR_DANGER);
        notificationBadge.setBorder(new EmptyBorder(0, 5, 0, 0));
        bellPanel.add(notificationBadge, BorderLayout.EAST);
        
        bellPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                showView("notifications");
            }
        });
        rightControls.add(bellPanel);

        // Profile Capsule Badge
        userProfileBadge = new JLabel("👤 Guest");
        userProfileBadge.setFont(ThemeManager.FONT_BOLD_SMALL);
        userProfileBadge.setForeground(ThemeManager.COLOR_PRIMARY_HOVER);
        rightControls.add(userProfileBadge);

        header.add(rightControls, BorderLayout.EAST);

        return header;
    }

    /**
     * Shows a specific screen panel by its registered CardLayout name.
     * Triggers dynamic data refresh on load.
     *
     * @param viewName Registered key name of the view card
     */
    public void showView(String viewName) {
        if ("login".equals(viewName) || "register".equals(viewName)) {
            // Remove full navigation framing layout
            remove(sidebarPanel);
            remove(headerPanel);
            revalidate();
            repaint();
        } else {
            // Check session login state
            if (!UserSession.getInstance().isLoggedIn()) {
                showView("login");
                return;
            }

            User currentUser = UserSession.getInstance().getCurrentUser();
            Role role = currentUser.getRole();

            // Enforce role-based routing checks
            if ("users".equals(viewName) && role != Role.ADMIN) {
                JOptionPane.showMessageDialog(this, "Access Denied: Only System Administrators can access this screen.", "Unauthorized Access", JOptionPane.ERROR_MESSAGE);
                showView("dashboard");
                return;
            }
            if ("reports".equals(viewName) && role == Role.EMPLOYEE) {
                JOptionPane.showMessageDialog(this, "Access Denied: Standard Employees are not authorized to access performance reports.", "Unauthorized Access", JOptionPane.ERROR_MESSAGE);
                showView("dashboard");
                return;
            }

            // Ensure full navigation layouts are set
            add(sidebarPanel, BorderLayout.WEST);
            add(headerPanel, BorderLayout.NORTH);
            revalidate();
            repaint();

            // Refresh Dynamic Sidebar components based on user role permissions
            updateSidebarPermissions();

            // Update Header user badge
            currentUser = UserSession.getInstance().getCurrentUser();
            userProfileBadge.setText("👤 " + currentUser.getFullName() + " (" + currentUser.getRole() + ")");

            // Sync Notification badge counts
            updateNotificationCount();

            // Update Header title string based on active views
            updateHeaderTitle(viewName);

            // Set active selection styling in navigation buttons list
            for (Map.Entry<String, SidebarButton> entry : sidebarButtons.entrySet()) {
                entry.getValue().setSelected(entry.getKey().equals(viewName));
            }
        }

        // Hot swap card view
        cardLayout.show(contentCards, viewName);

        // Invoke custom refresh handler
        BaseView activeView = viewInstances.get(viewName);
        if (activeView != null) {
            try {
                activeView.refresh();
            } catch (Exception e) {
                System.err.println("Failed to refresh view " + viewName + ": " + e.getMessage());
            }
        }
    }

    private void updateHeaderTitle(String viewName) {
        switch (viewName) {
            case "dashboard" -> headerTitleLabel.setText("📊 Dashboard Overview");
            case "projects" -> headerTitleLabel.setText("📂 Projects Board");
            case "tasks" -> headerTitleLabel.setText("📝 Task Board");
            case "deadlines" -> headerTitleLabel.setText("📅 Upcoming Deadlines & Schedule");
            case "notifications" -> headerTitleLabel.setText("🔔 Notifications Drawer");
            case "reports" -> headerTitleLabel.setText("📈 Performance & Reports");
            case "users" -> headerTitleLabel.setText("👥 User Management Directory");
            case "logs" -> headerTitleLabel.setText("📜 Activity Timeline & Audits");
            case "profile" -> headerTitleLabel.setText("👤 User Settings & Profile");
        }
    }

    private void updateSidebarPermissions() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) return;

        Role role = currentUser.getRole();
        // Hide Admin-only User Directory from Managers/Employees
        boolean isAdmin = (role == Role.ADMIN);
        sidebarButtons.get("users").setVisible(isAdmin);

        // Hide Reports panel from standard Employees
        boolean canSeeReports = (role == Role.ADMIN || role == Role.MANAGER);
        sidebarButtons.get("reports").setVisible(canSeeReports);
    }

    public void updateNotificationCount() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null) {
            try {
                int count = notificationService.getUnreadNotificationsForUser(currentUser.getId()).size();
                notificationBadge.setText(String.valueOf(count));
                notificationBadge.setVisible(count > 0);
            } catch (Exception e) {
                notificationBadge.setVisible(false);
            }
        } else {
            notificationBadge.setVisible(false);
        }
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to log out of IntelliFlow?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            UserSession.getInstance().cleanSession();
            showView("login");
        }
    }

    // Getters for services to expose to subpanels
    public UserService getUserService() { return userService; }
    public ProjectService getProjectService() { return projectService; }
    public TaskService getTaskService() { return taskService; }
    public NotificationService getNotificationService() { return notificationService; }
    public ReportService getReportService() { return reportService; }
    public CommentService getCommentService() { return commentService; }
    public AttachmentService getAttachmentService() { return attachmentService; }

    // Getter for view instances
    public BaseView getView(String viewName) {
        return viewInstances.get(viewName);
    }
}

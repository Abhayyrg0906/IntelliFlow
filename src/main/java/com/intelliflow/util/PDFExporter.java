package com.intelliflow.util;

import com.intelliflow.enums.ProjectHealth;
import com.intelliflow.enums.TaskPriority;
import com.intelliflow.enums.TaskStatus;
import com.intelliflow.model.*;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class PDFExporter {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Colors
    private static final Color COLOR_PRIMARY = new Color(79, 70, 229);
    private static final Color COLOR_HEADER_BG = new Color(30, 41, 59);
    private static final Color COLOR_ROW_ALT = new Color(248, 250, 252);
    private static final Color COLOR_BORDER = new Color(226, 232, 240);
    private static final Color COLOR_MUTED = new Color(100, 116, 139);

    // Fonts
    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, COLOR_PRIMARY);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, COLOR_HEADER_BG);
    private static final Font FONT_SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COLOR_PRIMARY);
    private static final Font FONT_BODY = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
    private static final Font FONT_BODY_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.DARK_GRAY);
    private static final Font FONT_HEADER_CELL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font FONT_META = FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_MUTED);

    private PDFExporter() {}

    /**
     * 1. Project Summary & Tasks PDF Report
     */
    public static void exportProjectReport(ProjectProgressReport report, List<Task> tasks, List<User> allUsers, File file) throws IOException {
        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(file));
            doc.open();

            addDocumentHeader(doc, "Project Progress & Status Report", "Comprehensive project audit for: " + report.getProjectName());

            // KPI Summary Grid
            PdfPTable kpiTable = new PdfPTable(4);
            kpiTable.setWidthPercentage(100);
            kpiTable.setSpacingBefore(10);
            kpiTable.setSpacingAfter(15);

            addKpiCell(kpiTable, "Project Health", report.getHealth().name(), getHealthColor(report.getHealth()));
            addKpiCell(kpiTable, "Completion Rate", report.getCompletionPercentage() + "%", COLOR_PRIMARY);
            addKpiCell(kpiTable, "Total Tasks", String.valueOf(report.getTotalTasks()), COLOR_HEADER_BG);
            addKpiCell(kpiTable, "Completed / Overdue", report.getCompletedTasks() + " / " + report.getOverdueTasks(), new Color(244, 63, 94));
            doc.add(kpiTable);

            // Tasks Table
            Paragraph tableHeading = new Paragraph("Assigned Project Tasks (" + tasks.size() + " total)", FONT_SECTION);
            tableHeading.setSpacingAfter(6);
            doc.add(tableHeading);

            PdfPTable taskTable = new PdfPTable(new float[]{1.0f, 3.0f, 2.2f, 1.5f, 1.8f, 1.8f});
            taskTable.setWidthPercentage(100);

            addTableHeader(taskTable, new String[]{"ID", "Task Name", "Assigned To", "Priority", "Deadline", "Status"});

            int index = 0;
            for (Task t : tasks) {
                String empName = resolveUserName(t.getAssignedEmployeeId(), allUsers);
                String deadline = t.getDeadline() != null ? t.getDeadline().format(DATE_FMT) : "No Deadline";
                Color rowBg = (index % 2 == 1) ? COLOR_ROW_ALT : Color.WHITE;

                addTableCell(taskTable, String.valueOf(t.getId()), FONT_BODY, Element.ALIGN_CENTER, rowBg);
                addTableCell(taskTable, t.getName(), FONT_BODY_BOLD, Element.ALIGN_LEFT, rowBg);
                addTableCell(taskTable, empName, FONT_BODY, Element.ALIGN_LEFT, rowBg);
                addTableCell(taskTable, t.getPriority().name(), FONT_BODY, Element.ALIGN_CENTER, rowBg);
                addTableCell(taskTable, deadline, FONT_BODY, Element.ALIGN_CENTER, rowBg);
                addTableCell(taskTable, t.getStatus().name(), FONT_BODY, Element.ALIGN_CENTER, rowBg);
                index++;
            }

            doc.add(taskTable);
            addFooter(doc);
        } catch (DocumentException de) {
            throw new IOException("Failed to generate Project PDF report: " + de.getMessage(), de);
        } finally {
            if (doc.isOpen()) doc.close();
        }
    }

    /**
     * 2. Comprehensive Task Directory PDF Report
     */
    public static void exportTaskReport(List<Task> tasks, List<Project> projects, List<User> allUsers, File file) throws IOException {
        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(file));
            doc.open();

            addDocumentHeader(doc, "Task Directory Audit Report", "Full system task registry and execution status");

            PdfPTable table = new PdfPTable(new float[]{0.8f, 3.0f, 2.5f, 2.2f, 1.5f, 1.8f, 1.8f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);

            addTableHeader(table, new String[]{"ID", "Task Name", "Project", "Assigned To", "Priority", "Deadline", "Status"});

            int index = 0;
            for (Task t : tasks) {
                String projName = resolveProjectName(t.getProjectId(), projects);
                String empName = resolveUserName(t.getAssignedEmployeeId(), allUsers);
                String deadline = t.getDeadline() != null ? t.getDeadline().format(DATE_FMT) : "No Deadline";
                Color rowBg = (index % 2 == 1) ? COLOR_ROW_ALT : Color.WHITE;

                addTableCell(table, String.valueOf(t.getId()), FONT_BODY, Element.ALIGN_CENTER, rowBg);
                addTableCell(table, t.getName(), FONT_BODY_BOLD, Element.ALIGN_LEFT, rowBg);
                addTableCell(table, projName, FONT_BODY, Element.ALIGN_LEFT, rowBg);
                addTableCell(table, empName, FONT_BODY, Element.ALIGN_LEFT, rowBg);
                addTableCell(table, t.getPriority().name(), FONT_BODY, Element.ALIGN_CENTER, rowBg);
                addTableCell(table, deadline, FONT_BODY, Element.ALIGN_CENTER, rowBg);
                addTableCell(table, t.getStatus().name(), FONT_BODY, Element.ALIGN_CENTER, rowBg);
                index++;
            }

            doc.add(table);
            addFooter(doc);
        } catch (DocumentException de) {
            throw new IOException("Failed to generate Task Directory PDF report: " + de.getMessage(), de);
        } finally {
            if (doc.isOpen()) doc.close();
        }
    }

    /**
     * 3. User & Role Directory PDF Report (Never exposes password hashes or credentials)
     */
    public static void exportUserReport(List<User> users, File file) throws IOException {
        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(file));
            doc.open();

            addDocumentHeader(doc, "User Accounts & Role Directory", "Authorized system user profiles and access roles");

            PdfPTable table = new PdfPTable(new float[]{0.8f, 2.0f, 2.5f, 3.2f, 1.8f, 1.6f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);

            addTableHeader(table, new String[]{"ID", "Username", "Full Name", "Email Address", "Security Role", "Status"});

            int index = 0;
            for (User u : users) {
                String status = u.isActive() ? "ACTIVE" : "INACTIVE";
                Color rowBg = (index % 2 == 1) ? COLOR_ROW_ALT : Color.WHITE;

                addTableCell(table, String.valueOf(u.getId()), FONT_BODY, Element.ALIGN_CENTER, rowBg);
                addTableCell(table, u.getUsername(), FONT_BODY_BOLD, Element.ALIGN_LEFT, rowBg);
                addTableCell(table, u.getFullName(), FONT_BODY, Element.ALIGN_LEFT, rowBg);
                addTableCell(table, u.getEmail(), FONT_BODY, Element.ALIGN_LEFT, rowBg);
                addTableCell(table, u.getRole().name(), FONT_BODY, Element.ALIGN_CENTER, rowBg);
                addTableCell(table, status, FONT_BODY_BOLD, Element.ALIGN_CENTER, rowBg);
                index++;
            }

            doc.add(table);
            addFooter(doc);
        } catch (DocumentException de) {
            throw new IOException("Failed to generate User Directory PDF report: " + de.getMessage(), de);
        } finally {
            if (doc.isOpen()) doc.close();
        }
    }

    /**
     * 4. Priority Breakdown PDF Report
     */
    public static void exportPriorityReport(List<Task> tasks, List<Project> projects, List<User> allUsers, File file) throws IOException {
        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(file));
            doc.open();

            addDocumentHeader(doc, "Task Priority Audit & Distribution", "Categorized breakdown of workload by priority level");

            long crit = tasks.stream().filter(t -> t.getPriority() == TaskPriority.CRITICAL).count();
            long high = tasks.stream().filter(t -> t.getPriority() == TaskPriority.HIGH).count();
            long med = tasks.stream().filter(t -> t.getPriority() == TaskPriority.MEDIUM).count();
            long low = tasks.stream().filter(t -> t.getPriority() == TaskPriority.LOW).count();

            PdfPTable kpiTable = new PdfPTable(4);
            kpiTable.setWidthPercentage(100);
            kpiTable.setSpacingBefore(10);
            kpiTable.setSpacingAfter(15);

            addKpiCell(kpiTable, "CRITICAL Tasks", String.valueOf(crit), new Color(239, 68, 68));
            addKpiCell(kpiTable, "HIGH Tasks", String.valueOf(high), new Color(245, 158, 11));
            addKpiCell(kpiTable, "MEDIUM Tasks", String.valueOf(med), new Color(59, 130, 246));
            addKpiCell(kpiTable, "LOW Tasks", String.valueOf(low), new Color(100, 116, 139));
            doc.add(kpiTable);

            PdfPTable table = new PdfPTable(new float[]{1.0f, 1.6f, 3.2f, 2.5f, 2.0f, 1.8f});
            table.setWidthPercentage(100);

            addTableHeader(table, new String[]{"ID", "Priority", "Task Name", "Project", "Assigned To", "Status"});

            int index = 0;
            List<Task> sortedTasks = tasks.stream()
                    .sorted(Comparator.comparing(Task::getPriority).reversed())
                    .toList();

            for (Task t : sortedTasks) {
                String projName = resolveProjectName(t.getProjectId(), projects);
                String empName = resolveUserName(t.getAssignedEmployeeId(), allUsers);
                Color rowBg = (index % 2 == 1) ? COLOR_ROW_ALT : Color.WHITE;

                addTableCell(table, String.valueOf(t.getId()), FONT_BODY, Element.ALIGN_CENTER, rowBg);
                addTableCell(table, t.getPriority().name(), FONT_BODY_BOLD, Element.ALIGN_CENTER, rowBg);
                addTableCell(table, t.getName(), FONT_BODY, Element.ALIGN_LEFT, rowBg);
                addTableCell(table, projName, FONT_BODY, Element.ALIGN_LEFT, rowBg);
                addTableCell(table, empName, FONT_BODY, Element.ALIGN_LEFT, rowBg);
                addTableCell(table, t.getStatus().name(), FONT_BODY, Element.ALIGN_CENTER, rowBg);
                index++;
            }

            doc.add(table);
            addFooter(doc);
        } catch (DocumentException de) {
            throw new IOException("Failed to generate Priority PDF report: " + de.getMessage(), de);
        } finally {
            if (doc.isOpen()) doc.close();
        }
    }

    /**
     * 5. Deadline & Overdue Schedule PDF Report
     */
    public static void exportDeadlineReport(List<Task> tasks, List<Project> projects, List<User> allUsers, File file) throws IOException {
        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(file));
            doc.open();

            addDocumentHeader(doc, "Deadlines & Schedule Management Report", "Chronological task milestones and overdue tracking");

            PdfPTable table = new PdfPTable(new float[]{0.8f, 3.2f, 2.5f, 2.2f, 1.5f, 1.8f, 2.2f, 1.8f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);

            addTableHeader(table, new String[]{"ID", "Task Name", "Project", "Assigned To", "Priority", "Deadline", "Schedule Alert", "Status"});

            LocalDate today = LocalDate.now();
            int index = 0;

            List<Task> sortedTasks = tasks.stream()
                    .sorted(Comparator.comparing(Task::getDeadline, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();

            for (Task t : sortedTasks) {
                String projName = resolveProjectName(t.getProjectId(), projects);
                String empName = resolveUserName(t.getAssignedEmployeeId(), allUsers);
                String deadlineStr = t.getDeadline() != null ? t.getDeadline().format(DATE_FMT) : "No Deadline";
                String alert;

                if (t.getStatus() == TaskStatus.COMPLETED) {
                    alert = "Completed";
                } else if (t.getDeadline() == null) {
                    alert = "No Deadline";
                } else {
                    long diff = ChronoUnit.DAYS.between(today, t.getDeadline());
                    if (diff < 0) {
                        alert = "OVERDUE (" + Math.abs(diff) + "d ago)";
                    } else if (diff == 0) {
                        alert = "DUE TODAY";
                    } else {
                        alert = diff + " days left";
                    }
                }

                Color rowBg = (index % 2 == 1) ? COLOR_ROW_ALT : Color.WHITE;

                addTableCell(table, String.valueOf(t.getId()), FONT_BODY, Element.ALIGN_CENTER, rowBg);
                addTableCell(table, t.getName(), FONT_BODY_BOLD, Element.ALIGN_LEFT, rowBg);
                addTableCell(table, projName, FONT_BODY, Element.ALIGN_LEFT, rowBg);
                addTableCell(table, empName, FONT_BODY, Element.ALIGN_LEFT, rowBg);
                addTableCell(table, t.getPriority().name(), FONT_BODY, Element.ALIGN_CENTER, rowBg);
                addTableCell(table, deadlineStr, FONT_BODY, Element.ALIGN_CENTER, rowBg);
                addTableCell(table, alert, FONT_BODY_BOLD, Element.ALIGN_CENTER, rowBg);
                addTableCell(table, t.getStatus().name(), FONT_BODY, Element.ALIGN_CENTER, rowBg);
                index++;
            }

            doc.add(table);
            addFooter(doc);
        } catch (DocumentException de) {
            throw new IOException("Failed to generate Deadline PDF report: " + de.getMessage(), de);
        } finally {
            if (doc.isOpen()) doc.close();
        }
    }

    /**
     * 6. Activity & Audit Trail PDF Report
     */
    public static void exportActivityReport(List<ActivityLog> logs, List<User> allUsers, File file) throws IOException {
        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(file));
            doc.open();

            addDocumentHeader(doc, "System Activity & Audit Log Report", "Chronological audit timeline of user and project events");

            PdfPTable table = new PdfPTable(new float[]{1.0f, 2.2f, 2.5f, 2.5f, 5.0f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);

            addTableHeader(table, new String[]{"ID", "Timestamp", "Actor / User", "Action", "Description"});

            int index = 0;
            for (ActivityLog l : logs) {
                String actor = "System Action";
                if (l.getUserId() != null) {
                    actor = resolveUserName(l.getUserId(), allUsers);
                }
                String time = l.getTimestamp() != null ? l.getTimestamp().format(TIME_FMT) : "";
                Color rowBg = (index % 2 == 1) ? COLOR_ROW_ALT : Color.WHITE;

                addTableCell(table, String.valueOf(l.getId()), FONT_BODY, Element.ALIGN_CENTER, rowBg);
                addTableCell(table, time, FONT_BODY, Element.ALIGN_CENTER, rowBg);
                addTableCell(table, actor, FONT_BODY_BOLD, Element.ALIGN_LEFT, rowBg);
                addTableCell(table, l.getAction(), FONT_BODY, Element.ALIGN_CENTER, rowBg);
                addTableCell(table, l.getDescription() != null ? l.getDescription() : "", FONT_BODY, Element.ALIGN_LEFT, rowBg);
                index++;
            }

            doc.add(table);
            addFooter(doc);
        } catch (DocumentException de) {
            throw new IOException("Failed to generate Activity PDF report: " + de.getMessage(), de);
        } finally {
            if (doc.isOpen()) doc.close();
        }
    }

    /**
     * 7. Executive Analytics Summary PDF Report
     */
    public static void exportAnalyticsReport(AnalyticsSummary summary, List<Project> projects, File file) throws IOException {
        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(file));
            doc.open();

            addDocumentHeader(doc, "Executive Analytics & Intelligence Brief", "Consolidated performance metrics, team workload, and project health");

            // KPI Grid
            PdfPTable kpiTable = new PdfPTable(4);
            kpiTable.setWidthPercentage(100);
            kpiTable.setSpacingBefore(10);
            kpiTable.setSpacingAfter(15);

            addKpiCell(kpiTable, "Total Tasks", String.valueOf(summary.getTotalTasks()), COLOR_PRIMARY);
            addKpiCell(kpiTable, "Completion Rate", summary.getTaskCompletionRate() + "%", new Color(16, 185, 129));
            addKpiCell(kpiTable, "Overdue Tasks", String.valueOf(summary.getOverdueTaskCount()), new Color(244, 63, 94));
            addKpiCell(kpiTable, "Due Soon (1-2d)", String.valueOf(summary.getDueSoonTaskCount()), new Color(245, 158, 11));
            doc.add(kpiTable);

            // Project Progress Section
            Paragraph projHeading = new Paragraph("Project Portfolio Progress", FONT_SECTION);
            projHeading.setSpacingAfter(6);
            doc.add(projHeading);

            PdfPTable projTable = new PdfPTable(new float[]{3.5f, 2.0f, 2.0f, 2.0f});
            projTable.setWidthPercentage(100);
            projTable.setSpacingAfter(15);

            addTableHeader(projTable, new String[]{"Project Name", "Health State", "Tasks (Done/Total)", "Completion %"});

            int pIdx = 0;
            for (ProjectProgressReport p : summary.getProjectProgressList()) {
                Color rowBg = (pIdx % 2 == 1) ? COLOR_ROW_ALT : Color.WHITE;
                addTableCell(projTable, p.getProjectName(), FONT_BODY_BOLD, Element.ALIGN_LEFT, rowBg);
                addTableCell(projTable, p.getHealth().name(), FONT_BODY, Element.ALIGN_CENTER, rowBg);
                addTableCell(projTable, p.getCompletedTasks() + " / " + p.getTotalTasks(), FONT_BODY, Element.ALIGN_CENTER, rowBg);
                addTableCell(projTable, p.getCompletionPercentage() + "%", FONT_BODY_BOLD, Element.ALIGN_CENTER, rowBg);
                pIdx++;
            }
            doc.add(projTable);

            // Team Workload Section
            Paragraph teamHeading = new Paragraph("Team Workload & Completion Analytics", FONT_SECTION);
            teamHeading.setSpacingAfter(6);
            doc.add(teamHeading);

            PdfPTable teamTable = new PdfPTable(new float[]{3.0f, 1.8f, 1.8f, 1.8f, 2.0f});
            teamTable.setWidthPercentage(100);

            addTableHeader(teamTable, new String[]{"Employee Name", "Assigned", "Active", "Overdue", "Completion %"});

            int tIdx = 0;
            for (EmployeePerformanceReport emp : summary.getEmployeeWorkloads()) {
                Color rowBg = (tIdx % 2 == 1) ? COLOR_ROW_ALT : Color.WHITE;
                addTableCell(teamTable, emp.getEmployeeName(), FONT_BODY_BOLD, Element.ALIGN_LEFT, rowBg);
                addTableCell(teamTable, String.valueOf(emp.getTotalTasks()), FONT_BODY, Element.ALIGN_CENTER, rowBg);
                addTableCell(teamTable, String.valueOf(emp.getPendingTasks()), FONT_BODY, Element.ALIGN_CENTER, rowBg);
                addTableCell(teamTable, String.valueOf(emp.getOverdueTasks()), FONT_BODY, Element.ALIGN_CENTER, rowBg);
                addTableCell(teamTable, emp.getCompletionRate() + "%", FONT_BODY_BOLD, Element.ALIGN_CENTER, rowBg);
                tIdx++;
            }
            doc.add(teamTable);

            addFooter(doc);
        } catch (DocumentException de) {
            throw new IOException("Failed to generate Analytics PDF report: " + de.getMessage(), de);
        } finally {
            if (doc.isOpen()) doc.close();
        }
    }

    // --- Styling Helpers ---

    private static void addDocumentHeader(Document doc, String titleText, String subtitleText) throws DocumentException {
        Paragraph title = new Paragraph("⚡ IntelliFlow Platform", FONT_TITLE);
        title.setSpacingAfter(2);
        doc.add(title);

        Paragraph reportTitle = new Paragraph(titleText, FONT_SUBTITLE);
        reportTitle.setSpacingAfter(4);
        doc.add(reportTitle);

        Paragraph meta = new Paragraph(subtitleText + " | Generated on " + LocalDateTime.now().format(TIME_FMT), FONT_META);
        meta.setSpacingAfter(10);
        doc.add(meta);

        // Decorative horizontal separator
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(COLOR_PRIMARY);
        cell.setFixedHeight(2f);
        cell.setBorder(Rectangle.NO_BORDER);
        line.addCell(cell);
        line.setSpacingAfter(10);
        doc.add(line);
    }

    private static void addTableHeader(PdfPTable table, String[] headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, FONT_HEADER_CELL));
            cell.setBackgroundColor(COLOR_HEADER_BG);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPaddingTop(6);
            cell.setPaddingBottom(6);
            cell.setBorderColor(COLOR_BORDER);
            table.addCell(cell);
        }
    }

    private static void addTableCell(PdfPTable table, String text, Font font, int align, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(bg);
        cell.setPaddingTop(5);
        cell.setPaddingBottom(5);
        cell.setPaddingLeft(6);
        cell.setPaddingRight(6);
        cell.setBorderColor(COLOR_BORDER);
        table.addCell(cell);
    }

    private static void addKpiCell(PdfPTable table, String label, String value, Color accent) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(COLOR_ROW_ALT);
        cell.setBorderColor(COLOR_BORDER);
        cell.setPadding(8);

        Paragraph pVal = new Paragraph(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, accent));
        pVal.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pVal);

        Paragraph pLbl = new Paragraph(label, FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_MUTED));
        pLbl.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pLbl);

        table.addCell(cell);
    }

    private static void addFooter(Document doc) throws DocumentException {
        Paragraph footer = new Paragraph("Confidential — Generated by IntelliFlow Smart Workflow Automation Platform", FONT_META);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(18);
        doc.add(footer);
    }

    private static Color getHealthColor(ProjectHealth health) {
        return switch (health) {
            case ON_TRACK -> new Color(16, 185, 129);
            case AT_RISK -> new Color(245, 158, 11);
            case DELAYED -> new Color(244, 63, 94);
        };
    }

    private static String resolveUserName(Integer userId, List<User> allUsers) {
        if (userId == null) return "Unassigned";
        return allUsers.stream()
                .filter(u -> u.getId() == userId)
                .map(User::getFullName)
                .findFirst()
                .orElse("User #" + userId);
    }

    private static String resolveProjectName(int projectId, List<Project> projects) {
        return projects.stream()
                .filter(p -> p.getId() == projectId)
                .map(Project::getName)
                .findFirst()
                .orElse("Project #" + projectId);
    }
}

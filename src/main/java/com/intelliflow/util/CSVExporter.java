package com.intelliflow.util;

import com.intelliflow.model.ProjectProgressReport;
import com.intelliflow.model.Task;
import com.intelliflow.model.User;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class CSVExporter {

    private CSVExporter() {}

    /**
     * Exports a compiled Project report and its task lists to a target CSV file.
     * Escapes values containing commas or quotes.
     */
    public static void exportProjectReport(ProjectProgressReport report, List<Task> tasks, List<User> allUsers, File file) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            // Write Report Summary Section
            writer.write("INTELLIFLOW PROJECT STATUS REPORT");
            writer.newLine();
            writer.write("Project Name," + escapeCSV(report.getProjectName()));
            writer.newLine();
            writer.write("Project ID," + report.getProjectId());
            writer.newLine();
            writer.write("Completion Rate," + report.getCompletionPercentage() + "%");
            writer.newLine();
            writer.write("Total Tasks," + report.getTotalTasks());
            writer.newLine();
            writer.write("Completed Tasks," + report.getCompletedTasks());
            writer.newLine();
            writer.write("Pending Tasks," + report.getPendingTasks());
            writer.newLine();
            writer.write("Blocked Tasks," + report.getBlockedTasks());
            writer.newLine();
            writer.write("Overdue Tasks," + report.getOverdueTasks());
            writer.newLine();
            writer.newLine();

            // Write Task List Section Headers
            writer.write("Task ID,Task Name,Description,Assigned Employee,Priority,Deadline,Status");
            writer.newLine();

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            // Write individual Tasks rows
            for (Task t : tasks) {
                String empName = "Unassigned";
                if (t.getAssignedEmployeeId() != null) {
                    Optional<User> uOpt = allUsers.stream()
                            .filter(u -> u.getId() == t.getAssignedEmployeeId())
                            .findFirst();
                    if (uOpt.isPresent()) {
                        empName = uOpt.get().getFullName();
                    }
                }

                StringBuilder sb = new StringBuilder();
                sb.append(t.getId()).append(",");
                sb.append(escapeCSV(t.getName())).append(",");
                sb.append(escapeCSV(t.getDescription() != null ? t.getDescription() : "")).append(",");
                sb.append(escapeCSV(empName)).append(",");
                sb.append(t.getPriority().name()).append(",");
                sb.append(t.getDeadline() != null ? t.getDeadline().format(dtf) : "").append(",");
                sb.append(t.getStatus().name());

                writer.write(sb.toString());
                writer.newLine();
            }
        }
    }

    private static String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}

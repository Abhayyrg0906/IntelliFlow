package com.intelliflow.util;

import com.intelliflow.enums.TaskStatus;
import com.intelliflow.model.Task;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DeadlineTimelineUtil {

    public static class DeadlineSection {
        private final String title;
        private final String subtitle;
        private final String badge;
        private final LocalDate date; // null for overdue or no-deadline
        private final boolean isOverdue;
        private final boolean isToday;
        private final boolean isTomorrow;
        private final List<Task> tasks;

        public DeadlineSection(String title, String subtitle, String badge, LocalDate date, boolean isOverdue, boolean isToday, boolean isTomorrow, List<Task> tasks) {
            this.title = title;
            this.subtitle = subtitle;
            this.badge = badge;
            this.date = date;
            this.isOverdue = isOverdue;
            this.isToday = isToday;
            this.isTomorrow = isTomorrow;
            this.tasks = tasks;
        }

        public String getTitle() { return title; }
        public String getSubtitle() { return subtitle; }
        public String getBadge() { return badge; }
        public LocalDate getDate() { return date; }
        public boolean isOverdue() { return isOverdue; }
        public boolean isToday() { return isToday; }
        public boolean isTomorrow() { return isTomorrow; }
        public List<Task> getTasks() { return tasks; }
    }

    private static final DateTimeFormatter MONTH_DAY_FORMATTER = DateTimeFormatter.ofPattern("MMM dd", Locale.US);
    private static final DateTimeFormatter FULL_DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy", Locale.US);

    /**
     * Groups tasks chronologically by deadline while preserving overdue tasks, prioritizing today/tomorrow,
     * and ordering tasks within each date by priority.
     */
    public static List<DeadlineSection> groupTasksByDeadline(List<Task> tasks, LocalDate today) {
        if (tasks == null) return Collections.emptyList();
        if (today == null) today = LocalDate.now();

        // 1. Deduplicate tasks by task ID
        Map<Integer, Task> uniqueMap = new LinkedHashMap<>();
        for (Task t : tasks) {
            if (t != null && !uniqueMap.containsKey(t.getId())) {
                uniqueMap.put(t.getId(), t);
            }
        }
        List<Task> uniqueTasks = new ArrayList<>(uniqueMap.values());

        // 2. Separate into Overdue, Dated, and No-Deadline
        List<Task> overdueTasks = new ArrayList<>();
        Map<LocalDate, List<Task>> dateMap = new TreeMap<>(); // sorted chronologically
        List<Task> noDeadlineTasks = new ArrayList<>();

        for (Task t : uniqueTasks) {
            if (t.getDeadline() == null) {
                noDeadlineTasks.add(t);
            } else if (t.getStatus() != TaskStatus.COMPLETED && t.getDeadline().isBefore(today)) {
                overdueTasks.add(t);
            } else {
                dateMap.computeIfAbsent(t.getDeadline(), d -> new ArrayList<>()).add(t);
            }
        }

        // Priority Comparator: CRITICAL -> HIGH -> MEDIUM -> LOW, then Name
        Comparator<Task> taskComparator = TaskSorter.RECOMMENDED_COMPARATOR;

        List<DeadlineSection> sections = new ArrayList<>();

        // Section: OVERDUE
        if (!overdueTasks.isEmpty()) {
            overdueTasks.sort(taskComparator);
            sections.add(new DeadlineSection(
                    "⛔ Overdue Tasks",
                    "Requires immediate action (" + overdueTasks.size() + " task" + (overdueTasks.size() > 1 ? "s" : "") + ")",
                    "⛔",
                    null,
                    true, false, false,
                    overdueTasks
            ));
        }

        // Sections: Chronological Dates (Today, Tomorrow, Upcoming)
        LocalDate tomorrow = today.plusDays(1);
        for (Map.Entry<LocalDate, List<Task>> entry : dateMap.entrySet()) {
            LocalDate date = entry.getKey();
            List<Task> dateTasks = entry.getValue();
            dateTasks.sort(taskComparator);

            boolean isToday = date.isEqual(today);
            boolean isTomorrow = date.isEqual(tomorrow);

            String title;
            String badge;
            String subtitle;

            if (isToday) {
                title = "Today";
                badge = "🔥";
                subtitle = date.format(FULL_DATE_FORMATTER);
            } else if (isTomorrow) {
                title = "Tomorrow";
                badge = "⚠️";
                subtitle = date.format(FULL_DATE_FORMATTER);
            } else {
                title = date.format(MONTH_DAY_FORMATTER);
                badge = "📅";
                subtitle = date.format(FULL_DATE_FORMATTER);
            }

            sections.add(new DeadlineSection(
                    title,
                    subtitle,
                    badge,
                    date,
                    false, isToday, isTomorrow,
                    dateTasks
            ));
        }

        // Section: No Deadline (if any)
        if (!noDeadlineTasks.isEmpty()) {
            noDeadlineTasks.sort(taskComparator);
            sections.add(new DeadlineSection(
                    "📋 No Deadline",
                    "Unscheduled backlog tasks (" + noDeadlineTasks.size() + ")",
                    "📋",
                    null,
                    false, false, false,
                    noDeadlineTasks
            ));
        }

        return sections;
    }
}

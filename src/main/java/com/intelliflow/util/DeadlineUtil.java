package com.intelliflow.util;

import com.intelliflow.enums.DeadlineState;
import com.intelliflow.enums.TaskStatus;
import com.intelliflow.model.Task;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class DeadlineUtil {

    public static final DateTimeFormatter FRIENDLY_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US);

    public static DeadlineState calculateDeadlineState(Task task, LocalDate referenceDate) {
        if (task == null || task.getDeadline() == null) {
            return DeadlineState.NO_DEADLINE;
        }

        if (task.getStatus() == TaskStatus.COMPLETED) {
            return DeadlineState.COMPLETED;
        }

        if (referenceDate == null) {
            referenceDate = LocalDate.now();
        }

        long daysRemaining = ChronoUnit.DAYS.between(referenceDate, task.getDeadline());

        if (daysRemaining < 0) {
            return DeadlineState.OVERDUE;
        } else if (daysRemaining == 0) {
            return DeadlineState.DUE_TODAY;
        } else if (daysRemaining <= 2) {
            return DeadlineState.DUE_SOON;
        } else if (daysRemaining <= 7) {
            return DeadlineState.UPCOMING;
        } else {
            return DeadlineState.ON_SCHEDULE;
        }
    }

    public static DeadlineState calculateDeadlineState(Task task) {
        return calculateDeadlineState(task, LocalDate.now());
    }

    public static String formatDeadlineDisplay(Task task, LocalDate referenceDate, DateTimeFormatter formatter) {
        if (task == null || task.getDeadline() == null) {
            return "📅 No deadline";
        }

        DateTimeFormatter dtf = (formatter != null) ? formatter : FRIENDLY_DATE_FORMATTER;
        String formattedDate = task.getDeadline().format(dtf);

        if (task.getStatus() == TaskStatus.COMPLETED) {
            return "📅 Due: " + formattedDate;
        }

        DeadlineState state = calculateDeadlineState(task, referenceDate);
        return switch (state) {
            case OVERDUE -> "⛔ Overdue: " + formattedDate;
            case DUE_TODAY -> "🔥 Due Today";
            case DUE_SOON -> "⚠️ Due Soon: " + formattedDate;
            case UPCOMING -> "📅 Due Soon: " + formattedDate;
            case ON_SCHEDULE -> "📅 Due: " + formattedDate;
            case NO_DEADLINE -> "📅 No deadline";
            case COMPLETED -> "📅 Due: " + formattedDate;
        };
    }

    public static String formatDeadlineDisplay(Task task) {
        return formatDeadlineDisplay(task, LocalDate.now(), FRIENDLY_DATE_FORMATTER);
    }
}

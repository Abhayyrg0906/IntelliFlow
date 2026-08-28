package com.intelliflow.util;

import com.intelliflow.enums.TaskPriority;
import com.intelliflow.model.Task;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;

public class TaskSorter {

    public static int getPriorityWeight(TaskPriority p) {
        if (p == null) return 0;
        return switch (p) {
            case CRITICAL -> 4;
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }

    public static int comparePriority(Task t1, Task t2) {
        return Integer.compare(getPriorityWeight(t2.getPriority()), getPriorityWeight(t1.getPriority()));
    }

    public static int compareDeadline(Task t1, Task t2) {
        LocalDate d1 = t1.getDeadline();
        LocalDate d2 = t2.getDeadline();
        if (d1 == null && d2 == null) return 0;
        if (d1 == null) return 1;  // nulls last
        if (d2 == null) return -1; // nulls last
        return d1.compareTo(d2);
    }

    public static int compareCreatedDate(Task t1, Task t2) {
        LocalDateTime c1 = t1.getCreatedAt();
        LocalDateTime c2 = t2.getCreatedAt();
        if (c1 == null && c2 == null) return 0;
        if (c1 == null) return 1;  // nulls last
        if (c2 == null) return -1;
        return c2.compareTo(c1); // Descending (newest first)
    }

    public static int compareName(Task t1, Task t2) {
        String n1 = t1.getName();
        String n2 = t2.getName();
        if (n1 == null && n2 == null) return 0;
        if (n1 == null) return 1;  // nulls last
        if (n2 == null) return -1;
        return n1.compareToIgnoreCase(n2);
    }

    public static final Comparator<Task> RECOMMENDED_COMPARATOR = (t1, t2) -> {
        int res = comparePriority(t1, t2);
        if (res != 0) return res;
        res = compareDeadline(t1, t2);
        if (res != 0) return res;
        res = compareCreatedDate(t1, t2);
        if (res != 0) return res;
        return compareName(t1, t2);
    };

    public static final Comparator<Task> PRIORITY_COMPARATOR = (t1, t2) -> {
        int res = comparePriority(t1, t2);
        if (res != 0) return res;
        res = compareDeadline(t1, t2);
        if (res != 0) return res;
        return compareName(t1, t2);
    };

    public static final Comparator<Task> DEADLINE_COMPARATOR = (t1, t2) -> {
        int res = compareDeadline(t1, t2);
        if (res != 0) return res;
        res = comparePriority(t1, t2);
        if (res != 0) return res;
        return compareName(t1, t2);
    };

    public static final Comparator<Task> CREATED_DATE_COMPARATOR = (t1, t2) -> {
        int res = compareCreatedDate(t1, t2);
        if (res != 0) return res;
        return compareName(t1, t2);
    };

    public static final Comparator<Task> NAME_COMPARATOR = (t1, t2) -> compareName(t1, t2);
}

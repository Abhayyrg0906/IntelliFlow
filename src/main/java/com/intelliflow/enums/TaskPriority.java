package com.intelliflow.enums;

public enum TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public static TaskPriority fromString(String priorityStr) {
        for (TaskPriority priority : TaskPriority.values()) {
            if (priority.name().equalsIgnoreCase(priorityStr)) {
                return priority;
            }
        }
        throw new IllegalArgumentException("Unknown task priority: " + priorityStr);
    }
}

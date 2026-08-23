package com.intelliflow.enums;

public enum TaskStatus {
    TO_DO,
    IN_PROGRESS,
    TESTING,
    COMPLETED,
    BLOCKED;

    public static TaskStatus fromString(String statusStr) {
        for (TaskStatus status : TaskStatus.values()) {
            if (status.name().equalsIgnoreCase(statusStr)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown task status: " + statusStr);
    }
}

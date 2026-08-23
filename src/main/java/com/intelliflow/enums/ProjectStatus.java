package com.intelliflow.enums;

public enum ProjectStatus {
    PLANNED,
    ACTIVE,
    COMPLETED,
    ON_HOLD,
    CANCELLED;

    public static ProjectStatus fromString(String statusStr) {
        for (ProjectStatus status : ProjectStatus.values()) {
            if (status.name().equalsIgnoreCase(statusStr)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown project status: " + statusStr);
    }
}

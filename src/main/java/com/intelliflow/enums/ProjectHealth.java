package com.intelliflow.enums;

public enum ProjectHealth {
    ON_TRACK("🟢 ON TRACK"),
    AT_RISK("🟡 AT RISK"),
    DELAYED("🔴 DELAYED");

    private final String displayName;

    ProjectHealth(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

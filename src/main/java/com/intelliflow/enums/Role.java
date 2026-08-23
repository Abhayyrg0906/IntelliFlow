package com.intelliflow.enums;

public enum Role {
    ADMIN,
    MANAGER,
    EMPLOYEE;

    public static Role fromString(String roleStr) {
        for (Role role : Role.values()) {
            if (role.name().equalsIgnoreCase(roleStr)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + roleStr);
    }
}

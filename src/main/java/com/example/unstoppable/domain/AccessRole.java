package com.example.unstoppable.domain;

public enum AccessRole {
    OWNER,
    EDITOR,
    VIEWER;

    public boolean canEdit() {
        return this != VIEWER;
    }
}

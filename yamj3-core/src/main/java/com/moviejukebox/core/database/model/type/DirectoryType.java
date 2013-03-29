package com.moviejukebox.core.database.model.type;

public enum DirectoryType {

    DVD,
    BLURAY,
    STANDARD;

    public static DirectoryType fromString(String type) {
        return DirectoryType.valueOf(type.trim().toUpperCase());
    }
}

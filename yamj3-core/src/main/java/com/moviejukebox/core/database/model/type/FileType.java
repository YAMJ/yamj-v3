package com.moviejukebox.core.database.model.type;

public enum FileType {

    VIDEO,
    IMAGE,
    SUBTITLE,
    NFO,
    UNKNOWN;

    public static FileType fromString(String type) {
        try {
            return FileType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
}

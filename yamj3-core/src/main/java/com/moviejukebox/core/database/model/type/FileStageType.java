package com.moviejukebox.core.database.model.type;

public enum FileStageType {

    UNKNOWN,
    NEW,
    ERROR,
    PENDING,
    DELETION;

    public static FileStageType fromString(String type) {
        try {
            return FileStageType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
}

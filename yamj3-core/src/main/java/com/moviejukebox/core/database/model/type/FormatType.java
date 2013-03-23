package com.moviejukebox.core.database.model.type;

/**
 * Enumeration for format type.
 */
public enum FormatType {

    UNKNOWN,
    BLURAY,
    DVD,
    FILE;

    public static FormatType fromString(String type) {
        try {
            return FormatType.valueOf(type.trim().toUpperCase());
        } catch (Exception ignore) {}
        return UNKNOWN;
    }
}

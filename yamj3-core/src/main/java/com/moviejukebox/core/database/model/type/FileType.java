package com.moviejukebox.core.database.model.type;

public enum FileType {

    VIDEO,
    TRAILER,
    POSTER,
    VIDEOIMAGE,
    FANART,
    SUBTITLE,
    NFO,
    UNKNOWN;

    public static FileType fromString(String type) {
        try {
            return FileType.valueOf(type.trim().toUpperCase());
        } catch (Exception ex) {
            return UNKNOWN;
        }
    }
}

package com.yamj.core.database.model.type;

public enum ArtworkType {

    UNKNOWN,
    POSTER,
    BANNER,
    FANART,
    VIDEOIMAGE,
    PERSON;

    public static ArtworkType fromString(String type) {
        try {
            return ArtworkType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
}

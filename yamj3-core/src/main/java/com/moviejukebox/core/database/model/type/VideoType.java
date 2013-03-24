package com.moviejukebox.core.database.model.type;

/**
 * Enumeration for video type.
 */
public enum VideoType {

	UNKNOWN,
	MOVIE,
	TVSHOW;

    public static VideoType fromString(String type) {
        try {
            return VideoType.valueOf(type.trim().toUpperCase());
        } catch (Exception ignore) {}
        return UNKNOWN;
    }
}
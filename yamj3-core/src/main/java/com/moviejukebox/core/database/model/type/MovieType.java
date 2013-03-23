package com.moviejukebox.core.database.model.type;

/**
 * Enumeration for movie type.
 */
public enum MovieType {

	UNKNOWN,
	MOVIE,
	TVSHOW;

    public static MovieType fromString(String type) {
        try {
            return MovieType.valueOf(type.trim().toUpperCase());
        } catch (Exception ignore) {}
        return UNKNOWN;
    }
}
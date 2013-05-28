package com.yamj.core.database.model.type;

/**
 * The list of override flags
 */
public enum OverrideFlag {

    UNKNOWN,
    COUNTRY,
    ORIGINALTITLE,
    OUTLINE,
    PLOT,
    QUOTE,
    RELEASEDATE,
    TAGLINE,
    TITLE,
    YEAR;

    public static OverrideFlag fromString(String overrideFlag) {
        try {
            return OverrideFlag.valueOf(overrideFlag.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
}

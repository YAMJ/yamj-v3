package com.moviejukebox.core.database.model.type;

/**
 * The list of override flags
 */
public enum OverrideFlag {

    UNKNOWN,
    ACTORS,
    ASPECTRATIO,
    CERTIFICATION,
    COMPANY,
    CONTAINER,
    COUNTRY,
    DIRECTORS,
    FPS, // frames per second
    GENRES,
    LANGUAGE,
    ORIGINALTITLE,
    OUTLINE,
    PLOT,
    QUOTE,
    RELEASEDATE,
    RESOLUTION,
    RUNTIME,
    TAGLINE,
    TITLE,
    VIDEOOUTPUT,
    VIDEOSOURCE,
    WRITERS,
    YEAR,
    // extra for people scraping
    PEOPLE_ACTORS,
    PEOPLE_DIRECTORS,
    PEOPLE_WRITERS,
    // extra for TV episodes
    EPISODE_FIRST_AIRED,
    EPISODE_PLOT,
    EPISODE_RATING,
    EPISODE_TITLE;

    public static OverrideFlag fromString(String overrideFlag) {
        try {
            return OverrideFlag.valueOf(overrideFlag.trim().toUpperCase());
        } catch (Exception ignore) {}
        return UNKNOWN;
    }
}

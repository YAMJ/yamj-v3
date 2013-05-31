package org.yamj.core.database.model.type;

/**
 * List of known job types for people
 */
public enum JobType {

    /**
     * The job type is not known
     */
    UNKNOWN,
    /**
     * The person is a director
     */
    DIRECTOR,
    /**
     * The person is an actor
     */
    ACTOR,
    /**
     * The person is a writer
     */
    WRITER,
    /**
     * The person is a producer
     */
    PRODUCER,
    /*
     * Camera and film related jobs
     */
    CAMERA,
    /*
     * Editing jobs, e.g. Editor, Dialog editor, Color Timer
     */
    EDITING,
    /*
     * All art related jobs
     */
    ART,
    /*
     * Costume and Make-up jobs
     */
    COSTUME_MAKEUP,
    /*
     * Sound jobs
     */
    SOUND,
    /*
     * Special effects
     */
    EFFECTS,
    /*
     * Misc Crew
     */
    CREW,
    /*
     * Lighting jobs
     */
    LIGHTING;

    /**
     * Determine the job type from a string
     *
     * @param type
     * @return
     */
    public static JobType fromString(String type) {
        try {
            return JobType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
}

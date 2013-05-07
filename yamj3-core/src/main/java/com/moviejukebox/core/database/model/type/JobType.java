package com.moviejukebox.core.database.model.type;

public enum JobType {

    UNKNOWN,
    DIRECTOR,
    ACTOR,
    WRITER,
    PRODUCER;

    public static JobType fromString(String type) {
        try {
            return JobType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
}

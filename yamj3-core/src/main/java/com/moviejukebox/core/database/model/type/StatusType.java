package com.moviejukebox.core.database.model.type;

public enum StatusType {

    NEW,
    UPDATED,
    DELETED,
    PENDING,
    DONE;

    public static StatusType fromString(String type) {
        try {
            return StatusType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return NEW;
        }
    }
}

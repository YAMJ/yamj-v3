package org.yamj.common.type;

public enum StatusType {

    NEW,
    UPDATED,
    DELETED,
    ERROR,
    PROCESSED,
    MISSING,
    DONE;
    
    public static StatusType fromString(String type) {
        try {
            return StatusType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return NEW;
        }
    }
}

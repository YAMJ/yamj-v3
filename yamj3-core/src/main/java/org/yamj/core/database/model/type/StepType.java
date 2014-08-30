package org.yamj.core.database.model.type;

/**
 * The list of steps.
 */
public enum StepType {

    NFO,
    ONLINE,
    SCANNED;

    public static StepType fromString(String stepType) {
        try {
            return StepType.valueOf(stepType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            // defaults to online
            return ONLINE;
        }
    }
}

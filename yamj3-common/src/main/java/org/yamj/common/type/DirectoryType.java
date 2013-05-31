package org.yamj.common.type;

public enum DirectoryType {

    DVD,
    BLURAY,
    STANDARD;

    public static DirectoryType fromString(String type) {
        try {
            return DirectoryType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return STANDARD;
        }
    }
}

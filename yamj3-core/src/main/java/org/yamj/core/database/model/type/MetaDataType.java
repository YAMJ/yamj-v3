package org.yamj.core.database.model.type;

/**
 * The meta data type used for the data scanning in the database
 *
 * @author stuart.boston
 */
public enum MetaDataType {

    VIDEODATA,
    SEASON,
    SERIES,
    PERSON;

    public static MetaDataType fromString(String type) {
        try {
            return MetaDataType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return VIDEODATA;
        }
    }
}

package org.yamj.core.database.model;

import org.yamj.common.type.StatusType;

public interface IMetadata {

    String getSourcedbId(String sourcedb);

    void setSourcedbId(String sourcedb, String id);

    String getTitle();
    
    String getTitleOriginal();
    
    int getYear();
    
    int getSeasonNumber();

    int getEpisodeNumber();

    StatusType getStatus();
}
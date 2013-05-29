package com.yamj.core.database.model;

import com.yamj.common.type.StatusType;

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
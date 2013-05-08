package com.yamj.core.database.model;

public interface ISourcedbIdentifiable {

    String getSourcedbId(String sourcedb);

    void setSourcedbId(String sourcedb, String id);
}
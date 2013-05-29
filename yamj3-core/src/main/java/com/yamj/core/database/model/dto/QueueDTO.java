package com.yamj.core.database.model.dto;

import com.yamj.core.database.model.type.ArtworkType;
import com.yamj.core.database.model.type.MetaDataType;
import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class QueueDTO {

    private Long id;
    private Date date;
    private MetaDataType metadataType;
    private ArtworkType artworkType;
    
    // GETTER and SETTER
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public MetaDataType getMetadataType() {
        return metadataType;
    }

    public void setMetadataType(String metadataType) {
        this.metadataType = MetaDataType.fromString(metadataType);
    }

    public void setMetadataType(MetaDataType metadataType) {
        this.metadataType = metadataType;
    }

    public boolean isMetadataType(MetaDataType metadataType) {
        return this.metadataType == metadataType;
    }

    public ArtworkType getArtworkType() {
        return artworkType;
    }

    public void setArtworkType(String artworkType) {
        this.artworkType = ArtworkType.fromString(artworkType);
    }

    public void setArtworkType(ArtworkType artworkType) {
        this.artworkType = artworkType;
    }

    public boolean isArtworkType(ArtworkType artworkType) {
        return this.artworkType == artworkType;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE);
    }
}

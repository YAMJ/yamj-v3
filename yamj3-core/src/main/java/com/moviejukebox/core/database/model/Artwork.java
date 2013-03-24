package com.moviejukebox.core.database.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import com.moviejukebox.core.database.model.type.ArtworkType;
import com.moviejukebox.core.hibernate.usertypes.EnumStringUserType;

@TypeDef(name = "artworkType",
    typeClass = EnumStringUserType.class,
    parameters = {@Parameter(name = "enumClassName", value = "com.moviejukebox.core.database.model.type.ArtworkType")})

@Entity
@Table(name = "artwork")
public class Artwork extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = -981494909436217076L;

    @Type(type = "artworkType")
    @Column(name = "artwork_type", nullable = false)
    private ArtworkType artworkType;
    
    @Column(name = "filename", nullable = false)
    private String  filename;
    
    @Column(name = "url")
    private String  url;

    // GETTER and SETTER
    
    public ArtworkType getArtworkType() {
        return artworkType;
    }

    public void setArtworkType(ArtworkType artworkType) {
        this.artworkType = artworkType;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

package com.moviejukebox.core.database.model;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.NaturalId;

@Entity
@Table(name = "studio")
public class Studio extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = -5113519542293276527L;
    
    @NaturalId(mutable = true)
    @Column(name = "name", nullable = false, length = 200)
    private String name;
    
    // GETTER and SETTER
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // EQUALITY CHECKS

    @Override
    public int hashCode() {
        final int PRIME = 17;
        int result = 1;
        result = PRIME * result + (this.name == null?0:this.name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if ( this == other ) return true;
        if ( other == null ) return false;
        if ( !(other instanceof Studio) ) return false;
        Studio castOther = (Studio)other;
        return StringUtils.equals(this.name, castOther.name);
    }
}

package com.yamj.core.database.model;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.NaturalId;

@Entity
@Table(name = "genre")
public class Genre extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = -5113519542293276527L;
    @NaturalId(mutable = true)
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    public Genre() {
    }

    public Genre(String name) {
        this.name = name;
    }

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
        result = PRIME * result + (this.name == null ? 0 : this.name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!(other instanceof Genre)) {
            return false;
        }
        Genre castOther = (Genre) other;
        return StringUtils.equals(this.name, castOther.name);
    }
}

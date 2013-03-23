package com.moviejukebox.core.database.model;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.NaturalId;

@Entity
@Table(name = "set_descriptor")
public class SetDescriptor  extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = 3074855702659953694L;

    @NaturalId(mutable = true)
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_SET_POSTER")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "posterId")
    private Artwork poster;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_SET_FANART")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "fanartId")
    private Artwork fanart;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Artwork getPoster() {
        return poster;
    }

    public void setPoster(Artwork poster) {
        this.poster = poster;
    }

    public Artwork getFanart() {
        return fanart;
    }

    public void setFanart(Artwork fanart) {
        this.fanart = fanart;
    }

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
        if ( !(other instanceof SetDescriptor) ) return false;
        SetDescriptor castOther = (SetDescriptor)other;
        return StringUtils.equals(this.name, castOther.name);
    }
}

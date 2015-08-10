/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.database.model;

import java.io.Serializable;
import java.util.*;
import javax.persistence.*;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NaturalId;

@Entity
@Table(name = "boxed_set",
        uniqueConstraints = @UniqueConstraint(name = "UIX_BOXEDSET_NATURALID", columnNames = {"identifier"})
)
@SuppressWarnings("unused")
public class BoxedSet extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = 3074855702659953694L;

    /**
     * This will be generated from a scanned file name.
     */
    @NaturalId
    @Column(name = "identifier", length = 100, nullable = false)
    private String identifier;
    
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "boxedSet")
    private List<Artwork> artworks = new ArrayList<>(0);

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "boxed_set_ids", joinColumns = @JoinColumn(name = "boxedset_id", foreignKey = @ForeignKey(name = "FK_BOXEDSET_SOURCEIDS")))
    @Fetch(FetchMode.SELECT)
    @MapKeyColumn(name = "sourcedb", length = 40)
    @Column(name = "sourcedb_id", length = 200, nullable = false)
    private Map<String, String> sourceDbIdMap = new HashMap<>(0);

    // CONSTRUCTORS
    
    public BoxedSet() {}

    public BoxedSet(String identifier) {
        setIdentifier(identifier);
    }
    
    // GETTER and SETTER
    
    public String getIdentifier() {
        return identifier;
    }

    private void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Artwork> getArtworks() {
        return artworks;
    }

    public void setArtworks(List<Artwork> artworks) {
        this.artworks = artworks;
    }

    private Map<String, String> getSourceDbIdMap() {
        return sourceDbIdMap;
    }

    private void setSourceDbIdMap(Map<String, String> sourceDbIdMap) {
        this.sourceDbIdMap = sourceDbIdMap;
    }
    
    public final String getSourceDbId(String sourceDb) {
        return getSourceDbIdMap().get(sourceDb);
    }
    
    public final boolean setSourceDbId(String sourceDb, String id) {
        if (StringUtils.isBlank(sourceDb) || StringUtils.isBlank(id)) {
            return false;
        }
        String newId = id.trim();
        String oldId = getSourceDbIdMap().put(sourceDb, newId);
        return !StringUtils.equals(oldId, newId);
    }

    public final boolean removeSourceDbId(String sourceDb) {
        String removedId = getSourceDbIdMap().remove(sourceDb);
        return (removedId != null);
    }
    
    // EQUALITY CHECKS
    
    @Override
    public int hashCode() {
        final int prime = 7;
        int result = 1;
        result = prime * result + (getIdentifier() == null ? 0 : getIdentifier().toLowerCase().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof BoxedSet)) {
            return false;
        }
        final BoxedSet other = (BoxedSet) obj;
        // first check the id
        if ((getId() > 0) && (other.getId() > 0)) {
            return getId() == other.getId();
        }
        // check the name
        return StringUtils.equalsIgnoreCase(getIdentifier(), other.getIdentifier());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BoxedSet [ID=");
        sb.append(getId());
        sb.append(", identifier=");
        sb.append(getIdentifier());
        sb.append(", name=");
        sb.append(getName());
        sb.append("]");
        return sb.toString();
    }
}

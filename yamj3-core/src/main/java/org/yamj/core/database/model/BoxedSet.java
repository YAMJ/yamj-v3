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
import org.yamj.core.api.model.dto.ApiBoxedSetDTO;

@NamedQueries({    
    @NamedQuery(name = BoxedSet.QUERY_ORPHANS,
        query = "SELECT b.id FROM BoxedSet b WHERE not exists (select 1 from BoxedSetOrder o where o.boxedSet=b)"
    )
})

@NamedNativeQueries({    
    @NamedNativeQuery(name = "metadata.boxedset.series", resultSetMapping="metadata.boxedset",
        query = "SELECT bs.id, bs.name,(select count(bo2.id) from boxed_set_order bo2 where bo2.boxedset_id=bs.id) as memberCount "+
                "FROM boxed_set bs JOIN boxed_set_order bo ON bs.id=bo.boxedset_id "+
                "WHERE bo.series_id=:id GROUP BY bs.id, bs.name"
    ),
    @NamedNativeQuery(name = "metadata.boxedset.season", resultSetMapping="metadata.boxedset",
        query = "SELECT bs.id, bs.name,(select count(bo2.id) from boxed_set_order bo2 where bo2.boxedset_id=bs.id) as memberCount "+
                "FROM boxed_set bs JOIN boxed_set_order bo ON bs.id=bo.boxedset_id "+
                "JOIN season sea ON sea.series_id=bo.series_id AND sea.id=:id GROUP BY bs.id, bs.name"
    ),
    @NamedNativeQuery(name = "metadata.boxedset.episode", resultSetMapping="metadata.boxedset",
        query = "SELECT bs.id, bs.name,(select count(bo2.id) from boxed_set_order bo2 where bo2.boxedset_id=bs.id) as memberCount "+
                "FROM boxed_set bs JOIN boxed_set_order bo ON bs.id=bo.boxedset_id "+
                "JOIN season sea ON sea.series_id=bo.series_id JOIN videodata vd ON vd.season_id=sea.id AND vd.id=:id GROUP BY bs.id, bs.name"
    ),
    @NamedNativeQuery(name = "metadata.boxedset.movie", resultSetMapping="metadata.boxedset",
        query = "SELECT bs.id, bs.name,(select count(bo2.id) from boxed_set_order bo2 where bo2.boxedset_id=bs.id) as memberCount "+
                "FROM boxed_set bs JOIN boxed_set_order bo ON bs.id=bo.boxedset_id "+
                "WHERE bo.videodata_id=:id GROUP BY bs.id, bs.name"
    )
})

@SqlResultSetMapping(name="metadata.boxedset", classes={
    @ConstructorResult(
        targetClass=ApiBoxedSetDTO.class,
        columns={
             @ColumnResult(name="id", type=Long.class),
             @ColumnResult(name="name", type=String.class),
             @ColumnResult(name="memberCount", type=Integer.class),
        }
    )}
)

@Entity
@Table(name = "boxed_set",
        uniqueConstraints = @UniqueConstraint(name = "UIX_BOXEDSET_NATURALID", columnNames = {"identifier"})
)
@SuppressWarnings("unused")
public class BoxedSet extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = 3074855702659953694L;
    public static final String QUERY_ORPHANS = "boxedset.orphans";
    
    @NaturalId
    @Column(name = "identifier", length = 100, nullable = false)
    private String identifier;
    
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "boxedSet")
    private List<Artwork> artworks = new ArrayList<>(0);

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "boxed_set_ids",
            joinColumns = @JoinColumn(name = "boxedset_id"), 
            foreignKey = @ForeignKey(name = "FK_BOXEDSET_SOURCEIDS"))
    @Fetch(FetchMode.JOIN)
    @MapKeyColumn(name = "sourcedb", length = 40)
    @Column(name = "sourcedb_id", length = 200, nullable = false)
    private Map<String, String> sourceDbIdMap = new HashMap<>(0);

    // CONSTRUCTORS
    
    public BoxedSet() {
        // empty constructor
    }

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

    public Map<String,String> getIdMap() {
        return new HashMap<>(getSourceDbIdMap());
    }
    
    private Map<String, String> getSourceDbIdMap() {
        return sourceDbIdMap;
    }

    private void setSourceDbIdMap(Map<String, String> sourceDbIdMap) {
        this.sourceDbIdMap = sourceDbIdMap;
    }
    
    public String getSourceDbId(String sourceDb) {
        return getSourceDbIdMap().get(sourceDb);
    }
    
    public boolean setSourceDbId(String sourceDb, String id) {
        if (StringUtils.isBlank(id) || StringUtils.isBlank(sourceDb)) {
            return false;
        }
        String newId = id.trim();
        String oldId = getSourceDbIdMap().put(sourceDb, newId);
        return !StringUtils.equals(oldId, newId);
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
        BoxedSet other = (BoxedSet) obj;
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

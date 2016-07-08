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
import javax.persistence.*;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.NaturalId;
import org.yamj.core.api.model.dto.ApiGenreDTO;

@NamedQueries({
    @NamedQuery(name = Genre.UPDATE_TARGET_XML_CLEAN,
        query = "UPDATE Genre SET targetXml = null WHERE targetXml is not null AND lower(name) not in (:subGenres)"
    ),
    @NamedQuery(name = Genre.UPDATE_TARGET_XML_SET,
        query = "UPDATE Genre SET targetXml=:targetXml WHERE lower(name)=:subGenre"
    )
})

@NamedNativeQueries({    
    @NamedNativeQuery(name = Genre.QUERY_FILENAME, resultSetMapping = "metadata.genre.full",
        query = "SELECT g.id, g.name,"+
                "CASE WHEN target_api is not null THEN target_api WHEN target_xml is not null THEN target_xml ELSE name END as target "+
                "FROM mediafile m, mediafile_videodata mv, videodata v, videodata_genres vg, genre g "+
                "WHERE m.id=mv.mediafile_id AND mv.videodata_id=v.id AND v.id = vg.data_id AND vg.genre_id=g.id AND lower(m.file_name)=:filename"
    ),
    @NamedNativeQuery(name = Genre.DELETE_ORPHANS,
        query = "DELETE FROM genre WHERE not exists (select 1 from videodata_genres vg where vg.genre_id=id) "+
                "AND not exists (select 1 from series_genres sg where sg.genre_id=id) "
    ),
    @NamedNativeQuery(name = "metadata.genre.series", resultSetMapping = "metadata.genre.target",
        query = "SELECT DISTINCT CASE WHEN target_api is not null THEN target_api WHEN target_xml is not null THEN target_xml ELSE name END as target "+
                "FROM series_genres sg, genre g WHERE sg.series_id=:id AND sg.genre_id=g.id ORDER BY target"
    ),
    @NamedNativeQuery(name = "metadata.genre.season", resultSetMapping = "metadata.genre.target",
        query = "SELECT DISTINCT CASE WHEN target_api is not null THEN target_api WHEN target_xml is not null THEN target_xml ELSE name END as target "+
                "FROM season sea, series_genres sg, genre g WHERE sea.id=:id AND sg.series_id=sea.series_id AND sg.genre_id=g.id ORDER BY target"
    ),
    @NamedNativeQuery(name = "metadata.genre.movie", resultSetMapping = "metadata.genre.target",
        query = "SELECT DISTINCT CASE WHEN target_api is not null THEN target_api WHEN target_xml is not null THEN target_xml ELSE name END as target "+
                "FROM videodata_genres vg, genre g WHERE vg.data_id=:id AND vg.genre_id=g.id ORDER BY target"
    )    
})

@SqlResultSetMappings({
    @SqlResultSetMapping(name="metadata.genre.target", classes={
        @ConstructorResult(
            targetClass=ApiGenreDTO.class,
            columns={
                 @ColumnResult(name="target", type=String.class),
            }
        )}
    ),
    @SqlResultSetMapping(name="metadata.genre.full", classes={
        @ConstructorResult(
            targetClass=ApiGenreDTO.class,
            columns={
                 @ColumnResult(name="id", type=Long.class),
                 @ColumnResult(name="name", type=String.class),
                 @ColumnResult(name="target", type=String.class),
            }
        )}
    )
})


@Entity
@Table(name = "genre",
        uniqueConstraints = @UniqueConstraint(name = "UIX_GENRE_NATURALID", columnNames = {"name"})
)
public class Genre extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = -5113519542293276527L;
    public static final String QUERY_FILENAME = "genre.filename";
    public static final String UPDATE_TARGET_XML_CLEAN = "genre.targetXml.clean";
    public static final String UPDATE_TARGET_XML_SET = "genre.targetXml.set";
    public static final String DELETE_ORPHANS = "genre.deleteOrphans";
    
    @NaturalId(mutable = true)
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "target_api", length = 100)
    private String targetApi;

    @Column(name = "target_xml", length = 100)
    private String targetXml;

    public Genre() {
        // empty constructor
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

    public String getTargetApi() {
        return targetApi;
    }

    public void setTargetApi(String targetApi) {
        this.targetApi = targetApi;
    }

    public String getTargetXml() {
        return targetXml;
    }

    public void setTargetXml(String targetXml) {
        this.targetXml = targetXml;
    }

    // EQUALITY CHECKS
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getName())
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Genre)) {
            return false;
        }
        Genre other = (Genre) obj;
        // first check the id
        if ((getId() > 0) && (other.getId() > 0)) {
            return getId() == other.getId();
        }
        // check other values        
        return new EqualsBuilder()
                .append(getName(), other.getName())
                .isEquals();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Genre [ID=");
        sb.append(getId());
        sb.append(", name=");
        sb.append(getName());
        if (getTargetApi() != null) {
            sb.append(", targetApi=");
            sb.append(getTargetApi());
        }
        if (getTargetXml() != null) {
            sb.append(", targetXml=");
            sb.append(getTargetXml());
        }
        sb.append("]");
        return sb.toString();
    }
}

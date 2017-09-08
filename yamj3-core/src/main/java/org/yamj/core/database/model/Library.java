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
import java.util.Date;
import javax.persistence.*;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@NamedNativeQueries({    
    @NamedNativeQuery(name = "metadata.library.series", resultClass = Library.class,
        query = "SELECT l.id, l.base_directory as baseDirectory FROM library l  ORDER BY base_directory"
    ),
    @NamedNativeQuery(name = "metadata.library.season", resultClass = Library.class,
        query = "SELECT l.id, l.base_directory as baseDirectory FROM library l  ORDER BY base_directory"
    ),
    @NamedNativeQuery(name = "metadata.library.movie", resultClass = Library.class,
        query = "SELECT l.id, l.base_directory as baseDirectory FROM library l ORDER BY base_directory"
    )    
})

@Entity
@Table(name = "library",
        uniqueConstraints = @UniqueConstraint(name = "UIX_LIBRARY_NATURALID", columnNames = {"base_directory"})
)
public class Library extends AbstractIdentifiable implements Serializable {

 	private static final long serialVersionUID = -3086992329257871600L;
	
    @Column(name = "client", nullable = false, length = 100)
    private String client;

    @Column(name = "player_path", nullable = false, length = 1000)
    private String playerPath;
	
	
    @Column(name = "base_directory", nullable = false, length = 1000)
    private String baseDirectory;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_scanned", nullable = false)
    private Date lastScanned;
	
    // GETTER and SETTER 
    
   public String getName() {
       return baseDirectory;
	}

    public void setName(String baseDirectory) {
      this.baseDirectory = baseDirectory;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getPlayerPath() {
        return playerPath;
    }

    public void setPlayerPath(String playerPath) {
        this.playerPath = playerPath;
    }

    public String getBaseDirectory() {
        return baseDirectory;
    }

    public void setBaseDirectory(String baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    public Date getLastScanned() {
        return lastScanned;
    }

    public void setLastScanned(Date lastScanned) {
        this.lastScanned = lastScanned;
    }
	
    // EQUALITY CHECKS
  
	@Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getClient())
                .append(getPlayerPath())
				.append(getBaseDirectory())
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
        if (!(obj instanceof Library)) {
            return false;
        }
	   Library other = (Library) obj;
        // first check the id
        if ((getId() > 0) && (other.getId() > 0)) {
            return getId() == other.getId();
        }
       
			return new EqualsBuilder()
                    .append(getClient(), other.getClient())
                    .append(getPlayerPath(), other.getPlayerPath())
					.append(getBaseDirectory(), other.getBaseDirectory())
                    .isEquals();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Library [ID=");
        sb.append(getId());
        sb.append(", base_directory=");
        sb.append(getBaseDirectory());
		sb.append(", player_path=");
        sb.append(getPlayerPath());
		sb.append(", client=");
        sb.append(getClient());
		sb.append(", last_scanned=");
        sb.append(getLastScanned());
        sb.append("]");
        return sb.toString();
    }
}

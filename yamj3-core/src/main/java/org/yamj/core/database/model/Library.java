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
import org.hibernate.annotations.NaturalId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;





@NamedNativeQueries({    
//    @NamedNativeQuery(name = Library.DELETE_ORPHANS,
  //      query = "DELETE FROM library WHERE not exists (select 1 from videodata_libraries vl where vl.library_id=id) "+
   //             "AND not exists (select 1 from series_libraries sl where sl.library_id=id)"
    //),
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

    private static final Logger LOG = LoggerFactory.getLogger(Library.class);
 //   private static final long serialVersionUID = -5113519542293276527L;
  //  public static final String DELETE_ORPHANS = "library.deleteOrphans";
    
    
//    @Column(name = "base_directory", nullable = false, length = 200)
//   private String name;

	// add library extends
	private static final long serialVersionUID = -3086992329257871600L;
	@NaturalId(mutable = true)
    @Column(name = "client", nullable = false, length = 100)
    private String client;

    @Column(name = "player_path", nullable = false, length = 1000)
    private String playerPath;
	
	
    @Column(name = "base_directory", nullable = false, length = 1000)
    private String baseDirectory;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_scanned", nullable = false)
    private Date lastScanned;
	// end library
	
	
	
	
    // GETTER and SETTER to be compatible with the studio duplication
    
   public String getName() {
	// LOG.debug("Library.java getName() baseDirectory : " + baseDirectory);
       return baseDirectory;
	}

    public void setName(String baseDirectory) {
	/// LOG.debug("Library.java setName() baseDirectory : " + baseDirectory);
      this.baseDirectory = baseDirectory;
    }
	// GETTER and SETTER
    public String getClient() {
	// LOG.debug("Library.java getClient() client : " + client);
        return client;
    }

    public void setClient(String client) {
	// LOG.debug("Library.java setClient() client : " + client);
        this.client = client;
    }

    public String getPlayerPath() {
	// LOG.debug("Library.java getPlayerPath() playerPath : " + playerPath);
        return playerPath;
    }

    public void setPlayerPath(String playerPath) {
	// LOG.debug("Library.java setPlayerPath() playerPath : " + playerPath);
        this.playerPath = playerPath;
    }

    public String getBaseDirectory() {
	// LOG.debug("Library.java getBaseDirectory() baseDirectory : " + baseDirectory);
        return baseDirectory;
    }

    public void setBaseDirectory(String baseDirectory) {
	// LOG.debug("Library.java setBaseDirectory() baseDirectory : " + baseDirectory);
        this.baseDirectory = baseDirectory;
    }

    public Date getLastScanned() {
	// LOG.debug("Library.java getLastScanned() lastScanned : " + lastScanned);
        return lastScanned;
    }

    public void setLastScanned(Date lastScanned) {
	// LOG.debug("Library.java setLastScanned() lastScanned : " + lastScanned);
        this.lastScanned = lastScanned;
    }
	//end library
	
    // EQUALITY CHECKS
    
  
	@Override
    public int hashCode() {
	//	LOG.debug ("Library HashCodeBuilder");
        return new HashCodeBuilder()
                .append(getClient())
                .append(getPlayerPath())
				.append(getBaseDirectory())
                .toHashCode();
    }
	
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
		// 	LOG.debug ("Library egals this == obj return true");
            return true;
        }
        if (obj == null) {
		//	LOG.debug ("Library egals obj == null return false");
            return false;
        }
        if (!(obj instanceof Library)) {
		//	LOG.debug ("Library !instanceof library return false");
            return false;
        }
	   Library other = (Library) obj;
        // first check the id
		// 	LOG.debug ("Library other getId() : " + getId() + " other.getId() : " + other.getId());
        if ((getId() > 0) && (other.getId() > 0)) {
            return getId() == other.getId();
        }
       
		// 	LOG.debug ("Library  return new EqualsBuilder ()");
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

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
package org.yamj.core.database.model.award;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.yamj.core.database.model.VideoData;

@Entity
@Table(name = "videodata_awards")
public class MovieAward implements Serializable {

    private static final long serialVersionUID = -6333705870878639167L;

    @EmbeddedId
    private MovieAwardPK movieAwardPK;
    
    @Column(name =  "won", nullable = false)
    private boolean won = false;

    @Column(name =  "nominated", nullable = false)
    private boolean nominated = false;

    public MovieAward() {
    }

    public MovieAward(VideoData videoData, Award award, int year) {
      setMovieAwardPK(new MovieAwardPK(videoData, award, year));
    }

    // GETTER and SETTER
    public MovieAwardPK getMovieAwardPK() {
        return movieAwardPK;
    }

    public void setMovieAwardPK(MovieAwardPK movieAwardPK) {
        this.movieAwardPK = movieAwardPK;
    }

    public boolean isWon() {
        return won;
    }

    public void setWon(boolean won) {
        this.won = won;
    }

    public boolean isNominated() {
        return nominated;
    }

    public void setNominated(boolean nominated) {
        this.nominated = nominated;
    }

    // EQUALITY CHECKS
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getMovieAwardPK())
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MovieAward) {
            final MovieAward other = (MovieAward) obj;
            return new EqualsBuilder()
                    .append(getMovieAwardPK(), other.getMovieAwardPK())
                    .isEquals();
        }
        return false;
    }
}

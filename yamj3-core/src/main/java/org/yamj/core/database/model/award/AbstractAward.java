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
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractAward implements Serializable {

    private static final long serialVersionUID = 5240208540257538001L;

    @Column(name =  "won", nullable = false)
    private boolean won = false;

    @Column(name =  "nominated", nullable = false)
    private boolean nominated = false;
    
    public AbstractAward() {
        super();
    }

    public final boolean isWon() {
        return won;
    }

    public final void setWon(boolean won) {
        this.won = won;
    }

    public final boolean isNominated() {
        return nominated;
    }

    public final void setNominated(boolean nominated) {
        this.nominated = nominated;
    }
}

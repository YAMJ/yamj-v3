/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
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
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.database.model.dto;

import java.io.Serializable;
import java.util.Comparator;

public class QueueDTOComparator implements Comparator<QueueDTO>, Serializable {

    private static final long serialVersionUID = 3538761237411750316L;

    @Override
    public int compare(QueueDTO o1, QueueDTO o2) {
        if (o1.getDate() == null && o2.getDate() == null) {
            return 0;
        }
        if (o1.getDate() != null && o2.getDate() == null) {
            return 1;
        }
        if (o1.getDate() == null && o2.getDate() != null) {
            return -11;
        }
        return o1.getDate().compareTo(o2.getDate());
    }
}

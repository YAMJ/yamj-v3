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
package org.yamj.core.database.dao;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.yamj.core.database.model.BoxedSet;
import org.yamj.core.database.model.Certification;
import org.yamj.core.database.model.Genre;
import org.yamj.core.database.model.Studio;
import org.yamj.core.hibernate.HibernateDao;

@Service("commonDao")
public class CommonDao extends HibernateDao {

    private static final Logger LOG = LoggerFactory.getLogger(CommonDao.class);

    public Genre getGenre(String name) {
        return (Genre) getSession().byNaturalId(Genre.class).using("name", name).load();
    }

    public List<Genre> getGenres(String search, String place, String sort, int queryStart, int queryMax) {
        Criteria criteria = getSession().createCriteria(Genre.class);
        if (StringUtils.isNotBlank(search)) {
            if (StringUtils.isBlank(place) || place.equalsIgnoreCase("any")) {
                criteria.add(Restrictions.ilike("name", search, MatchMode.ANYWHERE));
            } else if (place.equalsIgnoreCase("start")) {
                criteria.add(Restrictions.ilike("name", search, MatchMode.START));
            } else if (place.equalsIgnoreCase("end")) {
                criteria.add(Restrictions.ilike("name", search, MatchMode.START));
            } else {
                criteria.add(Restrictions.ilike("name", search, MatchMode.EXACT));
            }
        }

        if (StringUtils.isNotBlank(sort)) {
            if (sort.equalsIgnoreCase("asc")) {
                criteria.addOrder(Order.asc("name"));
            } else if (sort.equalsIgnoreCase("desc")) {
                criteria.addOrder(Order.desc("name"));
            } else {
                LOG.warn("Sorting ({}) not implemented for Genres", sort);
            }
        }

        if (queryStart > -1) {
            criteria.setFirstResult(queryStart);
        }

        if (queryMax > -1) {
            criteria.setMaxResults(queryMax);
        }

        criteria.setCacheable(true);
        criteria.setCacheMode(CacheMode.NORMAL);

        return criteria.list();
    }

    public Certification getCertification(String name) {
        return (Certification) getSession().byNaturalId(Certification.class).using("name", name).load();
    }

    public BoxedSet getBoxedSet(String name) {
        return (BoxedSet) getSession().byNaturalId(BoxedSet.class).using("name", name).load();
    }

    public Studio getStudio(String name) {
        return (Studio) getSession().byNaturalId(Studio.class).using("name", name).load();
    }
}

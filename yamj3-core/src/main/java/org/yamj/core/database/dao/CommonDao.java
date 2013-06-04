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

import org.yamj.core.database.model.BoxedSet;
import org.yamj.core.database.model.Certification;
import org.yamj.core.database.model.Genre;
import org.yamj.core.database.model.Studio;
import org.yamj.core.hibernate.ExtendedHibernateDaoSupport;
import java.sql.SQLException;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.stereotype.Service;

@Service("commonDao")
public class CommonDao extends ExtendedHibernateDaoSupport {

    @SuppressWarnings("rawtypes")
    public List getObjectsById(CharSequence query, long id) {
        return getHibernateTemplate().find(query.toString(), id);
    }

    public Genre getGenre(final String name) {
        return this.getHibernateTemplate().executeWithNativeSession(new HibernateCallback<Genre>() {
            @Override
            public Genre doInHibernate(Session session) throws HibernateException, SQLException {
                Criteria criteria = session.createCriteria(Genre.class);
                criteria.add(Restrictions.naturalId().set("name", name));
                criteria.setCacheable(true);
                criteria.setCacheMode(CacheMode.NORMAL);
                return (Genre) criteria.uniqueResult();
            }
        });
    }

    public Certification getCertification(final String name) {
        return this.getHibernateTemplate().executeWithNativeSession(new HibernateCallback<Certification>() {
            @Override
            public Certification doInHibernate(Session session) throws HibernateException, SQLException {
                Criteria criteria = session.createCriteria(Certification.class);
                criteria.add(Restrictions.naturalId().set("name", name));
                criteria.setCacheable(true);
                criteria.setCacheMode(CacheMode.NORMAL);
                return (Certification) criteria.uniqueResult();
            }
        });
    }

    public BoxedSet getBoxedSet(final String name) {
        return this.getHibernateTemplate().executeWithNativeSession(new HibernateCallback<BoxedSet>() {
            @Override
            public BoxedSet doInHibernate(Session session) throws HibernateException, SQLException {
                Criteria criteria = session.createCriteria(BoxedSet.class);
                criteria.add(Restrictions.naturalId().set("name", name));
                criteria.setCacheable(true);
                criteria.setCacheMode(CacheMode.NORMAL);
                return (BoxedSet) criteria.uniqueResult();
            }
        });
    }

    public Studio getStudio(final String name) {
        return this.getHibernateTemplate().executeWithNativeSession(new HibernateCallback<Studio>() {
            @Override
            public Studio doInHibernate(Session session) throws HibernateException, SQLException {
                Criteria criteria = session.createCriteria(Studio.class);
                criteria.add(Restrictions.naturalId().set("name", name));
                criteria.setCacheable(true);
                criteria.setCacheMode(CacheMode.NORMAL);
                return (Studio) criteria.uniqueResult();
            }
        });
    }
}

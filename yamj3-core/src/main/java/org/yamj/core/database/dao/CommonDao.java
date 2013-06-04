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

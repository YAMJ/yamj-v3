package com.moviejukebox.core.database.dao;

import java.sql.SQLException;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.stereotype.Service;

import com.moviejukebox.core.database.model.Certification;
import com.moviejukebox.core.database.model.Genre;
import com.moviejukebox.core.database.model.SetDescriptor;
import com.moviejukebox.core.hibernate.ExtendedHibernateDaoSupport;

@Service("commonDao")
public class CommonDao extends ExtendedHibernateDaoSupport {

    public Genre getGenre(final String name) {
        return this.getHibernateTemplate().executeWithNativeSession(new HibernateCallback<Genre>() {
            @Override
            public Genre doInHibernate(Session session) throws HibernateException, SQLException {
                Criteria criteria = session.createCriteria(Genre.class);
                criteria.add(Restrictions.naturalId().set("name", name));
                criteria.setCacheable(true);
                criteria.setCacheMode(CacheMode.NORMAL);
                return (Genre)criteria.uniqueResult();
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
                return (Certification)criteria.uniqueResult();
            }
        });
    }

    public SetDescriptor getSetDescriptor(final String name) {
        return this.getHibernateTemplate().executeWithNativeSession(new HibernateCallback<SetDescriptor>() {
            @Override
            public SetDescriptor doInHibernate(Session session) throws HibernateException, SQLException {
                Criteria criteria = session.createCriteria(SetDescriptor.class);
                criteria.add(Restrictions.naturalId().set("name", name));
                criteria.setCacheable(true);
                criteria.setCacheMode(CacheMode.NORMAL);
                return (SetDescriptor)criteria.uniqueResult();
            }
        });
    }
}

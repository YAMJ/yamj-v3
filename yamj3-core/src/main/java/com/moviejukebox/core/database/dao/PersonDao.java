package com.moviejukebox.core.database.dao;

import com.moviejukebox.core.database.model.Person;
import com.moviejukebox.core.hibernate.ExtendedHibernateDaoSupport;
import java.sql.SQLException;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.stereotype.Service;

@Service("personDao")
public class PersonDao extends ExtendedHibernateDaoSupport {

    public Person getPerson(final String name) {
        return this.getHibernateTemplate().executeWithNativeSession(new HibernateCallback<Person>() {
            @Override
            public Person doInHibernate(Session session) throws HibernateException, SQLException {
                Criteria criteria = session.createCriteria(Person.class);
                criteria.add(Restrictions.naturalId().set("name", name));
                criteria.setCacheable(true);
                criteria.setCacheMode(CacheMode.NORMAL);
                return (Person) criteria.uniqueResult();
            }
        });
    }
}

package org.yamj.core.database.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.*;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.stereotype.Service;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.Person;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.model.dto.QueueDTOComparator;
import org.yamj.core.database.model.type.MetaDataType;
import org.yamj.core.hibernate.ExtendedHibernateDaoSupport;

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

    public Person getPerson(final long id) {
        return this.getHibernateTemplate().get(Person.class, id);
    }

    public List<QueueDTO> getPersonQueueForScanning(final int maxResults) {
        final StringBuilder sql = new StringBuilder();
        sql.append("select id, '");
        sql.append(MetaDataType.PERSON);
        sql.append("' as mediatype, create_timestamp, update_timestamp ");
        sql.append("from person ");
        sql.append("where status in ('");
        sql.append(StatusType.NEW);
        sql.append("','");
        sql.append(StatusType.UPDATED);
        sql.append("') ");

        return this.getHibernateTemplate().executeWithNativeSession(new HibernateCallback<List<QueueDTO>>() {
            @Override
            @SuppressWarnings("unchecked")
            public List<QueueDTO> doInHibernate(Session session) throws HibernateException, SQLException {
                SQLQuery query = session.createSQLQuery(sql.toString());
                query.setReadOnly(true);
                query.setCacheable(true);
                if (maxResults > 0) {
                    query.setMaxResults(maxResults);
                }

                List<QueueDTO> queueElements = new ArrayList<QueueDTO>();
                List<Object[]> objects = query.list();
                for (Object[] object : objects) {
                    QueueDTO queueElement = new QueueDTO();
                    queueElement.setId(convertRowElementToLong(object[0]));
                    queueElement.setMetadataType(convertRowElementToString(object[1]));
                    queueElement.setDate(convertRowElementToDate(object[3]));
                    if (queueElement.getDate() == null) {
                        queueElement.setDate(convertRowElementToDate(object[2]));
                    }
                    queueElements.add(queueElement);
                }

                Collections.sort(queueElements, new QueueDTOComparator());
                return queueElements;
            }
        });
    }
}

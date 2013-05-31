package org.yamj.core.hibernate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * Extended hibernate DAO support.
 */
public abstract class ExtendedHibernateDaoSupport extends HibernateDaoSupport implements ExtendedHibernateDao {

    @Autowired
    public void setExtendedSessionFactory(SessionFactory sessionFactory) {
        super.setSessionFactory(sessionFactory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ExtendedHibernateTemplate createHibernateTemplate(SessionFactory sessionFactory) {
        return new ExtendedHibernateTemplate(sessionFactory);
    }

    /**
     * Get the extended hibernate template.
     *
     * @return the hibernate template
     */
    public final ExtendedHibernateTemplate getExtendedHibernateTemplate() {
        return (ExtendedHibernateTemplate) this.getHibernateTemplate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void storeEntity(final Object entity) {
        this.getHibernateTemplate().saveOrUpdate(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("rawtypes")
    public void storeAll(final Collection entities) {
        this.getHibernateTemplate().saveOrUpdateAll(entities);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void saveEntity(final Object object) {
        this.getHibernateTemplate().save(object);
    }

    /**
     * {@inheritDoc}
     */
    public final void saveOrUpdate(final Object object) {
        this.getHibernateTemplate().saveOrUpdate(object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void updateEntity(final Object entity) {
        this.getHibernateTemplate().update(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void deleteEntity(final Object entity) {
        this.getHibernateTemplate().delete(entity);
    }

    /**
     * Convert row object to a string.
     *
     * @param rowElement
     * @return <code>String</code>
     */
    protected String convertRowElementToString(Object rowElement) {
        if (rowElement == null) {
            return null;
        } else if (rowElement instanceof String) {
            return (String) rowElement;
        } else {
            return rowElement.toString();
        }
    }

    /**
     * Convert row object to Integer.
     *
     * @param rowElement
     * @return <code>Integer</code>
     */
    protected Integer convertRowElementToInteger(Object rowElement) {
        if (rowElement == null) {
            return Integer.valueOf(0);
        } else if (StringUtils.isNumeric(rowElement.toString())) {
            return Integer.valueOf(rowElement.toString());
        } else {
            return Integer.valueOf(0);
        }
    }

    /**
     * Convert row object to Long.
     *
     * @param rowElement
     * @return <code>Long</code>
     */
    protected Long convertRowElementToLong(Object rowElement) {
        if (rowElement == null) {
            return Long.valueOf(0);
        } else if (StringUtils.isNumeric(rowElement.toString())) {
            return Long.valueOf(rowElement.toString());
        } else {
            return Long.valueOf(0);
        }
    }

    /**
     * Convert row object to date.
     *
     * @param rowElement
     * @return
     */
    protected Date convertRowElementToDate(Object rowElement) {
        if (rowElement == null) {
            return null;
        } else if (rowElement instanceof Date) {
            return (Date) rowElement;
        } else {
            // TODO invalid date
            return null;
        }
    }

    /**
     * Convert row object to date.
     *
     * @param rowElement
     * @return
     */
    protected Timestamp convertRowElementToTimestamp(Object rowElement) {
        if (rowElement == null) {
            return null;
        } else if (rowElement instanceof Timestamp) {
            return (Timestamp) rowElement;
        } else {
            // TODO invalid ttimestamp
            return null;
        }
    }

    /**
     * Convert row object to big decimal.
     *
     * @param rowElement
     * @return <code>BigDecimal</code>
     */
    protected BigDecimal convertRowElementToBigDecimal(Object rowElement) {
        if (rowElement == null) {
            return BigDecimal.ZERO;
        } else if (rowElement instanceof BigDecimal) {
            return (BigDecimal) rowElement;
        } else {
            return new BigDecimal(rowElement.toString());
        }
    }
}
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
package org.yamj.core.hibernate;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.*;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.*;
import org.hibernate.transform.Transformers;
import org.springframework.beans.factory.annotation.Autowired;
import org.yamj.core.api.model.builder.SqlScalars;
import org.yamj.core.api.options.IOptions;
import org.yamj.core.api.wrapper.IApiWrapper;

/**
 * Hibernate DAO implementation
 */
public abstract class HibernateDao {

    public static final String IDENTIFIER = "identifier";

    @Autowired
    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public Session currentSession() {
        return sessionFactory.getCurrentSession();
    }

    /**
     * Flush and clear the session.
     */
    public void flushAndClear() {
        currentSession().flush();
        currentSession().clear();
    }

    /**
     * Store an entity.
     *
     * @param entity the entity to store
     */
    public void storeEntity(final Object entity) {
        currentSession().saveOrUpdate(entity);
    }

    /**
     * Merge an entity.
     *
     * @param entity the entity to merge
     */
    public void mergeEntity(final Object entity) {
        currentSession().merge(entity);
    }

    /**
     * Store all entities.
     *
     * @param entities the entities to store
     */
    @SuppressWarnings("rawtypes")
    public void storeAll(final Collection entities) {
        if (entities != null && !entities.isEmpty()) {
            for (Object entity : entities) {
                currentSession().saveOrUpdate(entity);
            }
        }
    }

    /**
     * Delete all entities.
     *
     * @param entities the entities to delete
     */
    @SuppressWarnings("rawtypes")
    public void deleteAll(final Collection entities) {
        if (entities != null && !entities.isEmpty()) {
            for (Object entity : entities) {
                currentSession().delete(entity);
            }
        }
    }

    /**
     * Save an entity.
     *
     * @param entity the entity to save
     */
    public void saveEntity(final Object entity) {
        currentSession().save(entity);
    }

    /**
     * Update an entity.
     *
     * @param entity the entity to update
     */
    public void updateEntity(final Object entity) {
        currentSession().update(entity);
    }

    /**
     * Delete an entity.
     *
     * @param entity the entity to delete
     */
    public void deleteEntity(final Object entity) {
        currentSession().delete(entity);
    }

    /**
     * Get a single object by its id
     *
     * @param entityClass
     * @param id
     * @return
     */
    public <T> T getById(Class<T> entityClass, Serializable id) {
        return currentSession().get(entityClass, id);
    }

    /**
     * Get a single object by the passed field using the name case sensitive.
     *
     * @param entityClass
     * @param field
     * @param name
     * @return
     */
    public <T> T getByNaturalId(Class<T> entityClass, String field, String name) {
        return currentSession().byNaturalId(entityClass).using(field, name).load();
    }

    /**
     * Get a single object by the passed field using the name case insensitive.
     *
     * @param entityClass
     * @param field
     * @param name
     * @return
     */
    public <T> T getByNaturalIdCaseInsensitive(Class<T> entityClass, String field, String name) {
        StringBuilder sb = new StringBuilder();
        sb.append("from ");
        sb.append(entityClass.getSimpleName());
        sb.append(" where lower(").append(field).append(") = :name) { ");

        Map<String, Object> params = Collections.singletonMap("name", (Object) name.toLowerCase());
        return this.findUniqueByNamedParameters(entityClass, sb, params);
    }

    /**
     * Convert row object to a string.
     *
     * @param rowElement
     * @return <code>String</code>
     */
    protected static String convertRowElementToString(Object rowElement) {
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
    protected static Integer convertRowElementToInteger(Object rowElement) {
        if (rowElement == null) {
            return 0;
        } else if (rowElement instanceof Integer) {
            return (Integer) rowElement;
        } else if (rowElement instanceof BigInteger) {
            return ((BigInteger) rowElement).intValue();
        } else if (rowElement instanceof Long) {
            return ((Long) rowElement).intValue();
        } else if (StringUtils.isNumeric(rowElement.toString())) {
            return Integer.valueOf(rowElement.toString());
        } else {
            return 0;
        }
    }

    /**
     * Convert row object to Long.
     *
     * @param rowElement
     * @return <code>Long</code>
     */
    protected static Long convertRowElementToLong(Object rowElement) {
        if (rowElement == null) {
            return 0L;
        } else if (rowElement instanceof BigInteger) {
            return ((BigInteger) rowElement).longValue();
        } else if (rowElement instanceof Long) {
            return (Long) rowElement;
        } else if (rowElement instanceof Integer) {
            return ((Integer) rowElement).longValue();
        } else if (StringUtils.isNumeric(rowElement.toString())) {
            return Long.valueOf(rowElement.toString());
        } else {
            return 0L;
        }
    }

    /**
     * Convert row object to date.
     *
     * @param rowElement
     * @return
     */
    protected static Date convertRowElementToDate(Object rowElement) {
        if (rowElement == null) {
            return null;
        } else if (rowElement instanceof Date) {
            return (Date) rowElement;
        } else if (rowElement instanceof Timestamp) {
            return (Date) rowElement;
        }
        return null;
    }

    /**
     * Convert row object to date.
     *
     * @param rowElement
     * @return
     */
    protected static Timestamp convertRowElementToTimestamp(Object rowElement) {
        if (rowElement == null) {
            return null;
        } else if (rowElement instanceof Timestamp) {
            return (Timestamp) rowElement;
        } else if (rowElement instanceof Date) {
            return new Timestamp(((Date)rowElement).getTime());
        }
        return null;
    }

    /**
     * Convert row object to big decimal.
     *
     * @param rowElement
     * @return <code>BigDecimal</code>
     */
    protected static BigDecimal convertRowElementToBigDecimal(Object rowElement) {
        if (rowElement == null) {
            return BigDecimal.ZERO;
        } else if (rowElement instanceof BigDecimal) {
            return (BigDecimal) rowElement;
        } else {
            return new BigDecimal(rowElement.toString());
        }
    }

    @SuppressWarnings("rawtypes")
    protected static void applyNamedParameterToQuery(Query queryObject, String paramName, Object value) throws HibernateException {
        if (value instanceof Collection) {
            queryObject.setParameterList(paramName, (Collection) value);
        } else if (value instanceof Object[]) {
            queryObject.setParameterList(paramName, (Object[]) value);
        } else if (value instanceof String) {
            queryObject.setString(paramName, (String) value);
        } else {
            queryObject.setParameter(paramName, value);
        }
    }

    /**
     * Find entries-
     *
     * @param queryString the query string
     * @return list of entities
     */
    @SuppressWarnings("rawtypes")
    public List find(CharSequence queryString) {
        Query queryObject = currentSession().createQuery(queryString.toString());
        queryObject.setCacheable(true);
        return queryObject.list();
    }

    /**
     * Find entries by id.
     *
     * @param queryString the query string
     * @param id the id
     * @return list of entities
     */
    @SuppressWarnings("rawtypes")
    public List findById(CharSequence queryString, Long id) {
        Query queryObject = currentSession().createQuery(queryString.toString());
        queryObject.setCacheable(true);
        queryObject.setParameter("id", id);
        return queryObject.list();
    }

    /**
     * Find list of entities by named parameters.
     *
     * @param queryCharSequence the query string
     * @param params the named parameters
     * @return list of entities
     */
    @SuppressWarnings("unused")
    public <T> List<T> findByNamedParameters(Class<T> entityClass, CharSequence queryCharSequence, Map<String, Object> params) { //NOSONAR
        Query query = currentSession().createQuery(queryCharSequence.toString());
        query.setCacheable(true);
        for (Entry<String, Object> param : params.entrySet()) {
            applyNamedParameterToQuery(query, param.getKey(), param.getValue());
        }
        return query.list();
    }

    /**
     * Find unique entity by named parameters.
     *
     * @param entityClass the entity class
     * @param queryCharSequence the query string
     * @param params the named parameters
     * @return list of entities
     */
    @SuppressWarnings("unchecked")
    public <T> T findUniqueByNamedParameters(Class<T> entityClass, CharSequence queryCharSequence, Map<String, Object> params) { //NOSONAR
        Query query = currentSession().createQuery(queryCharSequence.toString());
        query.setCacheable(true);
        for (Entry<String, Object> param : params.entrySet()) {
            applyNamedParameterToQuery(query, param.getKey(), param.getValue());
        }
        return (T)query.uniqueResult();
    }

    /**
     * Execute an update statement.
     *
     * @param queryCharSequence the query string
     * @return number of affected rows
     */
    public int executeUpdate(CharSequence queryCharSequence) {
        Query query = currentSession().createQuery(queryCharSequence.toString());
        query.setCacheable(true);
        return query.executeUpdate();
    }

    /**
     * Execute an update statement.
     *
     * @param queryCharSequence the query string
     * @param params the named parameters
     * @return number of affected rows
     */
    public int executeUpdate(CharSequence queryCharSequence, Map<String, Object> params) {
        Query query = currentSession().createQuery(queryCharSequence.toString());
        query.setCacheable(true);
        for (Entry<String, Object> param : params.entrySet()) {
            applyNamedParameterToQuery(query, param.getKey(), param.getValue());
        }
        return query.executeUpdate();
    }

    /**
     * Execute a SQL update statement.
     *
     * @param queryCharSequence the query string
     * @return number of affected rows
     */
    public int executeSqlUpdate(CharSequence queryCharSequence) {
        SQLQuery query = currentSession().createSQLQuery(queryCharSequence.toString());
        query.setCacheable(true);
        return query.executeUpdate();
    }

    /**
     * Execute an update statement.
     *
     * @param queryCharSequence the query string
     * @param params the named parameters
     * @return number of affected rows
     */
    public int executeSqlUpdate(CharSequence queryCharSequence, Map<String, Object> params) {
        SQLQuery query = currentSession().createSQLQuery(queryCharSequence.toString());
        query.setCacheable(true);
        for (Entry<String, Object> param : params.entrySet()) {
            applyNamedParameterToQuery(query, param.getKey(), param.getValue());
        }
        return query.executeUpdate();
    }

    /**
     * Execute a query return the results.
     *
     * Puts the total count returned from the query into the wrapper
     *
     * @param sql
     * @param wrapper
     * @return
     */
    public List<Object[]> executeQuery(String sql, IApiWrapper wrapper) {
        SqlScalars ss = new SqlScalars(sql);
        return executeQueryWithTransform(Object[].class, ss, wrapper);
    }

    /**
     * Execute a query to return the results
     *
     * Gets the options from the wrapper for start and max
     *
     * Puts the total count returned from the query into the wrapper
     *
     * @param entityClass
     * @param sqlScalars
     * @param wrapper
     * @return
     */
    public <T> List<T> executeQueryWithTransform(Class<T> entityClass, SqlScalars sqlScalars, IApiWrapper wrapper) { //NOSONAR
        SQLQuery query = sqlScalars.createSqlQuery(currentSession());
        query.setReadOnly(true);
        query.setCacheable(true);

        if (entityClass.equals(String.class) || entityClass.equals(Long.class) || entityClass.equals(Integer.class)) {
            // no transformer needed
        } else if (entityClass.equals(Object[].class)) {
            // no transformer needed
        } else {
            query.setResultTransformer(Transformers.aliasToBean(entityClass));
        }

        // Add the scalars to the query
        sqlScalars.populateScalars(query);

        List<T> queryResults = query.list();

        // If the wrapper is populated, then run the query to get the maximum results
        if (wrapper != null) {
            wrapper.setTotalCount(queryResults.size());

            // If there is a start or max set, we will need to re-run the query after setting the options
            IOptions options = wrapper.getOptions();
            if (options != null && (options.getStart() > 0 || options.getMax() > 0)) {
                if (options.getStart() > 0) {
                    query.setFirstResult(options.getStart());
                }

                if (options.getMax() > 0) {
                    query.setMaxResults(options.getMax());
                }
                // This will get the trimmed list
                queryResults = query.list();
            }
        }

        return queryResults;
    }
}

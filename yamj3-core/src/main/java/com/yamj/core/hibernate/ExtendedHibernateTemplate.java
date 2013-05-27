package com.yamj.core.hibernate;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.hibernate.*;
import org.hibernate.criterion.CriteriaSpecification;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.SessionFactoryUtils;

/**
 * Extended hibernate DAO support.
 */
@SuppressWarnings("rawtypes")
public class ExtendedHibernateTemplate extends HibernateTemplate {

    /**
     * Create a new extended hibernate template.
     *
     * @see HibernateTemplate#HibernateTemplate()
     */
    public ExtendedHibernateTemplate() {
        super();
    }

    /**
     * Create a new extended hibernate template.
     *
     * @see HibernateTemplate#HibernateTemplate(SessionFactory)
     */
    public ExtendedHibernateTemplate(final SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    /**
     * Create a new extended hibernate template.
     *
     * @see HibernateTemplate#HibernateTemplate(SessionFactory, boolean)
     */
    public ExtendedHibernateTemplate(final SessionFactory sessionFactory, final boolean allowCreate) {
        super(sessionFactory, allowCreate);
    }

    /**
     * Prepare hibernate query.
     *
     * @param queryObject the query object
     */
    @Override
    public void prepareQuery(Query queryObject) {
        this.prepareQuery(queryObject, null, null, null, null);
    }

    /**
     * Prepare hibernate query.
     *
     * @param queryObject the query object
     * @param startIndex index of first result to return
     * @param maxResults number of maximal results to return
     * @param readOnly if query should be read only
     * @param removeDuplicates if duplicates should be removed
     */
    public final void prepareQuery(final Query queryObject, final Integer startIndex, final Integer maxResults,
            final Boolean readOnly, final Boolean removeDuplicates) {

        if (this.isCacheQueries()) {
            queryObject.setCacheable(true);
            if (this.getQueryCacheRegion() != null) {
                queryObject.setCacheRegion(this.getQueryCacheRegion());
            }
        }

        if (this.getFetchSize() > 0) {
            queryObject.setFetchSize(this.getFetchSize());
        }

        if ((startIndex != null) && (startIndex.intValue() > 0)) {
            queryObject.setFirstResult(startIndex);
        }

        if (maxResults != null && maxResults.intValue() > 0) {
            queryObject.setMaxResults(maxResults.intValue());
        } else if (super.getMaxResults() > 0) {
            queryObject.setMaxResults(super.getMaxResults());
        }

        if (readOnly != null && Boolean.TRUE.equals(readOnly)) {
            queryObject.setReadOnly(true);
        }

        if (removeDuplicates != null && Boolean.TRUE.equals(removeDuplicates)) {
            queryObject.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
        }

        SessionFactoryUtils.applyTransactionTimeout(queryObject, this.getSessionFactory());
    }

    /**
     * Find results with query where parameters has to be set.
     *
     * @param query the query
     * @param params the parameter values
     * @param maxResults maximal number of results
     * @return the result list
     * @throws DataAccessException if an error occurred
     */
    public final List find(CharSequence query, Object[] params, Integer maxResults) throws DataAccessException {
        return this.find(query, params, null, maxResults, false, false);
    }

    /**
     * Find results with query where parameters has to be set.
     *
     * @param query the query
     * @param params the parameter values
     * @param startIndex the start index
     * @param maxResults maximal number of results
     * @return the result list
     * @throws DataAccessException if an error occurred
     */
    public final List find(CharSequence query, Object[] params, Integer startIndex, Integer maxResults)
            throws DataAccessException {
        return this.find(query, params, startIndex, maxResults, false, false);
    }

    /**
     * Find results with query where parameters has to be set.
     *
     * @param query the query
     * @param params the parameter values
     * @param startIndex the start index
     * @param maxResults maximal number of results
     * @param readOnly if query should be done read-only
     * @return the result list
     * @throws DataAccessException if an error occurred
     */
    public final List find(CharSequence query, Object[] params, Integer startIndex, Integer maxResults, Boolean readOnly)
            throws DataAccessException {
        return this.find(query, params, startIndex, maxResults, readOnly, false);
    }

    /**
     * Find results with query where parameters has to be set.
     *
     * @param query the query
     * @param params the parameter values
     * @param startIndex the start index
     * @param maxResults maximal number of results
     * @param readOnly if query should be done read-only
     * @param removeDuplicates if duplicates should be removed
     * @return the result list
     * @throws DataAccessException if an error occurred
     */
    public final List find(final CharSequence query, final Object[] params, final Integer startIndex,
            final Integer maxResults, final Boolean readOnly, final Boolean removeDuplicates) throws DataAccessException {
        return this.executeWithNativeSession(new HibernateCallback<List>() {
            @Override
            public List doInHibernate(final Session session) throws HibernateException {

                final Query queryObject = session.createQuery(query.toString());
                ExtendedHibernateTemplate.this.prepareQuery(queryObject, startIndex, maxResults, readOnly, removeDuplicates);

                if (params != null) {
                    for (int i = 0; i < params.length; i++) {
                        queryObject.setParameter(i, params[i]);
                    }
                }

                return queryObject.list();
            }
        });
    }

    /**
     * Find objects by SQL query
     *
     * @param sqlQuery the SQL query
     * @return the result list
     * @throws DataAccessException if an exception occurred
     */
    public final List findBySQLQuery(CharSequence sqlQuery) throws DataAccessException {

        return this.findBySQLQuery(sqlQuery, (Object[]) null);
    }

    /**
     * Find objects by SQL query
     *
     * @param query the SQL query
     * @param param the parameter value
     * @return the result list
     * @throws DataAccessException if an exception occurred
     */
    public final List findBySQLQuery(CharSequence sqlQuery, Object param) throws DataAccessException {

        return this.findBySQLQuery(sqlQuery, new Object[]{param});
    }

    /**
     * Find objects by SQL query
     *
     * @param query the SQL query
     * @param params the parameter values
     * @return the result list
     * @throws DataAccessException if an exception occurred
     */
    public final List findBySQLQuery(final CharSequence sqlQuery, final Object[] params) throws DataAccessException {
        return this.executeWithNativeSession(new HibernateCallback<List>() {
            @Override
            public List doInHibernate(final Session session) throws HibernateException {

                final SQLQuery queryObject = session.createSQLQuery(sqlQuery.toString());
                ExtendedHibernateTemplate.this.prepareQuery(queryObject);

                if (params != null) {
                    for (int i = 0; i < params.length; i++) {
                        queryObject.setParameter(i, params[i]);
                    }
                }

                return queryObject.list();
            }
        });
    }

    /**
     * Find results with query where parameters has to be set.
     *
     * @param query the query
     * @param nameParams the named parameters
     * @return the result list
     * @throws DataAccessException if an error occurred
     */
    public final List findByNamedParam(final CharSequence query, final Map<String, Object> namedParams)
            throws DataAccessException {
        return this.executeWithNativeSession(new HibernateCallback<List>() {
            @Override
            public List doInHibernate(final Session session) throws HibernateException {

                final Query queryObject = session.createQuery(query.toString());
                ExtendedHibernateTemplate.this.prepareQuery(queryObject);

                for (Entry<String, Object> entry : namedParams.entrySet()) {
                    ExtendedHibernateTemplate.this.applyNamedParameterToQuery(queryObject,
                            entry.getKey(), entry.getValue());
                }

                return queryObject.list();
            }
        });
    }

    /**
     * Find results with query where parameters has to be set.
     *
     * @param query the query
     * @param nameParams the named parameters
     * @param maxResults maximal number of results
     * @return the result list
     * @throws DataAccessException if an error occurred
     */
    public final List findByNamedParam(final CharSequence query, final Map<String, Object> namedParams, final Integer maxResults)
            throws DataAccessException {
        return findByNamedParam(query, namedParams, -1, maxResults, false, false);
    }

    /**
     * Find results with query where parameters has to be set.
     *
     * @param query the query
     * @param nameParams the named parameters
     * @param startIndex the start index
     * @param maxResults maximal number of results
     * @param readOnly if query should be done read-only
     * @param removeDuplicates if duplicates should be removed
     * @return the result list
     * @throws DataAccessException if an error occurred
     */
    public final List findByNamedParam(final CharSequence query, final Map<String, Object> namedParams,
            final Integer startIndex, final Integer maxResults, final Boolean readOnly, final Boolean removeDuplicates)
            throws DataAccessException {
        return this.executeWithNativeSession(new HibernateCallback<List>() {
            @Override
            public List doInHibernate(final Session session) throws HibernateException {

                final Query queryObject = session.createQuery(query.toString());
                ExtendedHibernateTemplate.this.prepareQuery(queryObject, startIndex, maxResults, readOnly, removeDuplicates);

                for (Entry<String, Object> entry : namedParams.entrySet()) {
                    ExtendedHibernateTemplate.this.applyNamedParameterToQuery(queryObject,
                            entry.getKey(), entry.getValue());
                }

                return queryObject.list();
            }
        });
    }

    /**
     * Do update by using given query and named parameters
     *
     * @param query the query
     * @param namedParams the valueMap to set in the query
     * @return number of updated rows
     */
    public final Integer bulkUpdateByNamedParam(final CharSequence query, final Map<String, Object> namedParams) {
        return this.executeWithNativeSession(new HibernateCallback<Integer>() {
            @Override
            public Integer doInHibernate(Session session) throws HibernateException, SQLException {
                final Query queryObject = session.createQuery(query.toString());
                ExtendedHibernateTemplate.this.prepareQuery(queryObject);

                for (Entry<String, Object> entry : namedParams.entrySet()) {
                    ExtendedHibernateTemplate.this.applyNamedParameterToQuery(queryObject,
                            entry.getKey(), entry.getValue());
                }

                return Integer.valueOf(queryObject.executeUpdate());
            }
        });
    }

    /**
     * Read scrollable results.
     *
     * @param query the query
     * @param namedParams the valueMap to set in the query
     * @param maxResults the maxResults to read
     * @param readOnly the readOnly flag
     * @return
     */
    @SuppressWarnings("unchecked")
    public final List readScrollable(final CharSequence query,
            final Map<String, Object> namedParams, final long maxResults,
            final boolean readOnly) {
        return this.executeWithNativeSession(new HibernateCallback<List>() {
            @Override
            public List doInHibernate(Session session) throws HibernateException, SQLException {

                Query queryObject = session.createQuery(query.toString());
                ExtendedHibernateTemplate.this.prepareQuery(queryObject);
                queryObject.setReadOnly(readOnly);

                for (Entry<String, Object> entry : namedParams.entrySet()) {
                    ExtendedHibernateTemplate.this.applyNamedParameterToQuery(
                            queryObject, entry.getKey(), entry.getValue());
                }

                ScrollableResults results = queryObject.scroll(ScrollMode.FORWARD_ONLY);

                // read results unit maxResults has reached
                List list = new ArrayList();
                long readCounter = 0;
                while (results.next() && readCounter < maxResults) {
                    list.add(results.get(0));
                    readCounter++;
                }

                return list;
            }
        });
    }
}
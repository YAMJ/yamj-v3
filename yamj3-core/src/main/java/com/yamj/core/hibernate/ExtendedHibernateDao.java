package com.yamj.core.hibernate;

import java.util.Collection;

/**
 * Extended hibernate DAO support.
 */
public interface ExtendedHibernateDao {

    /**
     * Store an entity.
     *
     * @param entity the entity to store
     */
    void storeEntity(final Object entity);

    /**
     * Store all entities.
     *
     * @param entities the entities to store
     */
    @SuppressWarnings("rawtypes")
    void storeAll(final Collection entities);

    /**
     * Save an entity.
     *
     * @param entity the entity to save
     */
    void saveEntity(final Object object);

    /**
     * Update an entity.
     *
     * @param entity the entity to update
     */
    void updateEntity(final Object entity);

    /**
     * Delete an entity.
     *
     * @param entity the entity to delete
     */
    void deleteEntity(final Object entity);
}
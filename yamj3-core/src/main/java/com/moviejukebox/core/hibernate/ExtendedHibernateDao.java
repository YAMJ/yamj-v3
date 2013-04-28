package com.moviejukebox.core.hibernate;

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
    public void storeEntity(final Object entity);

    /**
     * Store all entities.
     *
     * @param entities the entities to store
     */
    @SuppressWarnings("rawtypes")
    public void storeAll(final Collection entities);

    /**
     * Save an entity.
     *
     * @param entity the entity to save
     */
    public void saveEntity(final Object object);

    /**
     * Update an entity.
     *
     * @param entity the entity to update
     */
    public void updateEntity(final Object entity);

    /**
     * Delete an entity.
     *
     * @param entity the entity to delete
     */
    public void deleteEntity(final Object entity);
}
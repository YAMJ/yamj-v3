package com.moviejukebox.core.hibernate;

import java.io.Serializable;
import java.sql.Timestamp;

import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;

/**
 * Interceptor for entity audits.
 */
public class AuditInterceptor extends EmptyInterceptor {

	/**
	 * Serial version UID
	 */
	private static final long serialVersionUID = 6892420119984901561L;

	/**
	 * @see org.hibernate.Interceptor#onFlushDirty(Object, Serializable,
	 *      Object[], Object[], String[], org.hibernate.type.Type[])
	 */
	@Override
	public boolean onFlushDirty(final Object entity, final Serializable id, final Object[] currentState,
		final Object[] previousState, final String[] propertyNames, final Type[] types)
	{
		if ( entity instanceof Auditable ) {
			for (int i = 0; i < propertyNames.length; i++) {
				if ( "updateTimestamp".equals(propertyNames[i]) ) {
					currentState[i] = new Timestamp(System.currentTimeMillis());
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * @see org.hibernate.Interceptor#onSave(Object, Serializable, Object[],
	 *      String[], org.hibernate.type.Type[])
	 */
	@Override
	public boolean onSave(final Object entity, final Serializable id, final Object[] state,
		final String[] propertyNames, final Type[] types)
	{

		if ( entity instanceof Auditable ) {
			for (int i = 0; i < propertyNames.length; i++) {
				if ( "createTimestamp".equals(propertyNames[i]) ) {
					state[i] = new Timestamp(System.currentTimeMillis());
					return true;
				}
			}
		}
		return false;
	}
}
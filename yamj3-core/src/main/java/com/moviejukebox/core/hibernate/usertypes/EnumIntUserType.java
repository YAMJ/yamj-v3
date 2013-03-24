package com.moviejukebox.core.hibernate.usertypes;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.EnumSet;
import java.util.Properties;
import org.hibernate.HibernateException;
import org.hibernate.usertype.EnhancedUserType;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

/**
 * User type for enumerations.
 * <p>
 * Example Mapping: inline typedef<br>
 * <code> 
 * &lt;property name=&quot;suit&quot;&gt;
 *   &lt;type name=&quot;EnumIntUserType&quot;&gt;
 *     &lt;param name=&quot;enumClassName&quot;&gt;com.company.project.Suit&lt;/param&gt;
 *   &lt;/type&gt;
 * &lt;/property&gt;
 * </code>
 * <p>
 * Example Mapping - using &lt;typedef&gt;<br>
 * <code>
 * &lt;typedef name=&quot;suit&quot; class=&quot;EnumIntUserType&quot;&gt;
 *   &lt;param name=&quot;enumClassName&quot;&gt;com.company.project.Suit&lt;/param&gt;
 * &lt;/typedef&gt;
 * 
 * &lt;class ...&gt;
 *   &lt;property name='suit' type='suit'/&gt;
 * &lt;/class&gt;
 * </code>
 */
public class EnumIntUserType implements EnhancedUserType, ParameterizedType {

	/**
	 * Holds the SQL types
	 */
	private static final int[] SQL_TYPES = {Types.INTEGER};

	/**
	 * Holds the enum class
	 */
	@SuppressWarnings("rawtypes")
	private Class<Enum> enumClass;

	/**
	 * @see ParameterizedType#setParameterValues(Properties)
	 */
    @Override
	@SuppressWarnings({"unchecked","rawtypes"})
	public void setParameterValues(final Properties parameters) {
		String enumClassName = parameters.getProperty("enumClassName");
		try {
			this.enumClass = (Class<Enum>)Class.forName(enumClassName);
		}
		catch (ClassNotFoundException cnfe) {
			throw new HibernateException("Enum class not found", cnfe);
		}
	}

	/**
	 * @see UserType#returnedClass()
	 */
    @Override
	@SuppressWarnings("rawtypes")
	public Class returnedClass() {
		return this.enumClass;
	}

	/**
	 * @see UserType#sqlTypes()
	 */
    @Override
	public int[] sqlTypes() {
		return SQL_TYPES;
	}

	/**
	 * @see UserType#replace(Object, Object, Object)
	 */
    @Override
	public Object replace(final Object original, final Object target, final Object owner) throws HibernateException {
		return original;
	}

	/**
	 * @see UserType#assemble(Serializable, Object)
	 */
    @Override
	public Object assemble(final Serializable cached, final Object owner) throws HibernateException {
		return cached;
	}

	/**
	 * @see UserType#disassemble(Object)
	 */
    @Override
	@SuppressWarnings("rawtypes")
	public Serializable disassemble(final Object value) throws HibernateException {
		return (Enum)value;
	}

	/**
	 * @see UserType#deepCopy(Object)
	 */
    @Override
	public Object deepCopy(final Object value) throws HibernateException {
		return value;
	}

	/**
	 * @see UserType#equals(Object, Object)
	 */
    @Override
	public boolean equals(final Object x, final Object y) throws HibernateException {
		return x == y;
	}

	/**
	 * @see UserType#hashCode(Object)
	 */
    @Override
	public int hashCode(final Object x) throws HibernateException {
		return x.hashCode();
	}

	/**
	 * @see UserType#isMutable()
	 */
    @Override
	public boolean isMutable() {
		return false;
	}

	/**
	 * @see UserType#nullSafeGet(ResultSet, String[], Object)
	 */
    @Override
	@SuppressWarnings("unchecked")
	public Object nullSafeGet(final ResultSet rs, final String[] names, final Object owner) throws HibernateException,
		SQLException
	{
		int ordinal = rs.getInt(names[0]);

		// don't know if we are guaranteed to get the ordinals
		// in the right order with toArray() below
		return rs.wasNull()?null:EnumSet.allOf(this.enumClass).toArray()[ordinal];
	}

	/**
	 * @see UserType#nullSafeSet(PreparedStatement, Object, int)
	 */
    @Override
	@SuppressWarnings("rawtypes")
	public void nullSafeSet(final PreparedStatement st, final Object value, final int index) throws HibernateException,
		SQLException
	{
		if ( value == null ) {
			st.setNull(index, Types.INTEGER);
		} else {
			st.setInt(index, ((Enum)value).ordinal());
		}
	}

	/**
	 * @see EnhancedUserType#fromXMLString(String)
	 */
    @Override
	@SuppressWarnings("unchecked")
	public Object fromXMLString(final String xmlValue) {
		return Enum.valueOf(this.enumClass, xmlValue);
	}

	/**
	 * @see EnhancedUserType#objectToSQLString(Object)
	 */
    @Override
	@SuppressWarnings("rawtypes")
	public String objectToSQLString(final Object value) {
		return new StringBuffer(((Enum)value).ordinal()).toString();
	}

	/**
	 * @see EnhancedUserType#toXMLString(Object)
	 */
    @Override
	public String toXMLString(final Object value) {
		return this.objectToSQLString(value);
	}
}
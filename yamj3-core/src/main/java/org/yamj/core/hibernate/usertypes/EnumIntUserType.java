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
package org.yamj.core.hibernate.usertypes;

import java.io.Serializable;
import java.sql.*;
import java.util.EnumSet;
import java.util.Properties;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
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
     * @param parameters
     * @see ParameterizedType#setParameterValues(Properties)
     */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void setParameterValues(final Properties parameters) {
        String enumClassName = parameters.getProperty("enumClassName");
        try {
            this.enumClass = (Class<Enum>) Class.forName(enumClassName);
        } catch (ClassNotFoundException cnfe) {
            throw new HibernateException("Enum class not found", cnfe);
        }
    }

    /**
     * @return @see UserType#returnedClass()
     */
    @Override
    public Class returnedClass() {
        return this.enumClass;
    }

    /**
     * @return @see UserType#sqlTypes()
     */
    @Override
    public int[] sqlTypes() {
        return SQL_TYPES;
    }

    /**
     * @param original
     * @param owner
     * @param target
     * @return
     * @see UserType#replace(Object, Object, Object)
     */
    @Override
    public Object replace(final Object original, final Object target, final Object owner) throws HibernateException {
        return original;
    }

    /**
     * @param cached
     * @param owner
     * @return
     * @see UserType#assemble(Serializable, Object)
     */
    @Override
    public Object assemble(final Serializable cached, final Object owner) throws HibernateException {
        return cached;
    }

    /**
     * @param value
     * @return
     * @see UserType#disassemble(Object)
     */
    @Override
    @SuppressWarnings("rawtypes")
    public Serializable disassemble(final Object value) throws HibernateException {
        return (Enum) value;
    }

    /**
     * @param value
     * @return
     * @see UserType#deepCopy(Object)
     */
    @Override
    public Object deepCopy(final Object value) throws HibernateException {
        return value;
    }

    /**
     * @param x
     * @param y
     * @return
     * @see UserType#equals(Object, Object)
     */
    @Override
    public boolean equals(final Object x, final Object y) throws HibernateException {
        if (null == x || null == y) {
            return false;
        }
        return x.equals(y);
    }

    /**
     * @param x
     * @return
     * @see UserType#hashCode(Object)
     */
    @Override
    public int hashCode(final Object x) throws HibernateException {
        return x.hashCode();
    }

    /**
     * @return
     *
     * @see UserType#isMutable()
     */
    @Override
    public boolean isMutable() {
        return false;
    }

    /**
     * @param rs
     * @param names
     * @param session
     * @param owner
     * @return
     * @throws java.sql.SQLException
     * @see UserType#nullSafeGet(ResultSet, String[], SessionImplementor, Object)
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object nullSafeGet(final ResultSet rs, final String[] names, final SessionImplementor session, final Object owner)
            throws HibernateException, SQLException {
        int ordinal = rs.getInt(names[0]);

        // don't know if we are guaranteed to get the ordinals
        // in the right order with toArray() below
        return rs.wasNull() ? null : EnumSet.allOf(this.enumClass).toArray()[ordinal];
    }

    /**
     * @param st
     * @param value
     * @param index
     * @param session
     * @throws java.sql.SQLException
     * @see UserType#nullSafeSet(PreparedStatement, Object, int, SessionImplementor)
     */
    @Override
    @SuppressWarnings("rawtypes")
    public void nullSafeSet(final PreparedStatement st, final Object value, final int index, final SessionImplementor session)
            throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index, Types.INTEGER);
        } else {
            st.setInt(index, ((Enum) value).ordinal());
        }
    }

    /**
     * @param xmlValue
     * @return
     * @see EnhancedUserType#fromXMLString(String)
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object fromXMLString(final String xmlValue) {
        return Enum.valueOf(this.enumClass, xmlValue);
    }

    /**
     * @param value
     * @return
     * @see EnhancedUserType#objectToSQLString(Object)
     */
    @Override
    @SuppressWarnings("rawtypes")
    public String objectToSQLString(final Object value) {
        return new StringBuilder(((Enum) value).ordinal()).toString();
    }

    /**
     * @param value
     * @return
     * @see EnhancedUserType#toXMLString(Object)
     */
    @Override
    public String toXMLString(final Object value) {
        return this.objectToSQLString(value);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(enumClass)
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof EnumIntUserType)) {
            return false;
        }
        final EnumIntUserType other = (EnumIntUserType) obj;
        return new EqualsBuilder()
                .append(enumClass, other.enumClass)
                .isEquals();
    }
}

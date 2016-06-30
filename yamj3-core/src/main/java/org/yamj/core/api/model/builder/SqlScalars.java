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
package org.yamj.core.api.model.builder;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.type.BasicType;

/**
 * Builds a SQL statement and holds the scalars for the statement
 *
 * @author Stuart
 */
public final class SqlScalars {

    private StringBuilder sql;
    private final Map<String, BasicType> scalars = new HashMap<>();
    private final Map<String, Object> parameters = new HashMap<>();

    public SqlScalars() {
        this.sql = new StringBuilder();
    }

    public SqlScalars(StringBuilder sql) {
        setSql(sql);
    }

    /**
     * Set the SQL
     *
     * @param sql
     */
    public void setSql(StringBuilder sql) {
        this.sql = sql;
    }

    /**
     * Append a line to the SQL.
     *
     * Will add a space at the end of the line
     *
     * @param line
     */
    public void addToSql(String line) {
        sql.append(line);
        if (!line.endsWith(" ")) {
            sql.append(" ");
        }
    }

    /**
     * Get the SQL as a string
     *
     * @return
     */
    public String getSql() {
        return StringUtils.normalizeSpace(sql.toString());
    }

    /**
     * Get the parameters for the query
     *
     * @return
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }

    /**
     * Add a parameter to the query
     *
     * @param param
     * @param value
     */
    public void addParameter(String param, Object value) {
        this.parameters.put(param, value);
    }

    /**
     * Get the scalars for the query
     *
     * @return
     */
    public Map<String, BasicType> getScalars() {
        return scalars;
    }

    /**
     * Add a scalar with type
     *
     * @param scalar
     * @param type
     */
    public void addScalar(String scalar, BasicType type) {
        this.scalars.put(scalar, type);
    }
}

/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
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
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.database.dao;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SQLQuery;
import org.springframework.stereotype.Service;
import org.yamj.core.api.model.CountTimestamp;
import org.yamj.core.api.model.dto.IndexVideoDTO;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.api.model.ApiWrapperList;
import org.yamj.core.api.model.IApiWrapper;
import org.yamj.core.api.model.dto.IndexPersonDTO;
import org.yamj.core.api.options.IOptions;
import org.yamj.core.hibernate.HibernateDao;

@Service("apiDao")
public class ApiDao extends HibernateDao {

    public void getVideoList(final String sqlString, ApiWrapperList<IndexVideoDTO> wrapper) {
        List<Object[]> queryResults = executeQuery(sqlString, wrapper);
        List<IndexVideoDTO> indexElements = new ArrayList<IndexVideoDTO>();
        for (Object[] result : queryResults) {
            IndexVideoDTO indexElement = new IndexVideoDTO();
            indexElement.setId(convertRowElementToLong(result[0]));
            indexElement.setVideoType(convertRowElementToString(result[1]));
            indexElement.setTitle(convertRowElementToString(result[2]));
            indexElement.setYear(convertRowElementToInteger(result[3]));
            indexElement.setIdentifier(convertRowElementToString(result[4]));
            indexElements.add(indexElement);
        }

        wrapper.setResults(indexElements);
    }

    public CountTimestamp getCountTimestamp(MetaDataType type, String tablename, String clause) {
        if (StringUtils.isBlank(tablename)) {
            return null;
        }

        StringBuilder sql = new StringBuilder("SELECT count(*), MAX(create_timestamp), MAX(update_timestamp), MAX(id) FROM ");
        sql.append(tablename);
        if (StringUtils.isNotBlank(clause)) {
            sql.append(" WHERE ").append(clause);
        }

        SQLQuery query = getSession().createSQLQuery(sql.toString());
        query.setReadOnly(true);
        query.setCacheable(true);

        Object[] result = (Object[]) query.uniqueResult();
        CountTimestamp ct = new CountTimestamp(type);
        ct.setCount(convertRowElementToInteger(result[0]));
        ct.setCreateTimestamp(convertRowElementToDate(result[1]));
        ct.setUpdateTimestamp(convertRowElementToDate(result[2]));
        ct.setLastId(convertRowElementToLong(result[3]));
        return ct;
    }

    public void getPersonList(final String sqlString, ApiWrapperList<IndexPersonDTO> wrapper) {
        List<Object[]> queryResults = executeQuery(sqlString, wrapper);

        List<IndexPersonDTO> indexElements=new ArrayList<IndexPersonDTO>();
        for(Object[] result : queryResults) {
            IndexPersonDTO person = new IndexPersonDTO();
            person.setId(convertRowElementToLong(result[0]));
            person.setName(convertRowElementToString(result[1]));
            person.setBiography(convertRowElementToString(result[2]));
            person.setBirthDay(convertRowElementToDate(result[3]));
            person.setBirthPlace(convertRowElementToString(result[4]));
            person.setBirthName(convertRowElementToString(result[5]));
            person.setDeathDay(convertRowElementToDate(result[6]));
            indexElements.add(person);
        }
        wrapper.setResults(indexElements);
    }

    private List<Object[]> executeQuery(String sql, IApiWrapper wrapper) {
        SQLQuery query = getSession().createSQLQuery(sql);
        query.setReadOnly(true);
        query.setCacheable(true);

        // Run the query once to get the maximum returned results
        List<Object[]> queryResults = query.list();
        wrapper.setTotalCount(queryResults.size());

        // If there is a start or max set, we will need to re-run the query after setting the options
        IOptions options = (IOptions) wrapper.getParameters();
        if (options.getStart() > 0 || options.getMax() > 0) {
            if (options.getStart() > 0) {
                query.setFirstResult(options.getStart());
            }

            if (options.getMax() > 0) {
                query.setMaxResults(options.getMax());
            }
            // This will get the trimmed list
            queryResults = query.list();
        }
        return queryResults;
    }
}

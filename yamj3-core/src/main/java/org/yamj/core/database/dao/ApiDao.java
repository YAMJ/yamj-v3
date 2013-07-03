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
import org.yamj.core.api.model.dto.IndexDTO;
import org.yamj.core.api.options.OptionsIndex;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.hibernate.HibernateDao;

@Service("apiDao")
public class ApiDao extends HibernateDao {

    public List<IndexDTO> getVideoList(final String sql, final OptionsIndex options) {
        SQLQuery query = getSession().createSQLQuery(sql.toString());
        query.setReadOnly(true);
        query.setCacheable(true);

        if (options.getStart() > 0) {
            query.setFirstResult(options.getStart());
        }

        if (options.getMax() > 0) {
            query.setMaxResults(options.getMax());
        }

        List<IndexDTO> indexElements = new ArrayList<IndexDTO>();
        List<Object[]> objects = query.list();
        for (Object[] object : objects) {
            IndexDTO indexElement = new IndexDTO();
            indexElement.setId(convertRowElementToLong(object[0]));
            indexElement.setVideoType(convertRowElementToString(object[1]));
            indexElement.setTitle(convertRowElementToString(object[2]));
            indexElement.setYear(convertRowElementToInteger(object[3]));
            indexElement.setIdentifier(convertRowElementToString(object[4]));
            indexElements.add(indexElement);
        }

        return indexElements;
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
}

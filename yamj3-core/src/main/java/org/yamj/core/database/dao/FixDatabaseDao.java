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
package org.yamj.core.database.dao;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.core.hibernate.HibernateDao;

@Transactional
@Repository("fixDatabaseDao")
public class FixDatabaseDao extends HibernateDao {

    /**
     * Issues: #150, #151
     * Date:   28.01.2015
     */
    public void fixCountryChanges() {
        StringBuilder sb = new StringBuilder();
        
        // drop COUNTRY override flag from
        sb.append("DELETE FROM videodata_override WHERE flag='COUNTRY'");
        currentSession().createSQLQuery(sb.toString()).executeUpdate();
        
        // insert countries from existing video data country
        sb.setLength(0);
        sb.append("INSERT INTO country(name) ");
        sb.append("SELECT distinct vd.country FROM videodata vd ");
        sb.append("WHERE vd.country is not null ");
        sb.append("AND not exists(select 1 from country c where c.name=vd.country)");
        currentSession().createSQLQuery(sb.toString()).executeUpdate();
        
        // make dependencies between videodata and country
        sb.setLength(0);
        sb.append("INSERT INTO videodata_countries(data_id, country_id) ");
        sb.append("SELECT vd.id,c.id ");
        sb.append("FROM videodata vd, country c ");
        sb.append("WHERE vd.country=c.name ");
        sb.append("AND not exists(select 1 from videodata_countries vc where vc.data_id=vd.id and vc.country_id=c.id)");
        currentSession().createSQLQuery(sb.toString()).executeUpdate();
        
        sb.setLength(0);
        sb.append("ALTER TABLE videodata DROP column country");
        currentSession().createSQLQuery(sb.toString()).executeUpdate();
    }
}

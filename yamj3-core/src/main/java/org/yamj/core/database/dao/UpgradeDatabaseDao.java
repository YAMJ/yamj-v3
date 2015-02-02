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
public class UpgradeDatabaseDao extends HibernateDao {

    private boolean existsColumn(String table, String column) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM information_schema.COLUMNS ");
        sb.append("WHERE TABLE_SCHEMA = 'yamj3' ");
        sb.append("AND TABLE_NAME = '").append(table).append("' ");
        sb.append("AND COLUMN_NAME = '").append(column).append("'");
        Object object = currentSession().createSQLQuery(sb.toString()).uniqueResult();
        return (object != null);
    }

    @SuppressWarnings("unused")
    private boolean existsForeignKey(String table, String foreignKey) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM information_schema.TABLE_CONSTRAINTS ");
        sb.append("WHERE TABLE_SCHEMA = 'yamj3' ");
        sb.append("AND TABLE_NAME = '").append(table).append("' ");
        sb.append("AND CONSTRAINT_TYPE = 'FOREIGN KEY' ");
        sb.append("AND CONSTRAINT_NAME = '").append(foreignKey).append("'");
        Object object = currentSession().createSQLQuery(sb.toString()).uniqueResult();
        return (object != null);
    }

    @SuppressWarnings("unused")
    private boolean existsUniqueIndex(String table, String indexName) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM information_schema.TABLE_CONSTRAINTS ");
        sb.append("WHERE TABLE_SCHEMA = 'yamj3' ");
        sb.append("AND TABLE_NAME = '").append(table).append("' ");
        sb.append("AND CONSTRAINT_TYPE = 'UNIQUE' ");
        sb.append("AND CONSTRAINT_NAME = '").append(indexName).append("'");
        Object object = currentSession().createSQLQuery(sb.toString()).uniqueResult();
        return (object != null);
    }

    /**
     * Issues: #151
     * Date:   28.01.2015
     */
    public void patchCountryOverrideFlag() {
        currentSession()
            .createSQLQuery("UPDATE videodata_override SET flag='COUNTRIES' WHERE flag='COUNTRY'")
            .executeUpdate();
    }
    
    /**
     * Issues: #150, #151
     * Date:   28.01.2015
     */
    public void patchVideoDataCountries() {
        // check if patch is needed
        if (!existsColumn("videodata", "country")) return;
        
        // insert countries from existing video data country
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO country(name) ");
        sb.append("SELECT distinct vd.country FROM videodata vd ");
        sb.append("WHERE vd.country is not null ");
        sb.append("AND not exists(select 1 from country c where c.name=vd.country)");
        currentSession().createSQLQuery(sb.toString()).executeUpdate();
        
        // make dependencies between video data and country
        sb.setLength(0);
        sb.append("INSERT INTO videodata_countries(data_id, country_id) ");
        sb.append("SELECT vd.id,c.id ");
        sb.append("FROM videodata vd, country c ");
        sb.append("WHERE vd.country=c.name ");
        sb.append("AND not exists(select 1 from videodata_countries vc where vc.data_id=vd.id and vc.country_id=c.id)");
        currentSession().createSQLQuery(sb.toString()).executeUpdate();
        
        // drop obsolete column country
        sb.setLength(0);
        sb.append("ALTER TABLE videodata DROP column country");
        currentSession().createSQLQuery(sb.toString()).executeUpdate();
    }

    /**
     * Issues: none
     * Date:   02.02.2015
     */
    public void patchAllocineWonAwards() {
        StringBuilder sb = new StringBuilder();
        sb.append("UPDATE movie_awards ma SET ma.won=1 ");
        sb.append("WHERE EXISTS (");
        sb.append("  SELECT 1 ");
        sb.append("  FROM award aw ");
        sb.append("  WHERE aw.sourcedb='allocine' ");
        sb.append("  AND aw.id=ma.award_id) ");
        sb.append("AND ma.won=0");
        currentSession().createSQLQuery(sb.toString()).executeUpdate();

        sb.setLength(0);
        sb.append("UPDATE series_awards sa SET sa.won=1 ");
        sb.append("WHERE EXISTS (");
        sb.append("  SELECT 1 ");
        sb.append("  FROM award aw ");
        sb.append("  WHERE aw.sourcedb='allocine' ");
        sb.append("  AND aw.id=sa.award_id) ");
        sb.append("AND sa.won=0");
        currentSession().createSQLQuery(sb.toString()).executeUpdate();
    }
}

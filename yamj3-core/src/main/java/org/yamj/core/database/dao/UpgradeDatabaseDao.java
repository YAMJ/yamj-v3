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
     * Issues: #195
     * Date:   07.07.2015
     */
    public void patchFilmographyConfig() {
        currentSession()
            .createSQLQuery("DELETE FROM configuration WHERE config_key='allocine.person.filmography'")
            .executeUpdate();
        currentSession()
        	.createSQLQuery("DELETE FROM configuration WHERE config_key='themoviedb.person.filmography'")
        	.executeUpdate();
    }

    /**
     * Issues: #218
     * Date:   08.07.2015
     */
    public void patchSkipOnlineScans() {
        if (existsColumn("videodata", "skip_online_scans")) {
            StringBuilder sb = new StringBuilder();
            sb.setLength(0);
            sb.append("ALTER TABLE videodata DROP skip_online_scans");
            currentSession().createSQLQuery(sb.toString()).executeUpdate();
        }

        if (existsColumn("series", "skip_online_scans")) {
            StringBuilder sb = new StringBuilder();
            sb.setLength(0);
            sb.append("ALTER TABLE series DROP skip_online_scans");
            currentSession().createSQLQuery(sb.toString()).executeUpdate();
        }
    }

    /**
     * Issues: #222
     * Date:   18.07.2015
     */
    public void patchTrailers() {
        if (existsColumn("videodata", "trailer_status")) {
            currentSession()
                .createSQLQuery("UPDATE videodata set trailer_status = 'NEW' where trailer_status=''")
                .executeUpdate();
        }

        if (existsColumn("series", "trailer_status")) {
            currentSession()
                .createSQLQuery("UPDATE series set trailer_status = 'NEW' where trailer_status=''")
                .executeUpdate();
        }

        if (existsColumn("trailer", "source_hash")) {
            currentSession()
                .createSQLQuery("UPDATE trailer set hash_code=source_hash")
                .executeUpdate();
            currentSession()
                .createSQLQuery("ALTER TABLE trailer DROP source_hash")
                .executeUpdate();
        }
    }
}

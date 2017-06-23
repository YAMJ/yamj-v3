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

import static org.yamj.plugin.api.metadata.MetadataTools.cleanRole;

import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.core.database.model.CastCrew;
import org.yamj.core.hibernate.HibernateDao;

@Transactional
@Repository("upgradeDatabaseDao")
public class UpgradeDatabaseDao extends HibernateDao {

    // MYSQL CHECKS

    @SuppressWarnings("unchecked")
	protected boolean mysqlExistsTable(String table) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM information_schema.TABLES ");
        sb.append("WHERE TABLE_SCHEMA = 'yamj3' ");
        sb.append("AND TABLE_NAME = '").append(table).append("' ");
        List<Object> objects = currentSession().createSQLQuery(sb.toString()).list();
        return CollectionUtils.isNotEmpty(objects);
    }

    @SuppressWarnings("unchecked")
	protected boolean mysqlExistsColumn(String table, String column) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM information_schema.COLUMNS ");
        sb.append("WHERE TABLE_SCHEMA = 'yamj3' ");
        sb.append("AND TABLE_NAME = '").append(table).append("' ");
        sb.append("AND COLUMN_NAME = '").append(column).append("'");
        List<Object> objects = currentSession().createSQLQuery(sb.toString()).list();
        return CollectionUtils.isNotEmpty(objects);
    }

    @SuppressWarnings("unchecked")
	    protected boolean mysqlExistsForeignKey(String table, String foreignKey) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM information_schema.TABLE_CONSTRAINTS ");
        sb.append("WHERE TABLE_SCHEMA = 'yamj3' ");
        sb.append("AND TABLE_NAME = '").append(table).append("' ");
        sb.append("AND CONSTRAINT_TYPE = 'FOREIGN KEY' ");
        sb.append("AND CONSTRAINT_NAME = '").append(foreignKey).append("'");
        List<Object> objects = currentSession().createSQLQuery(sb.toString()).list();
        return CollectionUtils.isNotEmpty(objects);
    }

    protected void mysqldropForeignKey(String table, String foreignKey) {
        if (mysqlExistsForeignKey(table, foreignKey)) {
            StringBuilder sb = new StringBuilder();
            sb.append("ALTER TABLE ").append(table);
            sb.append(" DROP FOREIGN KEY ").append(foreignKey);
            currentSession().createSQLQuery(sb.toString()).executeUpdate();
        }
    }
    
    @SuppressWarnings({ "cast", "unchecked" })
    protected List<String> mysqlListForeignKeys(String table) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS ");
        sb.append("WHERE TABLE_SCHEMA = 'yamj3' ");
        sb.append("AND TABLE_NAME = '").append(table).append("' ");
        sb.append("AND CONSTRAINT_TYPE = 'FOREIGN KEY' ");
        return (List<String>) currentSession().createSQLQuery(sb.toString()).list();
    }

    @SuppressWarnings("unchecked")
	protected boolean mysqlExistsIndex(String table, String indexName) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM information_schema.STATISTICS ");
        sb.append("WHERE TABLE_SCHEMA = 'yamj3' ");
        sb.append("AND TABLE_NAME = '").append(table).append("' ");
        sb.append("AND INDEX_NAME = '").append(indexName).append("'");
        List<Object> objects = currentSession().createSQLQuery(sb.toString()).list();
        return CollectionUtils.isNotEmpty(objects);
    }

    protected void mysqlDropIndex(String table, String indexName) {
        if (mysqlExistsIndex(table, indexName)) {
            StringBuilder sb = new StringBuilder();
            sb.append("ALTER TABLE ").append(table);
            sb.append(" DROP INDEX ").append(indexName);
            currentSession().createSQLQuery(sb.toString()).executeUpdate();
        }
    }

    @SuppressWarnings("unchecked")
	protected boolean mysqlExistsUniqueIndex(String table, String indexName) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM information_schema.TABLE_CONSTRAINTS ");
        sb.append("WHERE TABLE_SCHEMA = 'yamj3' ");
        sb.append("AND TABLE_NAME = '").append(table).append("' ");
        sb.append("AND CONSTRAINT_TYPE = 'UNIQUE' ");
        sb.append("AND CONSTRAINT_NAME = '").append(indexName).append("'");
        List<Object> objects = currentSession().createSQLQuery(sb.toString()).list();
        return CollectionUtils.isNotEmpty(objects);
    }

    protected void mysqlDropUniqueIndex(String table, String indexName) {
        if (mysqlExistsUniqueIndex(table, indexName)) {
            StringBuilder sb = new StringBuilder();
            sb.append("ALTER TABLE ").append(table);
            sb.append(" DROP INDEX ").append(indexName);
            currentSession().createSQLQuery(sb.toString()).executeUpdate();
        }
    }  

    // HSQL CHECKS
    
    @SuppressWarnings("unchecked")
	protected boolean hsqlExistsColumn(String table, String column) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM information_schema.COLUMNS ");
        sb.append("WHERE UPPER(TABLE_NAME) = '").append(table.toUpperCase()).append("' ");
        sb.append("AND UPPER(COLUMN_NAME) = '").append(column.toUpperCase()).append("'");
        List<Object> objects = currentSession().createSQLQuery(sb.toString()).list();
        return CollectionUtils.isNotEmpty(objects);
    }

    // FIX ROLES
    
    @SuppressWarnings("unchecked")
	public void fixRoles() {
        List<CastCrew> list = currentSession().createQuery("FROM CastCrew cc WHERE cc.role like '%(%'").list();
        for (CastCrew cc : list) {
            String role = cc.getRole();
            String fixedRole = cleanRole(role);
            if (!StringUtils.equals(role, fixedRole)) {
                cc.setRole(StringUtils.abbreviate(fixedRole, 255));
                currentSession().update(cc);
            }
        }
    }
    
    // PATCHES
    
    public void deleteOrphanConfigs() {
        currentSession()
        .createSQLQuery("DELETE FROM configuration WHERE config_key like '%.throwError.tempUnavailable'")
        .executeUpdate();

        currentSession()
        .createSQLQuery("DELETE FROM configuration WHERE config_key like '%.maxRetries.movie' and config_key not like 'yamj3.error.%'")
        .executeUpdate();

        currentSession()
        .createSQLQuery("DELETE FROM configuration WHERE config_key like '%.maxRetries.tvshow' and config_key not like 'yamj3.error.%'")
        .executeUpdate();

        currentSession()
        .createSQLQuery("DELETE FROM configuration WHERE config_key like '%.maxRetries.person' and config_key not like 'yamj3.error.%'")
        .executeUpdate();

        currentSession()
        .createSQLQuery("DELETE FROM configuration WHERE config_key like '%.maxRetries.filmography' and config_key not like 'yamj3.error.%'")
        .executeUpdate();
        
        currentSession()
        .createSQLQuery("DELETE FROM configuration WHERE config_key='tvrage.language'")
        .executeUpdate();

        currentSession()
        .createSQLQuery("DELETE FROM configuration WHERE config_key='tvrage.country'")
        .executeUpdate();

        currentSession()
        .createSQLQuery("DELETE FROM configuration WHERE config_key='nfo.ignore.present.id'")
        .executeUpdate();

        currentSession()
        .createSQLQuery("DELETE FROM configuration WHERE config_key='yamj3.castcrew.skip.uncredited'")
        .executeUpdate();

        currentSession()
        .createSQLQuery("DELETE FROM configuration WHERE config_key='yamj3.castcrew.skip.faceless'")
        .executeUpdate();

        currentSession()
        .createSQLQuery("DELETE FROM configuration WHERE config_key='themoviedb.language'")
        .executeUpdate();

        currentSession()
        .createSQLQuery("DELETE FROM configuration WHERE config_key='themoviedb.country'")
        .executeUpdate();

        currentSession()
        .createSQLQuery("DELETE FROM configuration WHERE config_key='thetvdb.language'")
        .executeUpdate();

        currentSession()
        .createSQLQuery("DELETE FROM configuration WHERE config_key='thetvdb.country'")
        .executeUpdate();

        currentSession()
        .createSQLQuery("DELETE FROM configuration WHERE config_key='fanarttv.language'")
        .executeUpdate();

        currentSession()
        .createSQLQuery("DELETE FROM configuration WHERE config_key='fanarttv.country'")
        .executeUpdate();

        currentSession()
        .createSQLQuery("DELETE FROM configuration WHERE config_key='trakttv.language'")
        .executeUpdate();

        currentSession()
        .createSQLQuery("DELETE FROM configuration WHERE config_key='trakttv.country'")
        .executeUpdate();

        currentSession()
        .createSQLQuery("DELETE FROM configuration WHERE config_key='imdb.language'")
        .executeUpdate();

        currentSession()
        .createSQLQuery("DELETE FROM configuration WHERE config_key='imdb.country'")
        .executeUpdate();
    }
}

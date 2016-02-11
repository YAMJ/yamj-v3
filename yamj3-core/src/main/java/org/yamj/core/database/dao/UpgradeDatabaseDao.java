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

import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.core.hibernate.HibernateDao;

@Transactional
@Repository("upgradeDatabaseDao")
public class UpgradeDatabaseDao extends HibernateDao {

    // MYSQL CHECKS
    
    protected boolean mysqlExistsColumn(String table, String column) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM information_schema.COLUMNS ");
        sb.append("WHERE TABLE_SCHEMA = 'yamj3' ");
        sb.append("AND TABLE_NAME = '").append(table).append("' ");
        sb.append("AND COLUMN_NAME = '").append(column).append("'");
        List<Object> objects = currentSession().createSQLQuery(sb.toString()).list();
        return CollectionUtils.isNotEmpty(objects);
    }

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
    
    @SuppressWarnings("cast")
    protected List<String> mysqlListForeignKeys(String table) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS ");
        sb.append("WHERE TABLE_SCHEMA = 'yamj3' ");
        sb.append("AND TABLE_NAME = '").append(table).append("' ");
        sb.append("AND CONSTRAINT_TYPE = 'FOREIGN KEY' ");
        return (List<String>) currentSession().createSQLQuery(sb.toString()).list();
    }

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
    
    protected boolean hsqlExistsColumn(String table, String column) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM information_schema.COLUMNS ");
        sb.append("WHERE UPPER(TABLE_NAME) = '").append(table.toUpperCase()).append("' ");
        sb.append("AND UPPER(COLUMN_NAME) = '").append(column.toUpperCase()).append("'");
        List<Object> objects = currentSession().createSQLQuery(sb.toString()).list();
        return CollectionUtils.isNotEmpty(objects);
    }

    // PATCHES
    
    // Patch for artwork generated status
    
    public void mysqlPatchArtworkGeneratedStatus() {
        if (!mysqlExistsColumn("artwork_generated", "status")) {
            currentSession()
            .createSQLQuery("ALTER TABLE artwork_generated ADD COLUMN status VARCHAR(30)")
            .executeUpdate();
        }
        
        currentSession()
        .createSQLQuery("UPDATE artwork_generated SET status='DONE' WHERE status is null")
        .executeUpdate();

        currentSession()
        .createSQLQuery("ALTER TABLE artwork_generated MODIFY COLUMN status VARCHAR(30) NOT NULL")
        .executeUpdate();            
    }
    
    public void hsqlPatchArtworkGeneratedStatus() {
        if (!hsqlExistsColumn("artwork_generated", "status")) {
            currentSession()
            .createSQLQuery("ALTER TABLE artwork_generated ADD COLUMN status VARCHAR(30)")
            .executeUpdate();
        }
        
        currentSession()
        .createSQLQuery("UPDATE artwork_generated SET status='DONE' WHERE status is null")
        .executeUpdate();

        currentSession()
        .createSQLQuery("ALTER TABLE artwork_generated ALTER COLUMN status SET NOT NULL")
        .executeUpdate();
    }

    // Patch for artwork profile
    
    public void mysqlPatchArtworkProfile() {
        if (!mysqlExistsColumn("artwork_profile", "scaling")) {
            currentSession()
            .createSQLQuery("ALTER TABLE artwork_profile ADD COLUMN scaling VARCHAR(20)")
            .executeUpdate();
        }
        if (mysqlExistsColumn("artwork_profile", "normalize")) {
            currentSession()
            .createSQLQuery("ALTER TABLE artwork_profile DROP COLUMN normalize")
            .executeUpdate();
        }
        if (mysqlExistsColumn("artwork_profile", "stretch")) {
            currentSession()
            .createSQLQuery("ALTER TABLE artwork_profile DROP COLUMN stretch")
            .executeUpdate();
        }
        if (mysqlExistsColumn("artwork_profile", "apply_to_episode")) {
            currentSession()
            .createSQLQuery("ALTER TABLE artwork_profile DROP COLUMN apply_to_episode")
            .executeUpdate();
        }
        if (mysqlExistsColumn("artwork_profile", "apply_to_person")) {
            currentSession()
            .createSQLQuery("ALTER TABLE artwork_profile DROP COLUMN apply_to_person")
            .executeUpdate();
        }

        currentSession()
        .createSQLQuery("UPDATE artwork_profile SET scaling='NORMALIZE' WHERE scaling is null")
        .executeUpdate();

        currentSession()
        .createSQLQuery("ALTER TABLE artwork_profile MODIFY COLUMN scaling VARCHAR(20) NOT NULL")
        .executeUpdate();            
    }
    
    public void hsqlPatchArtworkProfile() {
        if (!hsqlExistsColumn("artwork_profile", "scaling")) {
            currentSession()
            .createSQLQuery("ALTER TABLE artwork_profile ADD COLUMN scaling VARCHAR(20)")
            .executeUpdate();
        }
        if (hsqlExistsColumn("artwork_profile", "normalize")) {
            currentSession()
            .createSQLQuery("ALTER TABLE artwork_profile DROP COLUMN normalize")
            .executeUpdate();
        }
        if (hsqlExistsColumn("artwork_profile", "stretch")) {
            currentSession()
            .createSQLQuery("ALTER TABLE artwork_profile DROP COLUMN stretch")
            .executeUpdate();
        }
        if (hsqlExistsColumn("artwork_profile", "apply_to_episode")) {
            currentSession()
            .createSQLQuery("ALTER TABLE artwork_profile DROP COLUMN apply_to_episode")
            .executeUpdate();
        }
        if (hsqlExistsColumn("artwork_profile", "apply_to_person")) {
            currentSession()
            .createSQLQuery("ALTER TABLE artwork_profile DROP COLUMN apply_to_person")
            .executeUpdate();
        }
        
        currentSession()
        .createSQLQuery("UPDATE artwork_profile SET scaling='NORMALIZE' WHERE scaling is null")
        .executeUpdate();

        currentSession()
        .createSQLQuery("ALTER TABLE artwork_profile ALTER COLUMN scaling SET NOT NULL")
        .executeUpdate();
    }
}

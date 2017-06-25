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
package org.yamj.core.database.service;

import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.stereotype.Component;
import org.yamj.core.database.DatabaseType;
import org.yamj.core.database.dao.UpgradeDatabaseDao;

@Component("upgradeDatabaseService")
public class UpgradeDatabaseService {

    private static final Logger LOG = LoggerFactory.getLogger(UpgradeDatabaseService.class);

    @Autowired
    @Qualifier("sessionFactory")
    private LocalSessionFactoryBean sessionFactory;
    @Autowired
    private UpgradeDatabaseDao upgradeDatabaseDao;
    
    @PostConstruct
    public void init() {
        LOG.trace("Upgrading database");

        final String dialect = sessionFactory.getHibernateProperties().getProperty("hibernate.dialect");
        final String databaseType;
        if (StringUtils.containsIgnoreCase(dialect, "HSQLDialect")) {
            databaseType = DatabaseType.HSQL;
        } else if (StringUtils.containsIgnoreCase(dialect, "MySQL")) {
            databaseType = DatabaseType.MYSQL;
        } else if (StringUtils.containsIgnoreCase(dialect, "H2Dialect")) {
            databaseType = DatabaseType.H2;
        } else {
            // no valid database type for patching
            databaseType = null;
        }
		
        LOG.trace("Run patches for database type {}", databaseType);
			
	
       LOG.debug("update database series_libraries, videodata_libraries");
		// Issues: 311  - populate libraries populate
        // Date:   24.06.2017
        try {
            upgradeDatabaseDao.patchDatabaseUpdateLibraries();
        } catch (Exception ex) {
		   LOG.warn("Failed to upgrade series_libraries for database type "+databaseType, ex);
        }
	
		// set HSQL to compatibility with SQL command by exemple syntax ON DUPLICATE KEY 
		if (databaseType == DatabaseType.HSQL)
		{
			try {
				upgradeDatabaseDao.patchDatabaseCompatibilityHSQL();
			} catch (Exception ex) {
			   LOG.warn("Failed to set compatibility for database type "+databaseType, ex);
			}
		}
		
        // fix roles (same for all database types)
        try {
            upgradeDatabaseDao.fixRoles();
        } catch (Exception ex) {
            LOG.warn("Failed upgrade 'fixRoles' for database type "+databaseType, ex);
        }

        // delete orphan configuration entries (same for all database types)
        try {
            upgradeDatabaseDao.deleteOrphanConfigs();
        } catch (Exception ex) {
            LOG.warn("Failed upgrade 'deleteOrphanConfigs' for database type "+databaseType, ex);
        }
    }
}

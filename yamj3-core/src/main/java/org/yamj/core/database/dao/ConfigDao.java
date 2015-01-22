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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.core.api.options.OptionsConfig;
import org.yamj.core.database.model.Configuration;
import org.yamj.core.hibernate.HibernateDao;

@Transactional
@Repository("configDao")
public class ConfigDao extends HibernateDao {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigDao.class);

    public Map<String, String> readConfig() {
        SQLQuery query = currentSession().createSQLQuery("select config_key, config_value from configuration");
        query.setReadOnly(true);
        query.setCacheable(true);

        HashMap<String, String> config = new HashMap<>();
        List<Object[]> objects = query.list();
        for (Object[] object : objects) {
            String key = convertRowElementToString(object[0]);
            String value = convertRowElementToString(object[1]);
            config.put(key, value);
        }

        return config;
    }

    public void storeConfig(Map<String, String> config) {
        for (Map.Entry<String, String> entry : config.entrySet()) {
            storeConfig(entry.getKey(), entry.getValue(), false);
        }
    }

    public void storeConfig(String key, String value, boolean updateAllowed) {
        Session session = currentSession();
        Configuration config = (Configuration) session.byId(Configuration.class).load(key);
        if (config == null) {
            LOG.debug("Store new configuration: key='{}', value='{}'", key, value);
            config = new Configuration();
            config.setKey(key);
            config.setValue(value);
            session.save(config);
        } else if (updateAllowed) {
            config.setValue(value);
            session.update(config);
        }
    }

    public List<Configuration> getConfigurationEntries(OptionsConfig options) {
        StringBuilder sbSQL = new StringBuilder("from Configuration");

        if (StringUtils.isBlank(options.getConfig())) {
            LOG.debug("Getting all configuration entries");
        } else {
            LOG.debug("Getting configuration for '{}'", options.getConfig());
        }

        sbSQL.append(options.getSearchString(true));
        sbSQL.append(options.getSortString());

        return currentSession().createQuery(sbSQL.toString()).list();
    }

    public List<Configuration> getConfigurationEntries(String key) {
        OptionsConfig options = new OptionsConfig();
        options.setConfig(key);
        // Make the search exact
        options.setMode("EXACT");
        return getConfigurationEntries(options);
    }

    /**
     * Delete keys from the database
     *
     * @param key
     */
    public void deleteConfig(String key) {
        if (StringUtils.isNotBlank(key)) {
            List<Configuration> configList = getConfigurationEntries(key);
            for (Configuration config : configList) {
                LOG.debug("Deleting key '{}'", key);
                currentSession().delete(config);
            }
            LOG.debug("Successfully deleted all keys for '{}'", key);
        }
    }
}

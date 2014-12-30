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
package org.yamj.core.configuration;

import java.util.*;
import java.util.Map.Entry;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.yamj.core.api.options.OptionsConfig;
import org.yamj.core.database.dao.ConfigDao;
import org.yamj.core.database.model.Configuration;

public class ConfigService {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigService.class);
    private static final String DEFAULT_SPLITTER = ",";
    
    private Map<String, String> cachedProperties = new HashMap<String, String>();

    @Autowired
    private ConfigDao configDao;

    @Required
    public void setConfigDao(ConfigDao configDao) {
        this.configDao = configDao;
    }

    @Required
    public void setCoreProperties(Properties properties) {
        for (Entry<Object, Object> entry : properties.entrySet()) {
            cachedProperties.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
    }

    @PostConstruct
    public void init() throws Exception {
        LOG.info("Initialize config service");
        
        // get stored properties
        Map<String, String> dbConfig = configDao.readConfig();
        // override existing properties with database properties
        cachedProperties.putAll(dbConfig);
        // store back all properties
        configDao.storeConfig(cachedProperties);
    }

    public void reloadCachedProperties() {
        cachedProperties = configDao.readConfig();
    }

    public Map<String, String> getCachedProperties() {
        return cachedProperties;
    }

    public String getProperty(String key) {
        return cachedProperties.get(key);
    }

    public String getProperty(String key, String defaultValue) {
        String value = cachedProperties.get(key);
        return (value == null ? defaultValue : value);
    }

    public List<String> getPropertyAsList(String key, String defaultValue) {
        return this.getPropertyAsList(key, defaultValue, DEFAULT_SPLITTER);
    }

    public List<String> getPropertyAsList(String key, String defaultValue, String splitter) {
        String props = this.getProperty(key, defaultValue);
        return Arrays.asList(props.split(splitter));
    }
    
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = cachedProperties.get(key);
        if (value != null) {
            return Boolean.parseBoolean(value.trim());
        }
        return defaultValue;
    }

    public int getIntProperty(String key, int defaultValue) {
        String value = cachedProperties.get(key);
        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException nfe) {
            }
        }
        return defaultValue;
    }

    public long getLongProperty(String key, long defaultValue) {
        String value = cachedProperties.get(key);
        if (value != null) {
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException nfe) {
            }
        }
        return defaultValue;
    }

    /**
     * Return the key property as a float
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public float getFloatProperty(String key, float defaultValue) {
        String value = cachedProperties.get(key);
        if (value != null) {
            try {
                return Float.parseFloat(value.trim());
            } catch (NumberFormatException nfe) {
            }
        }
        return defaultValue;
    }

    public void setProperty(String key, String value) {
        // first store in database ...
        configDao.storeConfig(key, value, true);
        // ... after that in cached properties
        cachedProperties.put(key, value);
    }

    public void setProperty(String key, boolean value) {
        setProperty(key, Boolean.toString(value));
    }

    public void setProperty(String key, int value) {
        setProperty(key, Integer.toString(value));
    }

    public void setProperty(String key, long value) {
        setProperty(key, Long.toString(value));
    }

    public void setProperty(String key, float value) {
        setProperty(key, Float.toString(value));
    }

    public void deleteProperty(String key) {
        // Delete the config from the database
        configDao.deleteConfig(key);
        // delete the config from the cached properties
        cachedProperties.remove(key);
    }

    /**
     * Get a list of the configuration entries for "key"
     *
     * @param key Can be blank/null or specific key
     * @return
     */
    public List<Configuration> getConfiguration(String key) {
        return configDao.getConfigurationEntries(key);
    }

    /**
     * Get a list of the configuration entries based on the options
     *
     * @param options
     * @return
     */
    public List<Configuration> getConfiguration(OptionsConfig options) {
        return configDao.getConfigurationEntries(options);
    }
}

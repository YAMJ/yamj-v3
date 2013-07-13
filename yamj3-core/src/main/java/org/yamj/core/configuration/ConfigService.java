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
package org.yamj.core.configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.yamj.core.database.dao.ConfigDao;
import org.yamj.core.database.model.Configuration;

public class ConfigService implements InitializingBean {

    @Autowired
    private ConfigDao configDao;
    private Map<String, String> cachedProperties = new HashMap<String, String>();

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

    @Override
    public void afterPropertiesSet() throws Exception {
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
        configDao.storeConfig(key, value);
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

    public Configuration getConfiguration(String key) {
        return configDao.getConfiguration(key);
    }

    public List<Configuration> getConfiguration() {
        return configDao.getConfiguration();
    }
}
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
package org.yamj.core.config;

import static org.yamj.plugin.api.Constants.DEFAULT_SPLITTER;

import java.util.*;
import javax.annotation.PostConstruct;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.core.api.options.OptionsConfig;
import org.yamj.core.database.dao.ConfigDao;
import org.yamj.core.database.model.Configuration;

@Service("configService")
public class ConfigService {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigService.class);
    
    private Map<String, String> cachedProperties = new HashMap<>();

    @Autowired
    private ConfigDao configDao;

    @Required
    @Autowired
    public void setDynamicProperties(Properties dynamicProperties) {
        for (String key : dynamicProperties.stringPropertyNames()) {
            cachedProperties.put(key, dynamicProperties.getProperty(key));
        }
    }

    @PostConstruct
    public void init() {
        LOG.trace("Initialize config service");
        
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

    public String getProperty(final String key) {
        return cachedProperties.get(key);
    }

    public boolean hasProperty(final String key) {
        return cachedProperties.containsKey(key);
    }
    
    public String getProperty(final String key, final String defaultValue) {
        final String value = cachedProperties.get(key);
        return (value == null) ? defaultValue : value;
    }

    public List<String> getPropertyAsList(final String key, final String defaultValue) {
        return this.getPropertyAsList(key, defaultValue, DEFAULT_SPLITTER);
    }

    public List<String> getPropertyAsList(final String key, final String defaultValue, final String splitter) {
        final String props = this.getProperty(key, defaultValue);
        return Arrays.asList(props.split(splitter));
    }
    
    public boolean getBooleanProperty(final String key, final boolean defaultValue) {
        final String value = cachedProperties.get(key);
        if (value != null) {
            return Boolean.parseBoolean(value.trim());
        }
        return defaultValue;
    }

    public int getIntProperty(final String key, final int defaultValue) {
        final String value = cachedProperties.get(key);
        return NumberUtils.toInt(value, defaultValue);
    }

    public long getLongProperty(final String key, final long defaultValue) {
        final String value = cachedProperties.get(key);
        return NumberUtils.toLong(value, defaultValue);
    }

    public float getFloatProperty(final String key, final float defaultValue) {
        String value = cachedProperties.get(key);
        return NumberUtils.toFloat(value, defaultValue);
    }

    public Date getDateProperty(final String key) {
        final long ms  = NumberUtils.toLong(cachedProperties.get(key), -1);
        if (ms < 0) {
            return null;
        }
        return new Date(ms);
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

    public void setProperty(String key, Date date) {
        if (date != null) {
            setProperty(key, Long.toString(date.getTime()));
        }
    }
    
    @Transactional
    public void deleteProperty(String key) {
        // Delete the config from the database
        configDao.deleteConfig(key);
        // delete the config from the cached properties
        cachedProperties.remove(key);
    }

    /**
     * Get a list of the configuration for "key"
     *
     * @param key the configuration key
     * @return
     */
    @Transactional(readOnly = true)
    public Configuration getConfiguration(String key) {
        return configDao.getById(Configuration.class, key);
    }

    /**
     * Get a list of the configuration entries based on the options
     *
     * @param options
     * @return
     */
    @Transactional(readOnly = true)
    public List<Configuration> getConfigurations(OptionsConfig options) {
        return configDao.getConfigurations(options);
    }
}

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
package org.yamj.plugin.api.mockobjects;

import java.util.*;
import org.apache.commons.lang3.math.NumberUtils;
import org.yamj.plugin.api.common.Constants;
import org.yamj.plugin.api.common.PluginConfigService;
import org.yamj.plugin.api.type.JobType;

public class PluginConfigServiceMock implements PluginConfigService {

    private final Properties properties;

    public PluginConfigServiceMock() {
        this.properties = new Properties();
    }

    public PluginConfigServiceMock(Properties properties) {
        this.properties = properties;
    }
    
    @Override
    public void pluginConfiguration(Properties pluginProperties) {
        for (Object key : pluginProperties.keySet()) {
            if (!properties.containsKey(key)) {
                properties.setProperty(key.toString(), pluginProperties.getProperty(key.toString()));
            }
        }
    }

    @Override
    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    @Override
    public List<String> getPropertyAsList(String key, String defaultValue) {
        return this.getPropertyAsList(key, defaultValue, Constants.DEFAULT_SPLITTER);
    }

    @Override
    public List<String> getPropertyAsList(String key, String defaultValue, String splitter) {
        final String props = this.getProperty(key, defaultValue);
        return Arrays.asList(props.split(splitter));
    }

    @Override
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        final String value = this.getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value.trim());
        }
        return defaultValue; 
    }

    @Override
    public int getIntProperty(String key, int defaultValue) {
        return NumberUtils.toInt(getProperty(key), defaultValue);
    }

    @Override
    public long getLongProperty(String key, long defaultValue) {
        return NumberUtils.toLong(getProperty(key), defaultValue);
    }

    @Override
    public float getFloatProperty(String key, float defaultValue) {
        return NumberUtils.toFloat(getProperty(key), defaultValue);
    }

    @Override
    public Date getDateProperty(String key) {
        final long ms  = NumberUtils.toLong(getProperty(key), -1);
        if (ms < 0) {
            return null;
        }
        return new Date(ms);
    }

    @Override
    public boolean isCastScanEnabled(JobType jobType) {
        return true;
    }
}

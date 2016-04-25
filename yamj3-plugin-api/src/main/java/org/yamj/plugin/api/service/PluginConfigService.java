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
package org.yamj.plugin.api.service;

import java.util.Date;
import java.util.List;
import java.util.Properties;
import org.yamj.plugin.api.model.type.JobType;

public interface PluginConfigService {

    void pluginConfiguration(Properties properties);
    
    String getProperty(String key);

    String getProperty(String key, String defaultValue);

    List<String> getPropertyAsList(String key, String defaultValue);

    List<String> getPropertyAsList(String key, String defaultValue, String splitter);
    
    boolean getBooleanProperty(String key, boolean defaultValue);

    int getIntProperty(String key, int defaultValue);

    long getLongProperty(String key, long defaultValue);

    float getFloatProperty(String key, float defaultValue);

    Date getDateProperty(String key);
    
    boolean isCastScanEnabled(JobType jobType);
}

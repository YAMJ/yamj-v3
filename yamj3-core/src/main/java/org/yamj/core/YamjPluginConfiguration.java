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
package org.yamj.core;

import org.yamj.plugin.api.YamjOnlinePlugin;

import java.io.File;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.yamj.api.common.http.PoolingHttpClient;
import org.yamj.core.config.ConfigServiceWrapper;
import org.yamj.plugin.api.YamjPlugin;
import ro.fortsoft.pf4j.*;

@Configuration
public class YamjPluginConfiguration {

    @Autowired
    private ConfigServiceWrapper configServiceWrapper;
    @Autowired
    private PoolingHttpClient poolingHttpClient;
    
    @Bean(destroyMethod="stopPlugins")
    @DependsOn({"configServiceWrapper", "poolingHttpClient"})
    public PluginManager pluginManager() {
        final String yamjHome = System.getProperty("yamj3.home", ".");
        File pluginsDir = new File (yamjHome + "/plugins");
        
        PluginManager pluginManager = new DefaultPluginManager(pluginsDir);
        pluginManager.loadPlugins();
        pluginManager.startPlugins();
        
        for (PluginWrapper wrapper : pluginManager.getPlugins(PluginState.STARTED)) {
            Plugin plugin = wrapper.getPlugin();
            if (plugin instanceof YamjPlugin) {
                ((YamjPlugin)plugin).setConfigService(configServiceWrapper);
            }
            if (plugin instanceof YamjOnlinePlugin) {
                ((YamjOnlinePlugin)plugin).setPoolingHttpClient(poolingHttpClient);
            }
        }
        return pluginManager;
    }
}

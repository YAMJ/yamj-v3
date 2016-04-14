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

import java.io.File;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.yamj.api.common.http.PoolingHttpClient;
import org.yamj.core.config.ConfigServiceWrapper;
import org.yamj.core.config.LocaleService;
import org.yamj.core.service.metadata.online.OnlineScannerService;
import org.yamj.core.service.metadata.online.PluginMovieScanner;
import org.yamj.plugin.api.YamjPlugin;
import org.yamj.plugin.api.metadata.MovieScanner;
import ro.fortsoft.pf4j.*;

@Configuration
public class YamjPluginConfiguration {

    @Autowired
    private ConfigServiceWrapper configServiceWrapper;
    @Autowired
    private LocaleService localeService;
    @Autowired
    private PoolingHttpClient poolingHttpClient;
    @Autowired
    private OnlineScannerService onlineScannerService;
    
    @Bean(destroyMethod="stopPlugins")
    @DependsOn({"configServiceWrapper", "localeService", "onlineScannerService", "poolingHttpClient"})
    public PluginManager pluginManager() {
        final String yamjHome = System.getProperty("yamj3.home", ".");
        File pluginsDir = new File (yamjHome + "/plugins");
        
        // load PlugIns
        PluginManager pluginManager = new DefaultPluginManager(pluginsDir);
        pluginManager.loadPlugins();
        
        // set service within PlugIns
        for (PluginWrapper wrapper : pluginManager.getPlugins()) {
            Plugin plugin = wrapper.getPlugin();
            if (plugin instanceof YamjPlugin) {
                ((YamjPlugin)plugin).setConfigService(configServiceWrapper);
            }
        }

        // start PlugIns
        pluginManager.startPlugins();

        // add movie scanner to online scanner service
        for (MovieScanner movieScanner : pluginManager.getExtensions(MovieScanner.class)) {
            movieScanner.init(configServiceWrapper, poolingHttpClient, localeService.getLocale());
            PluginMovieScanner scanner = new PluginMovieScanner(movieScanner, localeService);
            this.onlineScannerService.registerMetadataScanner(scanner);
        }
        // TODO also for series, person and filmography scanner
        
        return pluginManager;
    }
}

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
package org.yamj.core.service.metadata.online;

import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.yamj.api.common.http.PoolingHttpClient;
import org.yamj.core.config.ConfigServiceWrapper;
import org.yamj.core.config.LocaleService;
import org.yamj.core.service.various.IdentifierService;
import org.yamj.plugin.api.metadata.*;
import ro.fortsoft.pf4j.PluginManager;

/**
 * Just used for initialization of online scanner plugins on startup.
 */
@Component("onlineScannerInitialization")
@DependsOn({"onlineScannerService","pluginManager"})
public class OnlineScannerInitialization {

    private static final Logger LOG = LoggerFactory.getLogger(OnlineScannerInitialization.class);
    
    @Autowired
    private ConfigServiceWrapper configServiceWrapper;
    @Autowired
    private LocaleService localeService;
    @Autowired
    private PoolingHttpClient poolingHttpClient;
    @Autowired
    private OnlineScannerService onlineScannerService;
    @Autowired
    private PluginManager pluginManager;
    @Autowired
    private IdentifierService identifierService;
    
    @PostConstruct
    public void init() {
        LOG.debug("Initialize online scanner with plugins");
        
        // add movie scanner to online scanner service
        for (MovieScanner movieScanner : pluginManager.getExtensions(MovieScanner.class)) {
            movieScanner.init(configServiceWrapper, poolingHttpClient, localeService.getLocale());
            PluginMovieScanner scanner = new PluginMovieScanner(movieScanner, localeService, identifierService);
            this.onlineScannerService.registerMetadataScanner(scanner);
        }
        
        // add series scanner to online scanner service
        for (SeriesScanner seriesScanner : pluginManager.getExtensions(SeriesScanner.class)) {
            seriesScanner.init(configServiceWrapper, poolingHttpClient, localeService.getLocale());
            PluginSeriesScanner scanner = new PluginSeriesScanner(seriesScanner, localeService, identifierService);
            this.onlineScannerService.registerMetadataScanner(scanner);
        }

        // add person scanner to online scanner service
        for (PersonScanner personScanner : pluginManager.getExtensions(PersonScanner.class)) {
            personScanner.init(configServiceWrapper, poolingHttpClient, localeService.getLocale());
            PluginPersonScanner scanner = new PluginPersonScanner(personScanner);
            this.onlineScannerService.registerMetadataScanner(scanner);
        }

        // add filmography scanner to online scanner service
        for (FilmographyScanner filmographyScanner : pluginManager.getExtensions(FilmographyScanner.class)) {
            filmographyScanner.init(configServiceWrapper, poolingHttpClient, localeService.getLocale());
            PluginFilmographyScanner scanner = new PluginFilmographyScanner(filmographyScanner, localeService);
            this.onlineScannerService.registerMetadataScanner(scanner);
        }
    }
}

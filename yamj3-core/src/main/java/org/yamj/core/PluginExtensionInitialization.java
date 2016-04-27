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

import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.yamj.api.common.http.PoolingHttpClient;
import org.yamj.core.config.ConfigServiceWrapper;
import org.yamj.core.config.LocaleService;
import org.yamj.core.service.artwork.ArtworkInitialization;
import org.yamj.core.service.artwork.ArtworkScannerService;
import org.yamj.core.service.artwork.online.*;
import org.yamj.core.service.metadata.online.*;
import org.yamj.core.service.various.IdentifierService;
import org.yamj.plugin.api.*;
import org.yamj.plugin.api.artwork.*;
import org.yamj.plugin.api.metadata.*;
import ro.fortsoft.pf4j.ExtensionPoint;
import ro.fortsoft.pf4j.PluginManager;

@Component("pluginInitialization")
@DependsOn({"localeService", "identifierService", "onlineScannerService", "artworkScannerService"})
public class PluginExtensionInitialization {

    private static final Logger LOG = LoggerFactory.getLogger(ArtworkInitialization.class);

    @Autowired
    private PluginManager pluginManager;
    @Autowired
    private ConfigServiceWrapper congfigServiceWrapper;
    @Autowired
    private LocaleService localeService;
    @Autowired
    private IdentifierService identifierService;
    @Autowired
    private PoolingHttpClient poolingHttpClient;
    @Autowired
    private OnlineScannerService onlineScannerService;
    @Autowired
    private ArtworkScannerService artworkScannerService;
    
    @PostConstruct
    public void init() {
        LOG.debug("Initialize plugin extensions");
        
        // METADATA
        
        // add movie scanner to online scanner service
        for (MovieScanner movieScanner : pluginManager.getExtensions(MovieScanner.class)) {
            initExtensionPoint(movieScanner);
            PluginMovieScanner scanner = new PluginMovieScanner(movieScanner, localeService, identifierService);
            onlineScannerService.registerMetadataScanner(scanner);
        }
        
        // add series scanner to online scanner service
        for (SeriesScanner seriesScanner : pluginManager.getExtensions(SeriesScanner.class)) {
            initExtensionPoint(seriesScanner);
            PluginSeriesScanner scanner = new PluginSeriesScanner(seriesScanner, localeService, identifierService);
            onlineScannerService.registerMetadataScanner(scanner);
        }

        // add person scanner to online scanner service
        for (PersonScanner personScanner : pluginManager.getExtensions(PersonScanner.class)) {
            initExtensionPoint(personScanner);
            PluginPersonScanner scanner = new PluginPersonScanner(personScanner);
            onlineScannerService.registerMetadataScanner(scanner);
        }

        // add filmography scanner to online scanner service
        for (FilmographyScanner filmographyScanner : pluginManager.getExtensions(FilmographyScanner.class)) {
            initExtensionPoint(filmographyScanner);
            PluginFilmographyScanner scanner = new PluginFilmographyScanner(filmographyScanner, localeService);
            onlineScannerService.registerMetadataScanner(scanner);
        }
        
        // ARTWORK
        
        // add movie artwork scanner to artwork scanner service
        for (MovieArtworkScanner movieArtworkScanner : pluginManager.getExtensions(MovieArtworkScanner.class)) {
            initExtensionPoint(movieArtworkScanner);
            PluginMovieArtworkScanner scanner = new PluginMovieArtworkScanner(movieArtworkScanner);
            artworkScannerService.registerArtworkScanner(scanner);
        }
        
        // add series artwork scanner to artwork scanner service
        for (SeriesArtworkScanner seriesArtworkScanner : pluginManager.getExtensions(SeriesArtworkScanner.class)) {
            initExtensionPoint(seriesArtworkScanner);
            PluginSeriesArtworkScanner scanner = new PluginSeriesArtworkScanner(seriesArtworkScanner);
            artworkScannerService.registerArtworkScanner(scanner);
        }

        // add boxed set artwork scanner to artwork scanner service
        for (BoxedSetArtworkScanner boxedSetArtworkScanner : pluginManager.getExtensions(BoxedSetArtworkScanner.class)) {
            initExtensionPoint(boxedSetArtworkScanner);
            PluginBoxedSetArtworkScanner scanner = new PluginBoxedSetArtworkScanner(boxedSetArtworkScanner);
            artworkScannerService.registerArtworkScanner(scanner);
        }

        // add person artwork scanner to artwork scanner service
        for (PersonArtworkScanner personArtworkScanner : pluginManager.getExtensions(PersonArtworkScanner.class)) {
            initExtensionPoint(personArtworkScanner);
            PluginPersonArtworkScanner scanner = new PluginPersonArtworkScanner(personArtworkScanner);
            artworkScannerService.registerArtworkScanner(scanner);
        }
        
    }
    
    private void initExtensionPoint(ExtensionPoint extensionPoint) {
        if (extensionPoint instanceof NeedsConfigService) {
            ((NeedsConfigService)extensionPoint).setConfigService(congfigServiceWrapper);
        }
        if (extensionPoint instanceof NeedsLocaleService) {
            ((NeedsLocaleService)extensionPoint).setLocaleService(localeService);
        }
        if (extensionPoint instanceof NeedsHttpClient) {
            ((NeedsHttpClient)extensionPoint).setHttpClient(poolingHttpClient);
        }
        if (extensionPoint instanceof NeedsMetadataService) {
            ((NeedsMetadataService)extensionPoint).setMetadataService(onlineScannerService);
        }
    }
}

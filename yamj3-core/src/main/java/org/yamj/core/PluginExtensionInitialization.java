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
import org.yamj.core.service.metadata.online.OnlineScannerService;
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
            onlineScannerService.registerMetadataScanner(movieScanner);
        }
        
        // add series scanner to online scanner service
        for (SeriesScanner seriesScanner : pluginManager.getExtensions(SeriesScanner.class)) {
            initExtensionPoint(seriesScanner);
            onlineScannerService.registerMetadataScanner(seriesScanner);
        }

        // add person scanner to online scanner service
        for (PersonScanner personScanner : pluginManager.getExtensions(PersonScanner.class)) {
            initExtensionPoint(personScanner);
            onlineScannerService.registerMetadataScanner(personScanner);
        }

        // add filmography scanner to online scanner service
        for (FilmographyScanner filmographyScanner : pluginManager.getExtensions(FilmographyScanner.class)) {
            initExtensionPoint(filmographyScanner);
            onlineScannerService.registerMetadataScanner(filmographyScanner);
        }
        
        // ARTWORK
        
        // add movie artwork scanner to artwork scanner service
        for (MovieArtworkScanner movieArtworkScanner : pluginManager.getExtensions(MovieArtworkScanner.class)) {
            initExtensionPoint(movieArtworkScanner);
            artworkScannerService.registerArtworkScanner(movieArtworkScanner);
        }
        
        // add series artwork scanner to artwork scanner service
        for (SeriesArtworkScanner seriesArtworkScanner : pluginManager.getExtensions(SeriesArtworkScanner.class)) {
            initExtensionPoint(seriesArtworkScanner);
            artworkScannerService.registerArtworkScanner(seriesArtworkScanner);
        }

        // add person artwork scanner to artwork scanner service
        for (PersonArtworkScanner personArtworkScanner : pluginManager.getExtensions(PersonArtworkScanner.class)) {
            initExtensionPoint(personArtworkScanner);
            artworkScannerService.registerArtworkScanner(personArtworkScanner);
        }

        // add boxed set artwork scanner to artwork scanner service
        for (BoxedSetArtworkScanner boxedSetArtworkScanner : pluginManager.getExtensions(BoxedSetArtworkScanner.class)) {
            initExtensionPoint(boxedSetArtworkScanner);
            artworkScannerService.registerArtworkScanner(boxedSetArtworkScanner);
        }
    }
    
    private void initExtensionPoint(ExtensionPoint extensionPoint) {
        if (extensionPoint instanceof NeedsConfigService) {
            ((NeedsConfigService)extensionPoint).setConfigService(congfigServiceWrapper);
        }
        if (extensionPoint instanceof NeedsLocaleService) {
            ((NeedsLocaleService)extensionPoint).setLocaleService(localeService);
        }
        if (extensionPoint instanceof NeedsMetadataService) {
            ((NeedsMetadataService)extensionPoint).setMetadataService(onlineScannerService);
        }
        if (extensionPoint instanceof NeedsHttpClient) {
            ((NeedsHttpClient)extensionPoint).setHttpClient(poolingHttpClient);
        }
    }
}

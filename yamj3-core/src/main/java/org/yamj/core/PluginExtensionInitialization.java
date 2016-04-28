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
import org.yamj.core.service.metadata.extras.ExtrasScannerService;
import org.yamj.core.service.metadata.online.OnlineScannerService;
import org.yamj.core.service.trailer.TrailerProcessorService;
import org.yamj.core.service.trailer.TrailerScannerService;
import org.yamj.core.service.various.IdentifierService;
import org.yamj.plugin.api.*;
import org.yamj.plugin.api.artwork.ArtworkScanner;
import org.yamj.plugin.api.extras.ExtrasScanner;
import org.yamj.plugin.api.metadata.MetadataScanner;
import org.yamj.plugin.api.trailer.TrailerDownloadBuilder;
import org.yamj.plugin.api.trailer.TrailerScanner;
import ro.fortsoft.pf4j.ExtensionPoint;
import ro.fortsoft.pf4j.PluginManager;

@Component("pluginExtensionInitialization")
@DependsOn({"localeService", "identifierService", "onlineScannerService", "extrasScannerService", 
            "artworkScannerService", "trailerScannerService", "trailerProcessorService"})
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
    @Autowired
    private TrailerScannerService trailerScannerService;
    @Autowired
    private TrailerProcessorService trailerProcessorService;
    @Autowired
    private ExtrasScannerService extrasScannerService;
    
    @PostConstruct
    public void init() {
        LOG.debug("Initialize plugin extensions");
        
        for (MetadataScanner metadataScanner : pluginManager.getExtensions(MetadataScanner.class)) {
            initExtensionPoint(metadataScanner);
            onlineScannerService.registerMetadataScanner(metadataScanner);
        }
        
        for (ExtrasScanner extrasScanner : pluginManager.getExtensions(ExtrasScanner.class)) {
            initExtensionPoint(extrasScanner);
            extrasScannerService.registerExtraScanner(extrasScanner);
        }

        for (ArtworkScanner artworkScanner : pluginManager.getExtensions(ArtworkScanner.class)) {
            initExtensionPoint(artworkScanner);
            artworkScannerService.registerArtworkScanner(artworkScanner);
        }

        for (TrailerScanner trailerScanner : pluginManager.getExtensions(TrailerScanner.class)) {
            initExtensionPoint(trailerScanner);
            trailerScannerService.registerTrailerScanner(trailerScanner);
        }

        for (TrailerDownloadBuilder downloadBuilder : pluginManager.getExtensions(TrailerDownloadBuilder.class)) {
            initExtensionPoint(downloadBuilder);
            trailerProcessorService.registerTrailerDownloadBuilder(downloadBuilder);
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

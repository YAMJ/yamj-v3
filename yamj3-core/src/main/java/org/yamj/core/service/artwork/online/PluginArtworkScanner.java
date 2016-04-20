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
package org.yamj.core.service.artwork.online;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.yamj.core.config.LocaleService;
import org.yamj.core.service.artwork.ArtworkDetailDTO;
import org.yamj.plugin.api.artwork.dto.ArtworkDTO;

public abstract class PluginArtworkScanner implements IArtworkScanner {

    private final LocaleService localeService;
    
    public PluginArtworkScanner(LocaleService localeService) {
        this.localeService = localeService;
    }
    
    protected List<ArtworkDetailDTO> createArtworkDetails(List<ArtworkDTO> artworks) {
        if (CollectionUtils.isEmpty(artworks)) {
            return Collections.emptyList();
        }
        
        List<ArtworkDetailDTO> result = new ArrayList<>(artworks.size());
        for (ArtworkDTO artwork : artworks) {
            ArtworkDetailDTO detail = new ArtworkDetailDTO(getScannerName(), artwork.getUrl(), artwork.getHashCode(), artwork.getImageType());
            detail.setRating(artwork.getRating());
            detail.setLanguageCode(localeService.findLanguageCode(artwork.getLanguage()));
            result.add(detail);
        }
        return result;
    }
}

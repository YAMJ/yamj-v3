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
package org.yamj.core.api.json;

import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.api.model.ApiStatus;
import org.yamj.core.api.model.dto.ApiArtworkDTO;
import org.yamj.core.api.options.OptionsIndexArtwork;
import org.yamj.core.api.wrapper.ApiWrapperList;
import org.yamj.core.api.wrapper.ApiWrapperSingle;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.database.service.CommonStorageService;
import org.yamj.core.database.service.JsonApiStorageService;
import org.yamj.core.scheduling.ArtworkProcessScheduler;
import org.yamj.core.service.artwork.ArtworkProcessorService;
import org.yamj.core.service.file.FileStorageService;

@RestController
@RequestMapping(value = "/api/artwork/**", produces = "application/json; charset=utf-8")
public class ArtworkController {

    private static final Logger LOG = LoggerFactory.getLogger(ArtworkController.class);
    @Autowired
    private JsonApiStorageService jsonApiStorageService;
    @Autowired
    private CommonStorageService commonStorageService;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private ArtworkProcessorService artworkProcessorService;
    @Autowired
    private ArtworkProcessScheduler artworkProcessScheduler; 
    
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ApiWrapperSingle<ApiArtworkDTO> getArtwork(@PathVariable("id") Long id) {
        LOG.debug("Attempting to retrieve artwork with ID {}", id);

        ApiWrapperSingle<ApiArtworkDTO> wrapper = new ApiWrapperSingle<>();
        wrapper.setResult(jsonApiStorageService.getArtworkById(id));
        return wrapper;
    }

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public ApiWrapperList<ApiArtworkDTO> getArtworkList(@ModelAttribute("options") OptionsIndexArtwork options) {
        LOG.debug("Artwork list - Options: {}", options);
        
        ApiWrapperList<ApiArtworkDTO> wrapper = new ApiWrapperList<>();
        wrapper.setOptions(options);
        wrapper.setResults(jsonApiStorageService.getArtworkList(wrapper));
        return wrapper;
    }

    /**
     * Mark a located artwork as ignored.
     *
     * @param options
     * @return
     */
    @RequestMapping(value = "/located/ignore/{id}", method = {RequestMethod.GET, RequestMethod.PUT})
    public ApiStatus ignoreLocatedArtwork(@PathVariable("id") Long id) {
        if (id <= 0L) {
            return ApiStatus.INVALID_ID;
        }
        
        LOG.info("Ignore located artwork with ID {}", id);

        ApiStatus status;
        Set<String> filesToDelete = this.commonStorageService.ignoreArtworkLocated(id);
        if (filesToDelete != null) {
            this.fileStorageService.deleteStorageFiles(filesToDelete);
            status = ApiStatus.ok("Successfully marked located artwork " + id + " as ignored");
        } else {
            status = ApiStatus.notFound("Located artwork not found " + id);
        }
        return status;
    }

    @RequestMapping(value = "/add/{artwork}/{type}/{id}", method=RequestMethod.POST)
    public ApiStatus addImage(@PathVariable("artwork") String artwork, @PathVariable("type") String type, @PathVariable("id") Long id, @RequestParam MultipartFile image) {
        if (id <= 0L) {
            return ApiStatus.INVALID_ID;
        }

        final ArtworkType artworkType = ArtworkType.fromString(artwork);
        if (ArtworkType.UNKNOWN == artworkType) {
            return ApiStatus.badRequest("Invalid artwork type '" + artwork + "'");
        }
        
        final MetaDataType metaDataType = MetaDataType.fromString(type);
        if (!metaDataType.isWithArtwork()) {
            return ApiStatus.badRequest("Invalid meta data type '" + type + "' for artwork");
        }
        
        ApiStatus apiStatus = this.artworkProcessorService.addArtwork(artworkType, metaDataType, id, image);
        if (apiStatus.isSuccessful())  {
            this.artworkProcessScheduler.triggerProcess();
        }
        return apiStatus;
    }
}

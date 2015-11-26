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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.api.model.ApiStatus;
import org.yamj.core.api.model.dto.ApiArtworkDTO;
import org.yamj.core.api.options.OptionsId;
import org.yamj.core.api.options.OptionsIndexArtwork;
import org.yamj.core.api.wrapper.ApiWrapperList;
import org.yamj.core.api.wrapper.ApiWrapperSingle;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.database.service.ArtworkStorageService;
import org.yamj.core.database.service.CommonStorageService;
import org.yamj.core.database.service.JsonApiStorageService;
import org.yamj.core.service.ArtworkProcessScheduler;
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
    private ArtworkStorageService artworkStorageService;
    @Autowired
    private ArtworkProcessorService artworkProcessorService;
    @Autowired
    private ArtworkProcessScheduler artworkProcessScheduler; 
    
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ApiWrapperSingle<ApiArtworkDTO> getArtwork(@PathVariable Long id) {
        ApiWrapperSingle<ApiArtworkDTO> wrapper = new ApiWrapperSingle<>();

        LOG.info("Attempting to retrieve artwork with id '{}'", id);
        ApiArtworkDTO artwork = jsonApiStorageService.getArtworkById(id);
        LOG.info("Artwork: {}", artwork);

        // Add the result to the wrapper
        wrapper.setResult(artwork);
        wrapper.setStatusCheck();

        return wrapper;
    }

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public ApiWrapperList<ApiArtworkDTO> getArtworkList(@ModelAttribute("options") OptionsIndexArtwork options) {
        LOG.info("INDEX: Artwork list - Options: {}", options.toString());
        ApiWrapperList<ApiArtworkDTO> wrapper = new ApiWrapperList<>();
        wrapper.setOptions(options);
        wrapper.setResults(jsonApiStorageService.getArtworkList(wrapper));
        wrapper.setStatusCheck();

        return wrapper;
    }

    /**
     * Mark a located artwork as ignored.
     *
     * @param options
     * @return
     */
    @RequestMapping(value = "/located/ignore/{id}", method = RequestMethod.GET)
    public ApiStatus ignoreLocatedArtwork(@ModelAttribute("options") OptionsId options) {
        ApiStatus status = new ApiStatus();
        Long id = options.getId();
        if (id != null && id > 0L) {
            LOG.info("Ignore located artwork '{}'", id);
            Set<String> filesToDelete = this.commonStorageService.ignoreArtworkLocated(id);
            if (filesToDelete != null) {
                this.fileStorageService.deleteStorageFiles(filesToDelete);
                status.setStatus(200);
                status.setMessage("Successfully marked located artwork '" + id + "' as ignored");
            } else {
                status.setStatus(400);
                status.setMessage("Located artwork not found '" + id + "'");
            }
        } else {
            status.setStatus(400);
            status.setMessage("Invalid located artwork id specified");
        }
        return status;
    }

    @RequestMapping(value = "/add/{artwork}/{type}/{id}", method=RequestMethod.POST)
    public ApiStatus addImage(@PathVariable("artwork") String artwork, @PathVariable("type") String type, @PathVariable("id") Long id, @RequestParam MultipartFile image) {
        final ArtworkType artworkType = ArtworkType.fromString(artwork);
        if (ArtworkType.UNKNOWN == artworkType) {
            return new ApiStatus(415, "Invalid artwork type '" + artwork + "'");
        }
        
        final MetaDataType metaDataType = MetaDataType.fromString(type);
        if (!(metaDataType.isRealMetaData() || MetaDataType.BOXSET == metaDataType)) {
            return new ApiStatus(415, "Invalid meta data type '" + type + "'");
        }
        
        if (id == null || id.longValue() <= 0) {
            return new ApiStatus(415, "Invalid id '" + id + "'");
        }
        
        // find matching artwork id
        Long artworkId = this.artworkStorageService.getArtworkId(artworkType, metaDataType, id);
        if (artworkId == null) {
            return new ApiStatus(400, "No matching artwork found");
        }
        
        ApiStatus apiStatus = this.artworkProcessorService.uploadImage(artworkId, image);
        if (apiStatus.isSuccessful()) this.artworkProcessScheduler.triggerProcess();
        return apiStatus;
    }
}

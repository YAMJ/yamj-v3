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

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.api.model.ApiStatus;
import org.yamj.core.api.model.dto.ApiArtworkDTO;
import org.yamj.core.api.model.dto.ApiArtworkProfileDTO;
import org.yamj.core.api.options.OptionsIndexArtwork;
import org.yamj.core.api.wrapper.ApiWrapperList;
import org.yamj.core.api.wrapper.ApiWrapperSingle;
import org.yamj.core.database.model.ArtworkProfile;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.database.service.ArtworkStorageService;
import org.yamj.core.database.service.CommonStorageService;
import org.yamj.core.database.service.JsonApiStorageService;
import org.yamj.core.scheduling.ArtworkProcessScheduler;
import org.yamj.core.service.artwork.ArtworkLocatedProcessorService;
import org.yamj.core.service.artwork.ArtworkUploadService;
import org.yamj.core.service.artwork.ImageDTO;
import org.yamj.core.service.file.FileStorageService;

@RestController
@RequestMapping(value = "/api/artwork", produces = "application/json; charset=utf-8")
public class ArtworkController {

    private static final Logger LOG = LoggerFactory.getLogger(ArtworkController.class);

    @Autowired
    private JsonApiStorageService jsonApiStorageService;
    @Autowired
    private CommonStorageService commonStorageService;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private ArtworkProcessScheduler artworkProcessScheduler; 
    @Autowired
    private ArtworkUploadService artworkUploadService;
    @Autowired
    private ArtworkStorageService artworkStorageService;
    @Autowired
    private ArtworkLocatedProcessorService artworkLocatedProcessorService;
    
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

    @RequestMapping(value = "/profiles", method = RequestMethod.GET)
    public ApiWrapperList<ApiArtworkProfileDTO> getArtworkProfiles() {
        LOG.debug("Artwork profiles list");
        
        List<ApiArtworkProfileDTO> results = new ArrayList<>();
        for (ArtworkProfile profile : artworkStorageService.getAllArtworkProfiles()) {
            ApiArtworkProfileDTO dto = new ApiArtworkProfileDTO();
            dto.setId(profile.getId());
            dto.setName(profile.getProfileName());
            dto.setArtworkType(profile.getArtworkType());
            dto.setMetaDataType(profile.getMetaDataType());
            dto.setWidth(profile.getWidth());
            dto.setHeight(profile.getHeight());
            dto.setScalingType(profile.getScalingType());
            dto.setPreProcess(profile.isPreProcess());
            dto.setReflection(profile.isReflection());
            dto.setRoundedCorners(profile.isRoundedCorners());
            results.add(dto);
        }
        
        ApiWrapperList<ApiArtworkProfileDTO> wrapper = new ApiWrapperList<>();
        wrapper.setResults(results);
        return wrapper;
    }

    @RequestMapping(value = "/regenerate/{id}", method = RequestMethod.GET)
    public ApiStatus regenerateImagesByProfile(@PathVariable("id") Long id) {
        LOG.debug("Attempting to regenerate images for profile ID {}", id);

        int count = this.artworkStorageService.generateImagesForProfile(id);
        if (count > 0) {
            LOG.debug("Trigger regeneration of {} images", count);
            artworkProcessScheduler.trigger();
            return ApiStatus.ok("Trigger regeneration of "+count+" images");
        }
        
        return ApiStatus.ok("No image regeneration needed");
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
        
        ApiStatus apiStatus = this.artworkUploadService.uploadArtwork(artworkType, metaDataType, id, image);
        if (apiStatus.isSuccessful())  {
            this.artworkProcessScheduler.trigger();
        }
        return apiStatus;
    }
    
    @RequestMapping(value = "/get/{profile}/{id}", method=RequestMethod.GET, produces = {MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE})
    public ResponseEntity<byte[]> getImage(@PathVariable("profile") String profile, @PathVariable("id") Long id) {
        final long start = System.currentTimeMillis();
        try {
            ImageDTO image = this.artworkLocatedProcessorService.getImage(id, profile);
            if (image == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            
            try (FileInputStream fos = new FileInputStream(image.getResource())) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(image.getMediaType());
                return new ResponseEntity<>(IOUtils.toByteArray(fos), headers, HttpStatus.OK);
            }
        } catch (Exception ex) {
            LOG.warn("Failed to get image for ID {} and profile '{}': {}", id, profile, ex.getMessage());
            LOG.trace("Image retrieval error", ex);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } finally {
            LOG.trace("Image generation took {} ms", System.currentTimeMillis()-start);
        }
    }
}

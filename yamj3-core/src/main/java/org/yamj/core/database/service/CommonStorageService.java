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
package org.yamj.core.database.service;

import static org.yamj.common.type.StatusType.*;
import static org.yamj.core.CachingNames.*;
import static org.yamj.core.database.model.type.FileType.VIDEO;

import java.util.*;
import java.util.Map.Entry;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.core.database.dao.StagingDao;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.dto.DeletionDTO;
import org.yamj.core.service.artwork.ArtworkStorageTools;
import org.yamj.core.service.file.FileStorageService;
import org.yamj.core.service.file.StorageType;
import org.yamj.core.service.various.StagingService;
import org.yamj.core.tools.WatchedDTO;
import org.yamj.core.tools.YamjTools;

@Service("commonStorageService")
public class CommonStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(CommonStorageService.class);
    private static final String DELETE_MESSAGE = "Delete: {}";
    
    @Autowired
    private StagingDao stagingDao;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private StagingService stagingService;

    @Transactional(readOnly = true)
    public List<Long> getStageFilesForDeletion() {
        return stagingDao.namedQuery(StageFile.QUERY_FOR_DELETION);
    }

    /**
     * Delete a stage file and all associated entities.
     *
     * @param id
     * @return list of cached file names which must be deleted also
     */
    @Transactional
    @CacheEvict(value=DB_STAGEFILE, key="#id")
    public Set<String> deleteStageFile(Long id) {
        // get the stage file
        StageFile stageFile = this.stagingDao.getById(StageFile.class, id);

        // check stage file
        if (stageFile == null) {
            // not found
            return Collections.emptySet();
        }
        if (!stageFile.isDeleted()) {
            // status must still be DELETED
            return Collections.emptySet();
        }

        Set<String> filesToDelete;
        switch (stageFile.getFileType()) {
            case VIDEO:
                filesToDelete = this.deleteVideoStageFile(stageFile);
                break;
            case IMAGE:
                filesToDelete = this.deleteImageStageFile(stageFile);
                break;
            case WATCHED:
                this.deleteWatchedStageFile(stageFile);
                filesToDelete = Collections.emptySet();
                break;
            default:
                this.delete(stageFile);
                filesToDelete = Collections.emptySet();
                break;
        }
        return filesToDelete;
    }

    private Set<String> deleteVideoStageFile(StageFile stageFile) {
        Set<String> filesToDelete = new HashSet<>();

        // delete the media file if no other stage files are present
        MediaFile mediaFile = stageFile.getMediaFile();
        if (mediaFile != null) {
            mediaFile.getStageFiles().remove(stageFile);
            if (CollectionUtils.isEmpty(mediaFile.getStageFiles())) {
                this.delete(mediaFile, filesToDelete);
            } else {
                // mark first duplicate as DONE and reset media file status
                for (StageFile check : mediaFile.getStageFiles()) {
                    if (check.isDuplicate()) {
                        check.setStatus(DONE);

                        // update watched file marker
                        this.stagingService.updateWatchedFile(mediaFile, check);
                        
                        // media file needs an update
                        mediaFile.setStatus(UPDATED);

                        for (VideoData videoData : mediaFile.getVideoDatas()) {
                            WatchedDTO watchedDTO = YamjTools.getWatchedDTO(videoData);
                            videoData.setWatched(watchedDTO.isWatched(), watchedDTO.getWatchedDate());
                        }
                        
                        // break the loop; so that just one duplicate is processed
                        break;
                    }
                }
            }
        }

        // delete attached artwork
        for (ArtworkLocated located : stageFile.getArtworkLocated()) {
            Artwork artwork = located.getArtwork();
            artwork.getArtworkLocated().remove(located);
            this.delete(artwork, located, filesToDelete);
        }

        // delete the stage file
        this.delete(stageFile);

        return filesToDelete;
    }

    private Set<String> deleteImageStageFile(StageFile stageFile) {
        Set<String> filesToDelete = new HashSet<>();

        for (ArtworkLocated located : stageFile.getArtworkLocated()) {
            Artwork artwork = located.getArtwork();
            artwork.getArtworkLocated().remove(located);
            this.delete(artwork, located, filesToDelete);

            // if no located artwork exists anymore then set status of artwork to NEW
            if (CollectionUtils.isEmpty(located.getArtwork().getArtworkLocated())) {
                artwork.setStatus(NEW);
            }
        }

        // delete stage file
        this.delete(stageFile);

        return filesToDelete;
    }

    private void deleteWatchedStageFile(StageFile watchedFile) {
        // set watched status for video file(s)
        for (StageFile videoFile : this.stagingService.findWatchedVideoFiles(watchedFile)) {
            this.toogleWatchedStatus(videoFile, false, false);
        }

        // delete watched file
        this.delete(watchedFile);
    }

    private void delete(StageFile stageFile) {
        LOG.debug(DELETE_MESSAGE, stageFile);

        StageDirectory stageDirectory = stageFile.getStageDirectory();

        // just delete the stage file
        this.stagingDao.deleteEntity(stageFile);
        stageDirectory.getStageFiles().remove(stageFile);

        // check and delete stage directory
        this.delete(stageDirectory);
    }

    /**
     * Delete stage file recursivly.
     *
     * @param stageDirectory
     */
    private void delete(StageDirectory stageDirectory) {
        if (stageDirectory == null) {
            return;
        } else if (CollectionUtils.isNotEmpty(stageDirectory.getStageFiles())) {
            return;
        } else if (CollectionUtils.isNotEmpty(this.stagingDao.getChildDirectories(stageDirectory))) {
            return;
        }

        LOG.debug("Delete empty directory:  {}", stageDirectory);
        this.stagingDao.deleteEntity(stageDirectory);

        // recurse to parents
        this.delete(stageDirectory.getParentDirectory());
    }

    private void delete(MediaFile mediaFile, Set<String> filesToDelete) {
        LOG.debug(DELETE_MESSAGE, mediaFile);

        // delete video data if this is the only media file
        for (VideoData videoData : mediaFile.getVideoDatas()) {
            if (videoData.getMediaFiles().size() == 1) {
                // video data has only this media file
                this.delete(videoData, filesToDelete);
            } else if (videoData.getMediaFiles().size() > 1) {
                // reset watched flag on video data
                videoData.getMediaFiles().remove(mediaFile);
                
                WatchedDTO watchedDTO = YamjTools.getWatchedDTO(videoData);
                videoData.setWatched(watchedDTO.isWatched(), watchedDTO.getWatchedDate());
                this.stagingDao.updateEntity(videoData);
            }
        }

        // delete media file
        mediaFile.getVideoDatas().clear();
        this.stagingDao.deleteEntity(mediaFile);
    }

    private void delete(VideoData videoData, Set<String> filesToDelete) {
        LOG.debug(DELETE_MESSAGE, videoData);

        Season season = videoData.getSeason();
        if (season != null) {
            season.getVideoDatas().remove(videoData);
            if (CollectionUtils.isEmpty(season.getVideoDatas())) {
                this.delete(season, filesToDelete);
            }
        }

        // delete artwork
        for (Artwork artwork : videoData.getArtworks()) {
            this.delete(artwork, filesToDelete);
        }

        this.stagingDao.deleteEntity(videoData);
    }

    private void delete(Season season, Set<String> filesToDelete) {
        LOG.debug(DELETE_MESSAGE, season);

        Series series = season.getSeries();
        series.getSeasons().remove(season);
        if (CollectionUtils.isEmpty(season.getVideoDatas())) {
            this.delete(series, filesToDelete);
        }

        // delete artwork
        for (Artwork artwork : season.getArtworks()) {
            this.delete(artwork, filesToDelete);
        }

        this.stagingDao.deleteEntity(season);
    }

    private void delete(Series series, Set<String> filesToDelete) {
        LOG.debug(DELETE_MESSAGE, series);

        // delete artwork
        for (Artwork artwork : series.getArtworks()) {
            this.delete(artwork, filesToDelete);
        }

        this.stagingDao.deleteEntity(series);
    }

    private void delete(Artwork artwork, Set<String> filesToDelete) {
        if (artwork == null) {
            return;
        }

        for (ArtworkLocated located : artwork.getArtworkLocated()) {
            this.delete(artwork, located, filesToDelete);
        }

        this.stagingDao.deleteEntity(artwork);
    }

    private void delete(Artwork artwork, ArtworkLocated located, Set<String> filesToDelete) {
        final StorageType storageType = ArtworkStorageTools.getStorageType(artwork);

        // delete generated files
        for (ArtworkGenerated generated : located.getGeneratedArtworks()) {
            this.delete(generated, storageType, filesToDelete);
        }

        // delete located file
        if (located.isCached()) {
            String filename = FilenameUtils.concat(located.getCacheDirectory(), located.getCacheFilename());
            filesToDelete.add(this.fileStorageService.getStorageDir(storageType, filename));
        }

        this.stagingDao.deleteEntity(located);
    }

    private void delete(ArtworkGenerated generated, StorageType storageType, Set<String> filesToDelete) {
        String filename = FilenameUtils.concat(generated.getCacheDirectory(), generated.getCacheFilename());
        filesToDelete.add(this.fileStorageService.getStorageDir(storageType, filename));
        this.stagingDao.deleteEntity(generated);
    }

    @Transactional(readOnly = true)
    public List<Long> getArtworkLocatedForDeletion() {
        return stagingDao.namedQuery(ArtworkLocated.QUERY_FOR_DELETION);
    }

    @Transactional
    public DeletionDTO deleteArtworkLocated(Long id) {
        ArtworkLocated located = this.stagingDao.getById(ArtworkLocated.class, id);
        LOG.debug(DELETE_MESSAGE, located);

        Set<String> filesToDelete = new HashSet<>();
        boolean updateTrigger = false;
        if (located != null) {
            Artwork artwork = located.getArtwork();
            artwork.getArtworkLocated().remove(located);
            this.delete(artwork, located, filesToDelete);

            // if no located artwork exists anymore then set status of artwork to NEW
            if (CollectionUtils.isEmpty(located.getArtwork().getArtworkLocated())) {
                artwork.setStatus(NEW);
                updateTrigger = true;
            }
        }

        return new DeletionDTO(filesToDelete, updateTrigger);
    }

    @Transactional(readOnly = true)
    public List<Long> getOrphanPersons() {
        return this.stagingDao.namedQuery(Person.QUERY_ORPHANS);
    }

    @Transactional
    @CacheEvict(value=DB_PERSON, key="#id")
    public Set<String> deletePerson(Long id) {
        Set<String> filesToDelete = new HashSet<>();
        Person person = this.stagingDao.getById(Person.class, id);

        LOG.debug(DELETE_MESSAGE, person);

        if (person != null) {
            this.delete(person.getPhoto(), filesToDelete);
            this.stagingDao.deleteEntity(person);
        }

        return filesToDelete;
    }

    @Transactional(readOnly = true)
    public List<Long> getOrphanBoxedSets() {
        return this.stagingDao.namedQuery(BoxedSet.QUERY_ORPHANS);
    }

    @Transactional
    @CacheEvict(value=DB_BOXEDSET, key="#id")
    public Set<String> deleteBoxedSet(Long id) {
        Set<String> filesToDelete = new HashSet<>();
        BoxedSet boxedSet = this.stagingDao.getById(BoxedSet.class, id);

        LOG.debug(DELETE_MESSAGE, boxedSet);

        if (boxedSet != null) {
            // delete artwork
            for (Artwork artwork : boxedSet.getArtworks()) {
                this.delete(artwork, filesToDelete);
            }

            this.stagingDao.deleteEntity(boxedSet);
        }

        return filesToDelete;
    }

    @Transactional
    public Set<String> ignoreArtworkLocated(Long id) {
        ArtworkLocated located = this.stagingDao.getById(ArtworkLocated.class, id);
        if (located != null) {
            final StorageType storageType = ArtworkStorageTools.getStorageType(located);

            Set<String> filesToDelete = new HashSet<>();
            // delete generated files
            for (ArtworkGenerated generated : located.getGeneratedArtworks()) {
                this.delete(generated, storageType, filesToDelete);
            }

            located.getGeneratedArtworks().clear();
            located.setStatus(IGNORE);
            stagingDao.updateEntity(located);
            return filesToDelete;
        }
        return null; // NOSONAR
    }

    @Transactional
    public boolean toogleWatchedStatus(Long id, boolean watched, boolean apiCall) {
        StageFile stageFile = this.stagingDao.getStageFile(id);
        return this.toogleWatchedStatus(stageFile, watched, apiCall);
    }

    @Transactional
    public boolean toogleWatchedStatus(final StageFile videoFile, final boolean watched, final boolean apiCall) {
        if (videoFile == null || !VIDEO.equals(videoFile.getFileType()) || videoFile.isDuplicate()) {
            return false;
        }

        MediaFile mediaFile = videoFile.getMediaFile();
        if (mediaFile == null) {
            return false;
        }
        
        // update media file
        boolean marked;
        if (apiCall) {
            mediaFile.setWatchedApi(watched, DateTime.now().withMillisOfSecond(0).toDate());
            marked = mediaFile.isWatchedApi();
        } else {
            // update watched file marker
            this.stagingService.updateWatchedFile(mediaFile, videoFile);
            marked = mediaFile.isWatchedFile();
        }
        
        LOG.debug("Mark media file as {} {}: {}", apiCall?"api":"file", marked?"watched":"unwatched", mediaFile);
        this.stagingDao.updateEntity(mediaFile);

        if (mediaFile.isExtra()) {
            LOG.trace("Media file is an extra where no watched status will be populated: {}", mediaFile);
        } else {
            // determine watch status for each video data
            for (VideoData videoData : mediaFile.getVideoDatas()) {
                WatchedDTO watchedDTO = YamjTools.getWatchedDTO(videoData);
                videoData.setWatched(watchedDTO.isWatched(), watchedDTO.getWatchedDate());
                this.stagingDao.updateEntity(videoData);
            }
        }
        
        return true;
    }

    @Transactional
    @CacheEvict(value=DB_GENRE, allEntries=true)
    public void updateGenresXml(Map<String, String> subGenres) {
        Map<String, Object> params = new HashMap<>();
        params.put("subGenres", subGenres.keySet());
        this.stagingDao.executeUpdate(Genre.UPDATE_TARGET_XML_CLEAN, params);
        
        for (Entry<String, String> entry : subGenres.entrySet()) {
            params.clear();
            params.put("subGenre", entry.getKey());
            params.put("targetXml", entry.getValue());
            this.stagingDao.executeUpdate(Genre.UPDATE_TARGET_XML_SET, params);
        }
    }

    @Transactional
    @CacheEvict(value=DB_GENRE, allEntries=true)
    public int deleteOrphanGenres() {
        return this.stagingDao.executeUpdate(Genre.DELETE_ORPHANS);
    }

    @Transactional
    @CacheEvict(value=DB_STUDIO, allEntries=true)
    public int deleteOrphanStudios() {
        return this.stagingDao.executeUpdate(Studio.DELETE_ORPHANS);
    }

    @Transactional
    @CacheEvict(value=DB_COUNTRY, allEntries=true)
    public int deleteOrphanCountries() {
        return this.stagingDao.executeUpdate(Country.DELETE_ORPHANS);
    }

    @Transactional
    @CacheEvict(value=DB_CERTIFICATION, allEntries=true)
    public int deleteOrphanCertifications() {
        return this.stagingDao.executeUpdate(Certification.DELETE_ORPHANS);
    }

    @Transactional(readOnly = true)
    public List<Long> getTrailersToDelete() {
        return stagingDao.namedQuery(Trailer.QUERY_FOR_DELETION);
    }

    @Transactional
    public String deleteTrailer(Long id) {
        // get the trailer
        Trailer trailer = this.stagingDao.getById(Trailer.class, id);

        String fileToDelete = null;
        if (trailer != null) {
            if (StringUtils.isNotBlank(trailer.getCacheFilename())) {
                fileToDelete = this.fileStorageService.getStorageDir(StorageType.TRAILER, trailer.getFullCacheFilename());
            }
            stagingDao.deleteEntity(trailer);
        }
        return fileToDelete;
    }
}

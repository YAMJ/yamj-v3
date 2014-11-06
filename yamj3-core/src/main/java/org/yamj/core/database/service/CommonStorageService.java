/*
 *      Copyright (c) 2004-2014 YAMJ Members
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

import java.util.*;
import java.util.Map.Entry;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.dao.StagingDao;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.database.model.type.FileType;
import org.yamj.core.service.file.FileStorageService;
import org.yamj.core.service.file.StorageType;
import org.yamj.core.service.metadata.tools.MetadataTools;
import org.yamj.core.service.staging.StagingService;

@Service("commonStorageService")
public class CommonStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(CommonStorageService.class);
    
    @Autowired
    private StagingDao stagingDao;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private StagingService stagingService;

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public List<Long> getStageFilesToDelete() {
        final StringBuilder sb = new StringBuilder();
        sb.append("SELECT f.id FROM StageFile f ");
        sb.append("WHERE f.status = :delete " );

        Map<String,Object> params = Collections.singletonMap("delete", (Object)StatusType.DELETED);
        return stagingDao.findByNamedParameters(sb, params);
    }
    
    /**
     * Delete a stage file and all associated entities.
     * 
     * @param stageFile
     * @return list of cached file names which must be deleted also
     */
    @Transactional
    public Set<String> deleteStageFile(Long id) {
        // get the stage file
        StageFile stageFile = this.stagingDao.getStageFile(id);
        
        // check stage file
        if (stageFile == null) {
            // not found
            return Collections.emptySet();
        } if (StatusType.DELETED != stageFile.getStatus()) {
            // status must still be DELETE
            return Collections.emptySet();
        }
        
        Set<String> filesToDelete;
        switch(stageFile.getFileType()) {
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
        Set<String> filesToDelete = new HashSet<String>(); 

        // delete the media file if no other stage files are present
        MediaFile mediaFile = stageFile.getMediaFile();
        if (mediaFile != null) {
            mediaFile.getStageFiles().remove(stageFile);
            if (CollectionUtils.isEmpty(mediaFile.getStageFiles())) {
                this.delete(mediaFile, filesToDelete);
            } else {
                // mark first duplicate as DONE and reset media file status
                for (StageFile check : mediaFile.getStageFiles()) {
                    if (StatusType.DUPLICATE.equals(check.getStatus())) {
                        check.setStatus(StatusType.DONE);
                        this.stagingDao.updateEntity(check);
                        
                        // reset watched file
                        boolean watchedFile = this.stagingService.isWatchedVideoFile(check);
                        mediaFile.setWatchedFile(watchedFile);
                        mediaFile.setStatus(StatusType.UPDATED);
                        this.stagingDao.updateEntity(mediaFile);
                        
                        for (VideoData videoData : mediaFile.getVideoDatas()) {
                            watchedFile = MetadataTools.allMediaFilesWatched(videoData, false);
                            if (videoData.isWatchedFile() != watchedFile) {
                                videoData.setWatchedFile(watchedFile);
                                this.stagingDao.updateEntity(videoData);
                            }

                            boolean watchedApi = MetadataTools.allMediaFilesWatched(videoData, true);
                            if (videoData.isWatchedApi() != watchedApi) {
                                videoData.setWatchedApi(watchedFile);
                                this.stagingDao.updateEntity(videoData);
                            }
                        }
                        // break the loop; so that just one duplicate is processed
                        break;
                    }
                }
            }
            
        }
        
        // delete the stage file
        this.delete(stageFile);

        return filesToDelete;
    }

    private Set<String> deleteImageStageFile(StageFile stageFile) {
        Set<String> filesToDelete = new HashSet<String>();
        
        for (ArtworkLocated located : stageFile.getArtworkLocated()) {
            Artwork artwork = located.getArtwork();
            this.delete(artwork, located, filesToDelete);

            // if no located artwork exists anymore then set status of artwork to NEW 
            if (CollectionUtils.isEmpty(located.getArtwork().getArtworkLocated())) {
                artwork.setStatus(StatusType.NEW);
                this.stagingDao.updateEntity(artwork);
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
        LOG.debug("Delete: {}", stageFile);
        
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
        LOG.debug("Delete: {}", mediaFile);
 
        // delete video data if this is the only media file
        for (VideoData videoData : mediaFile.getVideoDatas()) {
            if (videoData.getMediaFiles().size() == 1) {
                // video data has only this media file
                this.delete(videoData, filesToDelete);
            } else if (videoData.getMediaFiles().size() > 1) {
                // reset watched flag on video data
                videoData.getMediaFiles().remove(mediaFile);
                
                boolean watchedFile = MetadataTools.allMediaFilesWatched(videoData, false);
                if (videoData.isWatchedFile() != watchedFile) {
                    videoData.setWatchedFile(watchedFile);
                    this.stagingDao.updateEntity(videoData);
                }

                boolean watchedApi = MetadataTools.allMediaFilesWatched(videoData, true);
                if (videoData.isWatchedApi() != watchedApi) {
                    videoData.setWatchedApi(watchedFile);
                    this.stagingDao.updateEntity(videoData);
                }
            }
        }
        
        // delete media file
        mediaFile.getVideoDatas().clear();
        this.stagingDao.deleteEntity(mediaFile);
    }

    private void delete(VideoData videoData, Set<String> filesToDelete) {
        LOG.debug("Delete: {}", videoData);
        
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
        LOG.debug("Delete: {}", season);
        
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
        LOG.debug("Delete: {}", series);
        
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
        StorageType storageType;
        if (artwork.getArtworkType() == ArtworkType.PHOTO) {
            storageType = StorageType.PHOTO;
        } else {
            storageType = StorageType.ARTWORK;
        }

        // delete generated files
        for (ArtworkGenerated generated : located.getGeneratedArtworks()) {
            this.delete(generated, storageType, filesToDelete);
        }

        // delete located file
        if (StringUtils.isNotBlank(located.getCacheFilename())) {
            String filename = FilenameUtils.concat(located.getCacheDirectory(), located.getCacheFilename());
            filesToDelete.add(this.fileStorageService.getStorageDir(storageType, filename));
        }
        
        artwork.getArtworkLocated().remove(located);
        this.stagingDao.deleteEntity(located);
    }

    private void delete(ArtworkGenerated generated, StorageType storageType, Set<String> filesToDelete) {
        String filename = FilenameUtils.concat(generated.getCacheDirectory(), generated.getCacheFilename());
        filesToDelete.add(this.fileStorageService.getStorageDir(storageType, filename));
        this.stagingDao.deleteEntity(generated);
    }
    
    @Transactional
    @SuppressWarnings("unchecked")
    public List<Long> getOrphanPersons() {
        final StringBuilder query = new StringBuilder();
        query.append("SELECT p.id FROM Person p ");
        query.append("WHERE not exists (select 1 from CastCrew c where c.person=p)");
        return this.stagingDao.find(query);
    }
    
    @Transactional
    public Set<String> deletePerson(Long id) {
        Set<String> filesToDelete = new HashSet<String>();
        Person person = this.stagingDao.getById(Person.class, id);
        
        LOG.debug("Delete: {}", person);
        
        if (person != null) {
            this.delete(person.getPhoto(), filesToDelete);
            this.stagingDao.deleteEntity(person);
        }
        
        return filesToDelete;
    }

    @Transactional
    public Set<String> ignoreArtworkLocated(Long id) {
        ArtworkLocated located = this.stagingDao.getById(ArtworkLocated.class, id);
        if (located != null) {
            StorageType storageType;
            if (located.getArtwork().getArtworkType() == ArtworkType.PHOTO) {
                storageType = StorageType.PHOTO;
            } else {
                storageType = StorageType.ARTWORK;
            }

            Set<String> filesToDelete = new HashSet<String>();
            // delete generated files
            for (ArtworkGenerated generated : located.getGeneratedArtworks()) {
                this.delete(generated, storageType, filesToDelete);
            }
            
            located.getGeneratedArtworks().clear();
            located.setStatus(StatusType.IGNORE);
            stagingDao.updateEntity(located);
            return filesToDelete;
        }
        return null;
    }

    @Transactional
    public boolean toogleWatchedStatus(long id, boolean watched, boolean apiCall) {
        StageFile stageFile = this.stagingDao.getStageFile(id);
        return this.toogleWatchedStatus(stageFile, watched, apiCall);
    }

    public boolean toogleWatchedStatus(StageFile stageFile, boolean watched, boolean apiCall) {
        if (stageFile == null) {
            return false;
        }
        if (!FileType.VIDEO.equals(stageFile.getFileType())) {
            return false;
        }
        if (StatusType.DUPLICATE.equals(stageFile.getStatus())) {
            return false;
        }

        MediaFile mediaFile = stageFile.getMediaFile();
        if (mediaFile == null) {
            return false;
        }
        
        // update media file
        if (apiCall) {
            mediaFile.setWatchedApi(watched);
        } else {
            mediaFile.setWatchedFile(watched);
        }
        LOG.debug("Mark as {} {}: {}", (apiCall?"api":"file"), (watched?"watched":"unwatched"), mediaFile);
        this.stagingDao.updateEntity(mediaFile);

        if (mediaFile.isExtra()) {
            LOG.trace("Media file is an extra where no watched status will be populated: {}", mediaFile);
            return true;
        }
        
        // determine watch status for each video data which is no extra
        for (VideoData videoData : mediaFile.getVideoDatas()) {
            boolean watchedAll = MetadataTools.allMediaFilesWatched(videoData, apiCall);
            if (apiCall) {
                if (videoData.isWatchedApi() != watchedAll) {
                    videoData.setWatchedApi(watchedAll);
                    LOG.debug("Mark as api {}: {}", (watchedAll?"watched":"unwatched"), videoData);
                    this.stagingDao.updateEntity(videoData);
                }
            } else if (videoData.isWatchedFile() != watchedAll) {
                videoData.setWatchedFile(watchedAll);
                LOG.debug("Mark as file {}: {}", (watchedAll?"watched":"unwatched"), videoData);
                this.stagingDao.updateEntity(videoData);
            }
        }
        return true;
    }

    @Transactional
    public void updateGenresXml(Map<String,String> subGenres) {
        StringBuilder sb = new StringBuilder();
        sb.append("UPDATE Genre ");
        sb.append("SET targetXml = null ");
        sb.append("WHERE targetXml is not null ");
        sb.append("AND lower(name) not in (:subGenres) ");
        
        Map<String,Object> params = new HashMap<String,Object>();
        params.put("subGenres", subGenres.keySet());
        this.stagingDao.executeUpdate(sb, params);

        for (Entry<String,String> entry : subGenres.entrySet()) {
            sb.setLength(0);
            sb.append("UPDATE Genre ");
            sb.append("SET targetXml=:targetXml " );
            sb.append("WHERE lower(name)=:subGenre ");

            params.clear();
            params.put("subGenre", entry.getKey());
            params.put("targetXml", entry.getValue());
            this.stagingDao.executeUpdate(sb, params);
        }
    }
    
    @Transactional
    public int deleteOrphanGenres() {
        StringBuffer sb = new StringBuffer();
        sb.append("DELETE FROM genre ");
        sb.append("WHERE not exists (select 1 from videodata_genres vg where vg.genre_id=id) ");
        sb.append("AND not exists (select 1 from series_genres sg where sg.genre_id=id) ");
        return this.stagingDao.executeSqlUpdate(sb);
    }
}
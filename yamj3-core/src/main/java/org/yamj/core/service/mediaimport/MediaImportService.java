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
package org.yamj.core.service.mediaimport;

import static org.yamj.common.type.MetaDataType.*;
import static org.yamj.common.type.StatusType.*;
import static org.yamj.core.service.mediaimport.ImageImportTools.*;
import static org.yamj.core.tools.YamjTools.getExternalSubtitleFormat;
import static org.yamj.core.tools.YamjTools.getWatchedDTO;
import static org.yamj.core.tools.YamjTools.setSortTitle;
import static org.yamj.plugin.api.Constants.LANGUAGE_UNTERTERMINED;
import static org.yamj.plugin.api.Constants.UNKNOWN;
import static org.yamj.plugin.api.model.type.ArtworkType.*;

import java.util.*;
import java.util.Map.Entry;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.config.ConfigServiceWrapper;
import org.yamj.core.config.LocaleService;
import org.yamj.core.database.dao.*;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.type.FileType;
import org.yamj.core.database.service.CommonStorageService;
import org.yamj.core.database.service.MetadataStorageService;
import org.yamj.core.service.file.FileTools;
import org.yamj.core.service.various.IdentifierService;
import org.yamj.core.service.various.StagingService;
import org.yamj.core.tools.WatchedDTO;
import org.yamj.plugin.api.model.type.ArtworkType;
import org.yamj.plugin.api.model.type.ImageType;

/**
 * The media import service is a spring-managed service. This will be used by
 * the MediaImportRunner only in order to access other spring beans cause the
 * MediaImportRunner itself is no spring-managed bean and dependency injection
 * will fail on that runner.
 *
 */
@Service("mediaImportService")
@DependsOn("upgradeDatabaseService")
public class MediaImportService {

    private static final Logger LOG = LoggerFactory.getLogger(MediaImportService.class);
    private static final String SCANNER_ID = "filename";
    private static final String TVSHOW_NFO_NAME = "tvshow";
    
    @Autowired
    private StagingDao stagingDao;
    @Autowired
    private MediaDao mediaDao;
    @Autowired
    private MetadataDao metadataDao;
    @Autowired
    private ArtworkDao artworkDao;
    @Autowired
    private FilenameScanner filenameScanner;
    @Autowired
    private ConfigServiceWrapper configServiceWrapper;
    @Autowired
    private MetadataStorageService metadataStorageService;
    @Autowired
    private CommonStorageService commonStorageService;
    @Autowired
    private StagingService stagingService;
    @Autowired
    private LocaleService localeService;
    @Autowired
    private IdentifierService identifierService;
    
    @Value("${yamj3.folder.name.nfo:null}")
    private String nfoFolderName;
    @Value("${yamj3.folder.name.subtitle:null}")
    private String subtitleFolderName;
    @Value("${yamj3.folder.name.artwork:null}")
    private String artworkFolderName;
    @Value("${yamj3.folder.name.photo:null}")
    private String photoFolderName;
    
    @Transactional(readOnly = true)
    public Long getNextStageFileId(final FileType... fileTypes) {
        return this.stagingDao.getNextStageFileId(fileTypes);
    }

    @Transactional
    public void processVideo(long id) {
        StageFile stageFile = stagingDao.getStageFile(id);

        if (stageFile.getMediaFile() == null) {
            LOG.info("Process new video {}-'{}'", stageFile.getId(), stageFile.getFileName());

            // process video file
            processVideoFile(stageFile);

            // attach NFO files
            attachNfoFilesToVideo(stageFile);
        } else {
            LOG.info("Process updated video {} - '{}'", stageFile.getId(), stageFile.getFileName());

            // just update media file
            MediaFile mediaFile = stageFile.getMediaFile();
            mediaFile.setStatus(UPDATED);
            mediaDao.updateEntity(mediaFile);
        }

        // mark stage file as done
        stageFile.setStatus(DONE);
        stagingDao.updateEntity(stageFile);
    }

    private void processVideoFile(StageFile stageFile) {

        // check if same media file already exists
        MediaFile mediaFile = mediaDao.getMediaFile(stageFile.getFileName());
        if (mediaFile != null) {
            LOG.warn("Media file for '{}' already present for new stage file", stageFile.getFileName());

            mediaFile.addStageFile(stageFile);
            stageFile.setMediaFile(mediaFile);
            stageFile.setStatus(DUPLICATE);
            mediaDao.updateEntity(mediaFile);
            return;
        }

        // scan filename for informations
        FilenameDTO dto = new FilenameDTO(stageFile);
        filenameScanner.scan(dto);
        LOG.debug("Scanned filename {}-'{}': title='{}', year={}",
                stageFile.getId(), stageFile.getFileName(), dto.getTitle(), dto.getYear());

        if (StringUtils.isBlank(dto.getTitle())) {
            if (dto.getYear() > 0) {
                LOG.warn("No valid title scanned from '{}', year will be used as title", stageFile.getFileName());
                dto.setTitle(String.valueOf(dto.getYear()));
                dto.setYear(-1);
            } else {
                LOG.error("No valid title and year could be scanned from filename '{}'", stageFile.getFileName());
                stageFile.setStatus(ERROR);
                mediaDao.updateEntity(stageFile);
                return;
            }
        } else {
            // clean the title
            final String cleanTitle = this.identifierService.cleanIdentifier(dto.getTitle());
            if (StringUtils.isBlank(cleanTitle)) {
                LOG.error("No valid clean title for '{}' from filename '{}'", dto.getTitle(), stageFile.getFileName());
                stageFile.setStatus(ERROR);
                mediaDao.updateEntity(stageFile);
                return;
            }
            
            // set clean title in DTO for later reuse
            dto.setCleanTitle(cleanTitle);
        }

        // determine if watched file exists for the video file
        final Date maxWatchedFileDate = this.stagingService.maxWatchedFileDate(stageFile);

        // new media file
        mediaFile = new MediaFile();
        mediaFile.setFileName(stageFile.getFileName());
        mediaFile.setContainer(dto.getContainer());
        mediaFile.setExtra(dto.isExtra());
        mediaFile.setPart(dto.getPart());
        mediaFile.setPartTitle(dto.getPartTitle());
        mediaFile.setMovieVersion(dto.getMovieVersion());
        mediaFile.setFps(dto.getFps());
        mediaFile.setCodec(dto.getVideoCodec());
        mediaFile.setVideoSource(dto.getVideoSource());
        mediaFile.setEpisodeCount(dto.getEpisodes().size());
        mediaFile.setStatus(NEW);
        mediaFile.setWatchedFile(maxWatchedFileDate != null, maxWatchedFileDate);
        mediaFile.addStageFile(stageFile);
        stageFile.setMediaFile(mediaFile);

        LOG.debug("Store new media file: '{}'", mediaFile.getFileName());
        mediaDao.saveEntity(mediaFile);

        // SUBTITLE
        this.attachSubtilesToMediaFile(mediaFile, stageFile);

        // METADATA OBJECTS
        if (dto.isMovie()) {
            // VIDEO DATA for movies

            String identifier = dto.buildIdentifier();
            VideoData videoData = metadataDao.getVideoData(identifier);
            if (videoData == null) {

                // NEW video data
                videoData = new VideoData(identifier, dto.getIdMap());
                videoData.setTitle(dto.getTitle(), SCANNER_ID);
                videoData.setTitleOriginal(dto.getTitle(), SCANNER_ID);
                videoData.setPublicationYear(dto.getYear(), SCANNER_ID);
                videoData.setStatus(NEW);
                mediaFile.addVideoData(videoData);
                videoData.addMediaFile(mediaFile);
                videoData.setTrailerStatus(NEW);
                
                // set watched if media file is NO extra
                if (!mediaFile.isExtra()) {
                    videoData.setWatched(mediaFile.isWatchedFile(), mediaFile.getWatchedFileLastDate());
                }

                // set sort title
                setSortTitle(videoData, configServiceWrapper.getSortStripPrefixes());

                LOG.debug("Store new movie: '{}' - {}", videoData.getTitle(), videoData.getPublicationYear());
                metadataDao.saveEntity(videoData);

                // create new poster artwork entry
                Artwork poster = new Artwork();
                poster.setArtworkType(POSTER);
                poster.setStatus(NEW);
                poster.setVideoData(videoData);
                metadataDao.saveEntity(poster);

                // create new fanart artwork entry
                Artwork fanart = new Artwork();
                fanart.setArtworkType(FANART);
                fanart.setStatus(NEW);
                fanart.setVideoData(videoData);
                metadataDao.saveEntity(fanart);

            } else {
                mediaFile.addVideoData(videoData);
                videoData.addMediaFile(mediaFile);

                // set watched status
                final WatchedDTO watchedDTO = getWatchedDTO(videoData);
                videoData.setWatched(watchedDTO.isWatched(), watchedDTO.getWatchedDate());
                
                // update video data
                metadataDao.updateEntity(videoData);
            }

            // boxed set handling
            if (MapUtils.isNotEmpty(dto.getSetMap())) {
                for (Entry<String, Integer> entry : dto.getSetMap().entrySet()) {
                    // add boxed set to video data
                    final String boxedSetIdentifier = identifierService.cleanIdentifier(entry.getKey());
                    if (StringUtils.isNotBlank(boxedSetIdentifier)) {
                        LOG.debug("Add movie filename boxed set: {} (Order={})", entry.getKey(), entry.getValue()==null?"-1":entry.getValue());
                        videoData.addBoxedSetDTO(SCANNER_ID, boxedSetIdentifier, entry.getKey(), entry.getValue(), UNKNOWN);
                    }
                }

                // store associated entities (only sets right now)
                this.metadataStorageService.storeAssociatedEntities(videoData);
                // updated boxed sets for video data
                this.metadataStorageService.updateBoxedSets(videoData);
            }

        } else {
            // VIDEO DATA for episodes
            for (Integer episode : dto.getEpisodes()) {
                String identifier = dto.buildEpisodeIdentifier(episode);
                VideoData videoData = metadataDao.getVideoData(identifier);
                if (videoData == null) {
                    // get the prefix for setting the sort title
                    List<String> prefixes = this.configServiceWrapper.getSortStripPrefixes();

                    // NEW video data
                    // getById or create season
                    String seasonIdentifier = dto.buildSeasonIdentifier();
                    Season season = metadataDao.getSeason(seasonIdentifier);
                    if (season == null) {

                        // getById or create series
                        String seriesIdentifier = dto.buildIdentifier();
                        Series series = metadataDao.getSeries(seriesIdentifier);
                        if (series == null) {
                            series = new Series(seriesIdentifier, dto.getIdMap());
                            series.setTitle(dto.getTitle(), SCANNER_ID);
                            series.setTitleOriginal(dto.getTitle(), SCANNER_ID);
                            series.setStatus(NEW);
                            series.setTrailerStatus(NEW);

                            // set sort title
                            setSortTitle(series, prefixes);

                            LOG.debug("Store new series: '{}'", series.getTitle());
                            metadataDao.saveEntity(series);

                            // create new poster artwork entry
                            Artwork poster = new Artwork();
                            poster.setArtworkType(POSTER);
                            poster.setStatus(NEW);
                            poster.setSeries(series);
                            metadataDao.saveEntity(poster);

                            // create new fanart artwork entry
                            Artwork fanart = new Artwork();
                            fanart.setArtworkType(FANART);
                            fanart.setStatus(NEW);
                            fanart.setSeries(series);
                            metadataDao.saveEntity(fanart);

                            // create new banner artwork entry
                            Artwork banner = new Artwork();
                            banner.setArtworkType(BANNER);
                            banner.setStatus(NEW);
                            banner.setSeries(series);
                            metadataDao.saveEntity(banner);
                        }

                        // boxed set handling
                        if (MapUtils.isNotEmpty(dto.getSetMap())) {
                            for (Entry<String, Integer> entry : dto.getSetMap().entrySet()) {
                                // add boxed set to video data
                                final String boxedSetIdentifier = identifierService.cleanIdentifier(entry.getKey());
                                if (StringUtils.isNotBlank(boxedSetIdentifier)) {
                                    LOG.debug("Add series filename boxed set: {} (Order={})", entry.getKey(), entry.getValue()==null?"-1":entry.getValue());
                                    series.addBoxedSetDTO(SCANNER_ID, boxedSetIdentifier, entry.getKey(), entry.getValue(), UNKNOWN);
                                }
                            }

                            // store associated entities (only sets right now)
                            this.metadataStorageService.storeAssociatedEntities(series);
                            // updated boxed sets for video data
                            this.metadataStorageService.updateBoxedSets(series);
                        }

                        season = new Season(seasonIdentifier);
                        season.setSeason(dto.getSeason());
                        season.setTitle(dto.getTitle(), SCANNER_ID);
                        season.setTitleOriginal(dto.getTitle(), SCANNER_ID);
                        season.setSeries(series);
                        season.setStatus(NEW);

                        // set sort title
                        setSortTitle(season, prefixes);

                        LOG.debug("Store new seaon: '{}' - Season {}", season.getTitle(), season.getSeason());
                        metadataDao.saveEntity(season);

                        // create new poster artwork entry
                        Artwork poster = new Artwork();
                        poster.setArtworkType(POSTER);
                        poster.setStatus(NEW);
                        poster.setSeason(season);
                        metadataDao.saveEntity(poster);

                        // create new fanart artwork entry
                        Artwork fanart = new Artwork();
                        fanart.setArtworkType(FANART);
                        fanart.setStatus(NEW);
                        fanart.setSeason(season);
                        metadataDao.saveEntity(fanart);

                        // create new banner artwork entry
                        Artwork banner = new Artwork();
                        banner.setArtworkType(BANNER);
                        banner.setStatus(NEW);
                        banner.setSeason(season);
                        metadataDao.saveEntity(banner);
                    }

                    videoData = new VideoData(identifier);
                    if (StringUtils.isNotBlank(dto.getEpisodeTitle())) {
                        videoData.setTitle(dto.getEpisodeTitle(), SCANNER_ID);
                        videoData.setTitleOriginal(dto.getEpisodeTitle(), SCANNER_ID);
                    } else {
                        videoData.setTitle(dto.getTitle(), SCANNER_ID);
                        videoData.setTitleOriginal(dto.getTitle(), SCANNER_ID);
                    }
                    videoData.setStatus(NEW);
                    videoData.setSeason(season);
                    videoData.setEpisode(episode);
                    mediaFile.addVideoData(videoData);
                    videoData.addMediaFile(mediaFile);
                    videoData.setTrailerStatus(NEW);

                    // set sort title
                    setSortTitle(videoData, prefixes);

                    LOG.debug("Store new episode: '{}' - Season {} - Episode {}", season.getTitle(), season.getSeason(), videoData.getEpisode());
                    metadataDao.saveEntity(videoData);

                    // create new videoimage artwork entry
                    Artwork videoimage = new Artwork();
                    videoimage.setArtworkType(VIDEOIMAGE);
                    videoimage.setStatus(NEW);
                    videoimage.setVideoData(videoData);
                    metadataDao.saveEntity(videoimage);

                } else {
                    mediaFile.addVideoData(videoData);
                    videoData.addMediaFile(mediaFile);
                    metadataDao.updateEntity(videoData);
                }
            }
        }
    }

    private void attachNfoFilesToVideo(StageFile stageFile) {
        if (stageFile.getMediaFile() == null) {
            // video file must be associated to a media file
            return;
        }
        if (stageFile.getMediaFile().isExtra()) {
            // media file may not be an extra
            return;
        }
        Set<VideoData> videoDatas = stageFile.getMediaFile().getVideoDatas();
        if (CollectionUtils.isEmpty(videoDatas)) {
            // videos must exists
            return;
        }

        // evaluate if TV show
        boolean isTvShow = false;
        for (VideoData videoData : videoDatas) {
            if (!videoData.isMovie()) {
                isTvShow = true;
                break;
            }
        }

        // holds the found NFO files with priority
        Map<StageFile, Integer> nfoFiles = new HashMap<>();

        // search name is the base name of the stage file
        final String searchName = stageFile.getBaseName();
        // search in directory
        final StageDirectory directory = stageFile.getStageDirectory();

        // case 1: find matching NFO in directory
        StageFile foundNfoFile = this.stagingDao.findNfoFile(searchName, directory);
        if (foundNfoFile != null && !nfoFiles.containsKey(foundNfoFile)) {
            nfoFiles.put(foundNfoFile, Integer.valueOf(1));

            // change status for PRIO-1-NFO
            if (FileTools.isFileScannable(foundNfoFile)) {
                foundNfoFile.setStatus(DONE);
            } else {
                foundNfoFile.setStatus(INVALID);
            }
            this.stagingDao.updateEntity(foundNfoFile);
        }

        // case 2: find matching files in NFO folder
        Set<String> searchNames = Collections.singleton(searchName.toLowerCase());

        Library library = null;
        if (this.configServiceWrapper.getBooleanProperty("yamj3.librarycheck.folder.nfo", true)) {
            library = stageFile.getStageDirectory().getLibrary();
        }

        for (StageFile nfoFile : this.stagingDao.findStageFilesInSpecialFolder(FileType.NFO, nfoFolderName, library, searchNames)) {
            nfoFiles.put(nfoFile, Integer.valueOf(2));

            // change status for PRIO-2-NFO
            if (FileTools.isFileScannable(nfoFile)) {
                nfoFile.setStatus(DONE);
            } else {
                nfoFile.setStatus(INVALID);
            }
            this.stagingDao.updateEntity(nfoFile);
        }

        if (isTvShow) {
            // case 3: tvshow.nfo in same directory as video
            foundNfoFile = this.stagingDao.findNfoFile(TVSHOW_NFO_NAME, directory);
            if (foundNfoFile != null && !nfoFiles.containsKey(foundNfoFile)) {
                nfoFiles.put(foundNfoFile, Integer.valueOf(3));
            }

            // case 4: tvshow.nfo in parent directory
            foundNfoFile = this.stagingDao.findNfoFile(TVSHOW_NFO_NAME, directory.getParentDirectory());
            if (foundNfoFile != null && !nfoFiles.containsKey(foundNfoFile)) {
                nfoFiles.put(foundNfoFile, Integer.valueOf(4));
            }
        }

        // case 10-n: apply "nfoName = dirName" to all video data
        // NOTE: 11-n are only applied if recursive scan is enabled
        boolean recurse = this.configServiceWrapper.getBooleanProperty("yamj3.scan.nfo.recursiveDirectories", false);
        LOG.trace("Recursive scan of directories is {}", recurse ? "enabled" : "disabled");
        this.findNfoWithDirectoryName(nfoFiles, stageFile.getStageDirectory(), 10, recurse);

        if (MapUtils.isEmpty(nfoFiles)) {
            // no NFO files found
            return;
        }

        for (Entry<StageFile, Integer> entry : nfoFiles.entrySet()) {
            StageFile nfoFile = entry.getKey();
            int priority = entry.getValue();

            for (VideoData videoData : videoDatas) {
                LOG.debug("Found NFO {}-'{}' with priority {} for video data '{}'",
                        nfoFile.getId(), nfoFile.getFileName(), priority, videoData.getIdentifier());

                NfoRelation nfoRelation = new NfoRelation(nfoFile, videoData);
                nfoRelation.setPriority(priority);

                if (!videoData.getNfoRelations().contains(nfoRelation)) {
                    this.mediaDao.saveEntity(nfoRelation);
                    videoData.addNfoRelation(nfoRelation);
                    nfoFile.addNfoRelation(nfoRelation);

                    LOG.trace("Stored new NFO relation: stageFile={}, videoData={}", nfoFile.getId(), videoData.getId());
                }
            }
        }
    }

    private void findNfoWithDirectoryName(Map<StageFile, Integer> nfoFiles, StageDirectory directory, int counter, boolean recurse) {
        if (directory == null) {
            return;
        }

        StageFile foundNfoFile = this.stagingDao.findNfoFile(directory.getDirectoryName(), directory);
        if (foundNfoFile != null && !nfoFiles.containsKey(foundNfoFile)) {
            nfoFiles.put(foundNfoFile, Integer.valueOf(counter));
        }

        if (recurse) {
            // recurse until parent is null
            this.findNfoWithDirectoryName(nfoFiles, directory.getParentDirectory(), counter+1, recurse);
        }
    }

    private void attachSubtilesToMediaFile(MediaFile mediaFile, StageFile videoFile) {
        Library library = null;
        if (this.configServiceWrapper.getBooleanProperty("yamj3.librarycheck.folder.subtitle", true)) {
            library = videoFile.getStageDirectory().getLibrary();
        }

        // case 1: find matching files in same directory
        List<StageFile> stageFiles = this.stagingDao.findStageFiles(videoFile.getBaseName(), null, videoFile.getStageDirectory(), FileType.SUBTITLE);
        // case 2: find matching files in subtitle folder
        Set<String> searchNames = Collections.singleton(videoFile.getBaseName().toLowerCase());
        List<StageFile> other = this.stagingDao.findStageFilesInSpecialFolder(FileType.SUBTITLE, subtitleFolderName, library, searchNames);
        stageFiles.addAll(other);

        for (StageFile subtitleFile : stageFiles) {
            Subtitle subtitle = new Subtitle();
            subtitle.setCounter(0);
            subtitle.setStageFile(subtitleFile);
            subtitle.setMediaFile(mediaFile);
            subtitle.setFormat(getExternalSubtitleFormat(subtitleFile.getExtension()));
            // TODO search stage files with language
            subtitle.setLanguageCode(LANGUAGE_UNTERTERMINED);
            subtitle.setDefaultFlag(true);
            this.mediaDao.saveEntity(subtitle);

            if (subtitleFile.isNotFound()) {
                subtitleFile.getSubtitles().add(subtitle);
                subtitleFile.setStatus(DONE);
                this.stagingDao.updateEntity(subtitleFile);
            }
        }
    }

    @Transactional
    public void processingError(Long id) {
        StageFile stageFile = stagingDao.getById(StageFile.class, id);
        if (stageFile != null) {
            stageFile.setStatus(ERROR);
            stagingDao.updateEntity(stageFile);
        }
    }

    @Transactional
    public void processNfo(Long id) {
        StageFile stageFile = stagingDao.getStageFile(id);
        LOG.info("Process nfo {}-'{}'", stageFile.getId(), stageFile.getFileName());

        // check if NFO file can be scanned
        if (!FileTools.isFileScannable(stageFile)) {
            LOG.debug("NFO file {}-'{}' is not scannable", stageFile.getId(), stageFile.getFileName());
            stageFile.setStatus(INVALID);
            stagingDao.updateEntity(stageFile);
            // nothing to do anymore
            return;
        }

        // process new NFO
        boolean found = processNfoFile(stageFile);
        if (found || CollectionUtils.isNotEmpty(stageFile.getNfoRelations())) {
            stageFile.setStatus(DONE);
        } else {
            stageFile.setStatus(NOTFOUND);
        }
        stagingDao.updateEntity(stageFile);

        for (NfoRelation nfoRelation : stageFile.getNfoRelations()) {
            VideoData videoData = nfoRelation.getNfoRelationPK().getVideoData();
            if (videoData.isMovie()) {
                videoData.setStatus(UPDATED);
                stagingDao.updateEntity(videoData);

                LOG.debug("Marked movied {}-'{}' as updated", videoData.getId(), videoData.getTitle());
            } else {
                videoData.setStatus(UPDATED);
                stagingDao.updateEntity(videoData);

                Season season = videoData.getSeason();
                season.setStatus(UPDATED);
                stagingDao.updateEntity(videoData);

                Series series = season.getSeries();
                series.setStatus(UPDATED);
                stagingDao.updateEntity(series);

                LOG.debug("Marked series {}-'{}' as updated", series.getId(), series.getTitle());
            }
        }
    }

    private boolean processNfoFile(StageFile stageFile) {
        // find video files for this NFO file
        Map<VideoData, Integer> videoFiles = this.findVideoFilesForNFO(stageFile);
        if (MapUtils.isEmpty(videoFiles)) {
            // no video files found
            return false;
        }

        for (Entry<VideoData, Integer> entry : videoFiles.entrySet()) {
            VideoData videoData = entry.getKey();
            int priority = entry.getValue().intValue();

            LOG.debug("Found video data {}-'{}' for nfo file '{}' with priority {}",
                    videoData.getId(), videoData.getIdentifier(), stageFile.getFileName(), priority);

            NfoRelation nfoRelation = new NfoRelation(stageFile, videoData);
            nfoRelation.setPriority(priority);

            if (!stageFile.getNfoRelations().contains(nfoRelation)) {
                this.mediaDao.saveEntity(nfoRelation);
                stageFile.addNfoRelation(nfoRelation);
                videoData.addNfoRelation(nfoRelation);

                LOG.trace("Stored new NFO relation: stageFile={}, videoData={}", stageFile.getId(), videoData.getId());
            }
        }
        
        return true;
    }

    private Map<VideoData, Integer> findVideoFilesForNFO(StageFile stageFile) {
        Map<VideoData, Integer> videoFiles = new HashMap<>();
        List<VideoData> videoDatas = null;

        if (FileTools.isWithinSpecialFolder(stageFile, nfoFolderName)) {

            // case 2: video file has same base name in library
            Library library = null;
            if (this.configServiceWrapper.getBooleanProperty("yamj3.librarycheck.folder.nfo", true)) {
                library = stageFile.getStageDirectory().getLibrary();
            }

            videoDatas = this.stagingDao.findVideoDatasForNFO(stageFile.getBaseName(), library);
            attachVideoDataToNFO(videoFiles, videoDatas, 2);

        } else if (stageFile.getBaseName().equalsIgnoreCase(stageFile.getStageDirectory().getDirectoryName())) {

            // case 10: apply to all video data in stage directory
            videoDatas = this.stagingDao.findVideoDatasForNFO(stageFile.getStageDirectory());
            attachVideoDataToNFO(videoFiles, videoDatas, 10);

            // get child directories
            List<StageDirectory> childDirectories = this.stagingDao.getChildDirectories(stageFile.getStageDirectory());
            if (CollectionUtils.isEmpty(childDirectories)) {
                return videoFiles;
            }

            if (CollectionUtils.isNotEmpty(childDirectories)) {
                boolean recurse = this.configServiceWrapper.getBooleanProperty("yamj3.scan.nfo.recursiveDirectories", false);
                LOG.trace("Recursive scan of directories is {}", recurse ? "enabled" : "disabled");

                if (recurse) {
                    // TODO case 11-n: recursive scanning
                }
            }

        } else if (TVSHOW_NFO_NAME.equals(stageFile.getBaseName())) {

            // case 3: tvshow.nfo in same directory as video
            videoDatas = this.stagingDao.findVideoDatasForNFO(stageFile.getStageDirectory());
            attachVideoDataToNFO(videoFiles, videoDatas, 3, true);

            // case 4: tvshow.nfo in parent directory (so search in child directories)
            List<StageDirectory> childDirectories = this.stagingDao.getChildDirectories(stageFile.getStageDirectory());
            videoDatas = this.stagingDao.findVideoDatasForNFO(childDirectories);
            attachVideoDataToNFO(videoFiles, videoDatas, 4, true);

        } else {

            // case 1: video file has same base name in same directory
            videoDatas = this.stagingDao.findVideoDatasForNFO(stageFile.getBaseName(), stageFile.getStageDirectory());
            attachVideoDataToNFO(videoFiles, videoDatas, 1);
        }

        return videoFiles;
    }

    private static void attachVideoDataToNFO(Map<VideoData, Integer> videoFiles, Collection<VideoData> videoDatas, int priority) {
        attachVideoDataToNFO(videoFiles, videoDatas, priority, false);
    }

    private static void attachVideoDataToNFO(Map<VideoData, Integer> videoFiles, Collection<VideoData> videoDatas, int priority, boolean tvShowOnly) {
        if (CollectionUtils.isNotEmpty(videoDatas)) {
            for (VideoData videoData : videoDatas) {
                if (tvShowOnly && videoData.isMovie()) {
                    // do not apply
                } else {
                    videoFiles.put(videoData, Integer.valueOf(priority));
                }
            }
        }
    }

    @Transactional
    public void processImage(Long id) {
        StageFile stageFile = stagingDao.getStageFile(id);
        LOG.info("Process image {}-'{}'", stageFile.getId(), stageFile.getFileName());

        boolean updated = false;
        // just update located artwork
        for (ArtworkLocated located : stageFile.getArtworkLocated()) {
            // mark located as updated
            located.setStatus(UPDATED);
            this.mediaDao.updateEntity(located);
            updated = true;
        }

        // process new image
        boolean found = processImageFile(stageFile);

        // update stage file
        if (found || updated) {
            stageFile.setStatus(DONE);
        } else {
            stageFile.setStatus(NOTFOUND);
        }
        stagingDao.updateEntity(stageFile);
    }

    private boolean processImageFile(StageFile stageFile) {
        // get the base file name
        final String fileBaseName = stageFile.getBaseName().toLowerCase();
        
        // get image tokens to regard
        final List<String> tokensPoster = this.configServiceWrapper.getArtworkTokens(POSTER);
        final List<String> tokensFanart = this.configServiceWrapper.getArtworkTokens(FANART);
        final List<String> tokensBanner = this.configServiceWrapper.getArtworkTokens(BANNER);
        final List<String> tokensPhoto = this.configServiceWrapper.getArtworkTokens(PHOTO);
            
        // determine artwork type and metadataTypes to which image could be applied
        final boolean generic;
        final ArtworkType artworkType;
        final EnumSet<MetaDataType> metaDataTypes;
        
        if (isGenericImage(fileBaseName, tokensPoster)) {
            // determine a generic poster image which can match several metadata objects
            generic = true;
            artworkType = POSTER;
            metaDataTypes = EnumSet.of(MOVIE, SEASON, SERIES);

        } else if (isGenericImage(fileBaseName, tokensFanart)) {
            // determine a generic fanart image which can match several metadata objects
            generic = true;
            artworkType = FANART;
            metaDataTypes = EnumSet.of(MOVIE, SEASON, SERIES);

        } else if (isGenericImage(fileBaseName, tokensBanner)) {
            // determine a generic banner image which can match several metadata objects
            generic = true;
            artworkType = BANNER;
            metaDataTypes = EnumSet.of(MOVIE, SEASON, SERIES);

        } else if (isVideoImage(fileBaseName)) {
            // determined a video image of an episode
            generic = false;
            artworkType = VIDEOIMAGE;
            metaDataTypes = EnumSet.of(EPISODE);
            
        } else if ("movie".equals(fileBaseName) || isSpecialImage(fileBaseName, "movie", tokensPoster)) {
            // this is a generic movie poster
            generic = true;
            artworkType = POSTER;
            metaDataTypes = EnumSet.of(MOVIE);

        } else if (fileBaseName.startsWith("movie") && isSpecialImage(fileBaseName, "movie", tokensFanart)) {
            // this is a generic movie fanart
            generic = true;
            artworkType = FANART;
            metaDataTypes = EnumSet.of(MOVIE);

        } else if (fileBaseName.startsWith("movie") && isSpecialImage(fileBaseName, "movie", tokensBanner)) {
            // this is a generic movie banner
            generic = true;
            artworkType = BANNER;
            metaDataTypes = EnumSet.of(MOVIE);

        } else if ("season".equals(fileBaseName) || isSpecialImage(fileBaseName, "season", tokensPoster)) {
            // this is a generic season poster
            generic = true;
            artworkType = POSTER;
            metaDataTypes = EnumSet.of(SEASON);

        } else if (fileBaseName.startsWith("season") && isSpecialImage(fileBaseName, "season", tokensFanart)) {
            // this is a generic season fanart
            generic = true;
            artworkType = FANART;
            metaDataTypes = EnumSet.of(SEASON);

        } else if (fileBaseName.startsWith("season") && isSpecialImage(fileBaseName, "season", tokensBanner)) {
            // this is a generic season banner
            generic = true;
            artworkType = BANNER;
            metaDataTypes = EnumSet.of(SEASON);

        } else if ("show".equals(fileBaseName) || isSpecialImage(fileBaseName, "season", tokensPoster) ||
                   "series".equals(fileBaseName) || isSpecialImage(fileBaseName, "series", tokensPoster) ||
                   "season-all".equals(fileBaseName) || isSpecialImage(fileBaseName, "season-all", tokensPoster))
        {
            // this is a generic series poster
            generic = true;
            artworkType = POSTER;
            metaDataTypes = EnumSet.of(SERIES);

        } else if ((fileBaseName.startsWith("show") && isSpecialImage(fileBaseName, "show", tokensFanart)) ||
                   (fileBaseName.startsWith("series") && isSpecialImage(fileBaseName, "series", tokensFanart)) ||
                   (fileBaseName.startsWith("season-all") && isSpecialImage(fileBaseName, "season-all", tokensFanart)))
        {
            // this is a generic series fanart
            generic = true;
            artworkType = FANART;
            metaDataTypes = EnumSet.of(SERIES);

        } else if ((fileBaseName.startsWith("show") && isSpecialImage(fileBaseName, "show", tokensBanner)) ||
                   (fileBaseName.startsWith("series") && isSpecialImage(fileBaseName, "series", tokensBanner)) ||
                   (fileBaseName.startsWith("season-all") && isSpecialImage(fileBaseName, "season-all", tokensBanner)))
        {
            // this is a generic series banner
            generic = true;
            artworkType = BANNER;
            metaDataTypes = EnumSet.of(SERIES);
            
        } else if (fileBaseName.startsWith("set_")) {
            // this is a set image
            generic = false;
            metaDataTypes = EnumSet.of(BOXSET);
            
            // determine artwork type
            if (endsWithToken(fileBaseName, tokensFanart)) {
                artworkType = FANART;
            } else if (endsWithToken(fileBaseName, tokensBanner)) {
                artworkType = BANNER;
            } else {
                // everything else is a poster
                artworkType = POSTER;
            }
            
        } else if (endsWithToken(fileBaseName, tokensFanart)) {
            // this is a fanart
            artworkType = FANART;
            
            if (StringUtils.equalsIgnoreCase(stageFile.getStageDirectory().getDirectoryName(), stripToken(fileBaseName, tokensFanart))) {
                // generic image if file name equals directory name
                generic = true;
                metaDataTypes = EnumSet.of(MOVIE, SEASON, SERIES);
            } else {
                generic = false;
                // could just be applied to season or movie   
                metaDataTypes = EnumSet.of(MOVIE, SEASON);
            }

        } else if (endsWithToken(fileBaseName, tokensBanner)) {
            // this is a banner
            artworkType = BANNER;
            
            if (StringUtils.equalsIgnoreCase(stageFile.getStageDirectory().getDirectoryName(), stripToken(fileBaseName, tokensBanner))) {
                // generic image if file name equals directory name
                generic = true;
                metaDataTypes = EnumSet.of(MOVIE, SEASON, SERIES);
            } else {
                generic = false;
                // could just be applied to season or movie   
                metaDataTypes = EnumSet.of(MOVIE, SEASON);
            }

        } else {
            // NOTE: poster and photo images may have no special image marker like poster or photo
            final boolean inPhotoFolder = FileTools.isWithinSpecialFolder(stageFile, photoFolderName);
            
            if (inPhotoFolder || endsWithToken(fileBaseName, tokensPhoto)) {
                // this image determines a photo
                generic = false;
                artworkType = PHOTO;
                metaDataTypes = EnumSet.of(PERSON);
            } else if (StringUtils.equalsIgnoreCase(stageFile.getStageDirectory().getDirectoryName(), stripToken(fileBaseName, tokensPoster))) {
                // generic image if file name equals directory name
                generic = true;
                artworkType = POSTER;
                metaDataTypes = EnumSet.of(MOVIE, SEASON, SERIES);
            } else {
                // this image determines a poster
                generic = false;
                artworkType = POSTER;
                // could just be applied to season or movie   
                metaDataTypes = EnumSet.of(MOVIE, SEASON);
            }
        }
        
        LOG.info("Determined {} for metadata {}: {}", (generic?"generic ":"")+artworkType.name().toLowerCase(), metaDataTypes, stageFile.getBaseName());

        // holds matching artwork
        final Collection<Artwork> artworks;
        int priority = 1;
        
        if (metaDataTypes.contains(PERSON)) {
            // PERSON PHOTO
            final String stripped = stripToken(fileBaseName, tokensPhoto);

            // find person artwork
            String identifier = identifierService.cleanIdentifier(stripped);
            if (StringUtils.isBlank(identifier)) {
                LOG.warn("Could not search person artwork with empty identifier for file '{}'", fileBaseName);
                artworks = Collections.emptyList();
            } else {
                artworks = this.metadataDao.findPersonArtworks(identifier);
            }
            
        } else if (metaDataTypes.contains(BOXSET)) {
            // BOXSET IMAGE
            String boxedSetName = getBoxedSetName(fileBaseName);
            artworks = this.artworkDao.getBoxedSetArtwork(boxedSetName, artworkType);
            
        } else if (metaDataTypes.contains(EPISODE)) {
            // EPISODE IMAGE
            
            // get the episode part in case of files containing multiple episodes
            int part = getVideoImagePart(fileBaseName);
            // get the base name which video files should have
            String baseName = getBaseNameFromVideoImage(fileBaseName);
            
            // get matching episode image artwork
            List<Artwork> matching = this.stagingDao.findMatchingVideoImages(baseName, stageFile.getStageDirectory());
            
            if (part <= matching.size()) {
                // found artwork which matches the episode part
                artworks = Collections.singleton(matching.get(part-1));
            } else {
                artworks = Collections.emptyList();
            }
            
        } else if (generic) {
            // GENERIC IMAGES
            
            if (metaDataTypes.contains(MOVIE) || metaDataTypes.contains(SEASON)) {
                // generic select without base name which forces just to obey the directory
                artworks = this.stagingDao.findMatchingArtworkForMovieOrSeason(artworkType, null, stageFile.getStageDirectory());
                
                if (metaDataTypes.contains(SERIES)) {
                    // additional for series:
                    // search artwork where image files are in sub directories
                    artworks.addAll(this.stagingDao.findMatchingArtworkForSeries(artworkType, stageFile.getStageDirectory(), true));
                }
            } else {
                // just series possible:
                // search in same directory as image where video files are in same or sub directories
                artworks = this.stagingDao.findMatchingArtworkForSeries(artworkType, stageFile.getStageDirectory(), false);
            }
            
        } else {
            // handle non-generic images in same directory, so that just movies and seasons will be obeyed
            // cause series images are always generic and depending on the directory structure
            
            final String stripped;
            switch(artworkType) {
            case FANART:
                stripped = stripToken(fileBaseName, tokensFanart);
                break;
            case BANNER:
                stripped = stripToken(fileBaseName, tokensBanner);
                break;
            default:
                stripped = stripToken(fileBaseName, tokensPoster);
                break;
            }
            
            if (FileTools.isWithinSpecialFolder(stageFile, artworkFolderName)) {
                // artwork inside located artwork directory
                Library library = null;
                if (this.configServiceWrapper.getBooleanProperty("yamj3.librarycheck.folder.artwork", true)) {
                    library = stageFile.getStageDirectory().getLibrary();
                }
                // priority = 2 when inside artwork folder
                priority = 2;

                // find matching artwork
                artworks = this.stagingDao.findMatchingArtworkForMovieOrSeason(artworkType, stripped, library);
            } else {
                // find matching artwork in same directory
                artworks = this.stagingDao.findMatchingArtworkForMovieOrSeason(artworkType, stripped, stageFile.getStageDirectory());
            }
        }
        
        LOG.debug("Found {} matching artwork entries", artworks.size());
        if (CollectionUtils.isEmpty(artworks)) {
            // no artwork found so return
            return false;
        }

        // add artwork stage file to artwork
        for (Artwork artwork : artworks) {
            if (!configServiceWrapper.isLocalArtworkScanEnabled(artwork)) {
                LOG.info("Local artwork scan disabled: {}", artwork);
                continue;
            }

            ArtworkLocated located = new ArtworkLocated();
            located.setArtwork(artwork);
            located.setSource("file");
            located.setStageFile(stageFile);
            located.setPriority(priority);
            located.setHashCode(stageFile.getHashCode());
            located.setImageType(ImageType.fromString(stageFile.getExtension()));

            if (!artwork.getArtworkLocated().contains(located)) {

                if (FileTools.isFileReadable(stageFile)) {
                    located.setStatus(NEW);
                } else {
                    located.setStatus(INVALID);
                }

                this.mediaDao.saveEntity(located);
                artwork.getArtworkLocated().add(located);
            }
        }

        return true;
    }
    
    @Transactional
    public void processWatched(long id) {
        StageFile watchedFile = stagingDao.getStageFile(id);
        LOG.info("Process watched {}-'{}'", watchedFile.getId(), watchedFile.getFileName());

        // set watched status for video file(s)
        for (StageFile videoFile : this.stagingService.findWatchedVideoFiles(watchedFile)) {
            this.commonStorageService.toogleWatchedStatus(videoFile, true, false);
        }

        // update stage file
        watchedFile.setStatus(DONE);
        stagingDao.updateEntity(watchedFile);
    }

    @Transactional
    public void processSubtitle(Long id) {
        StageFile subtitleFile = stagingDao.getStageFile(id);
        LOG.info("Process subtitle {}-'{}'", subtitleFile.getId(), subtitleFile.getFileName());

        // determine language which may only be the "extension" of the base name
        final String language = FilenameUtils.getExtension(subtitleFile.getBaseName());
        final String languageCode = localeService.findLanguageCode(language);

        for (StageFile videoFile : this.stagingService.findSubtitleVideoFiles(subtitleFile, language)) {
            if (videoFile.getMediaFile() != null) {
                Subtitle subtitle = new Subtitle();
                subtitle.setCounter(0);
                subtitle.setStageFile(subtitleFile);
                subtitle.setMediaFile(videoFile.getMediaFile());

                if (!subtitleFile.getSubtitles().contains(subtitle)) {
                    subtitle.setFormat(getExternalSubtitleFormat(subtitleFile.getExtension()));

                    if (StringUtils.isBlank(languageCode)) {
                        subtitle.setLanguageCode(LANGUAGE_UNTERTERMINED);
                        subtitle.setDefaultFlag(true);
                    } else {
                        subtitle.setLanguageCode(languageCode);
                    }

                    subtitleFile.getSubtitles().add(subtitle);
                    this.mediaDao.saveEntity(subtitle);
                }
            }
        }

        // update stage file
        if (CollectionUtils.isEmpty(subtitleFile.getSubtitles())) {
            subtitleFile.setStatus(NOTFOUND);
        } else {
            subtitleFile.setStatus(DONE);
        }
        stagingDao.updateEntity(subtitleFile);
    }
}

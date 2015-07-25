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

import java.util.*;
import java.util.Map.Entry;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.common.type.MetaDataType;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.dao.ArtworkDao;
import org.yamj.core.database.dao.CommonDao;
import org.yamj.core.database.dao.MetadataDao;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.award.Award;
import org.yamj.core.database.model.award.MovieAward;
import org.yamj.core.database.model.award.SeriesAward;
import org.yamj.core.database.model.dto.*;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.database.model.type.OverrideFlag;
import org.yamj.core.service.artwork.ArtworkTools;
import org.yamj.core.tools.CountryXmlTools;
import org.yamj.core.tools.GenreXmlTools;
import org.yamj.core.tools.MetadataTools;

@Service("metadataStorageService")
public class MetadataStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(MetadataStorageService.class);
    @Autowired
    private CommonDao commonDao;
    @Autowired
    private MetadataDao metadataDao;
    @Autowired
    private ArtworkDao artworkDao;

    @Transactional(readOnly = true)
    public List<QueueDTO> getMetaDataQueueForScanning(final int maxResults) {
        final StringBuilder sql = new StringBuilder();
        sql.append("select vd.id,'");
        sql.append(MetaDataType.MOVIE);
        sql.append("' as metatype,vd.create_timestamp,vd.update_timestamp ");
        sql.append("from videodata vd ");
        sql.append("where vd.status in ('NEW','UPDATED') ");
        sql.append("and vd.episode<0 ");
        sql.append("union ");
        sql.append("select ser.id,'");
        sql.append(MetaDataType.SERIES);
        sql.append("' as mediatype,ser.create_timestamp,ser.update_timestamp ");
        sql.append("from series ser, season sea, videodata vd ");
        sql.append("where ser.id=sea.series_id ");
        sql.append("and sea.id=vd.season_id ");
        sql.append("and (ser.status in ('NEW','UPDATED') ");
        sql.append(" or  (ser.status='DONE' and sea.status in ('NEW','UPDATED')) ");
        sql.append(" or  (ser.status='DONE' and vd.status in ('NEW','UPDATED'))) ");

        return metadataDao.getMetadataQueue(sql, maxResults);
    }

    @Transactional(readOnly = true)
    public List<QueueDTO> getPersonQueueForScanning(final int maxResults) {
        final StringBuilder sql = new StringBuilder();
        sql.append("select id, '");
        sql.append(MetaDataType.PERSON);
        sql.append("' as metatype, create_timestamp, update_timestamp ");
        sql.append("from person ");
        sql.append("where status in ('NEW','UPDATED') ");

        return metadataDao.getMetadataQueue(sql, maxResults);
    }

    @Transactional(readOnly = true)
    public List<QueueDTO> getFilmographyQueueForScanning(final int maxResults) {
        final StringBuilder sql = new StringBuilder();
        sql.append("select id, '");
        sql.append(MetaDataType.FILMOGRAPHY);
        sql.append("' as metatype, create_timestamp, update_timestamp ");
        sql.append("from person ");
        sql.append("where status='DONE' ");
        sql.append("and (filmography_status is null or filmography_status in ('NEW','UPDATED')) ");

        return metadataDao.getMetadataQueue(sql, maxResults);
    }

    @Transactional(readOnly = true)
    public VideoData getRequiredVideoData(Long id) {
        final StringBuilder sb = new StringBuilder();
        sb.append("from VideoData vd ");
        sb.append("where vd.id = :id ");

        List<VideoData> objects = this.commonDao.findById(sb, id);
        return DataAccessUtils.requiredUniqueResult(objects);
    }

    @Transactional(readOnly = true)
    public Series getRequiredSeries(Long id) {
        final StringBuilder sb = new StringBuilder();
        sb.append("from Series ser ");
        sb.append("join fetch ser.seasons sea ");
        sb.append("join fetch sea.videoDatas vd ");
        sb.append("where ser.id = :id ");

        List<Series> objects = this.commonDao.findById(sb, id);
        return DataAccessUtils.requiredUniqueResult(objects);
    }

    @Transactional(readOnly = true)
    public Person getRequiredPerson(Long id) {
        final StringBuilder sb = new StringBuilder();
        sb.append("from Person p ");
        sb.append("where p.id = :id ");

        List<Person> objects = this.commonDao.findById(sb, id);
        return DataAccessUtils.requiredUniqueResult(objects);
    }

    /**
     * Store associated entities, like genres or cast.
     *
     * @param videoData
     */
    public void storeAssociatedEntities(VideoData videoData) {

        if (CollectionUtils.isNotEmpty(videoData.getGenreNames())) {
            // store new genres
            for (String genreName : videoData.getGenreNames()) {
                try {
                    String targetXml = GenreXmlTools.getMasterGenre(genreName);
                    this.commonDao.storeNewGenre(genreName, targetXml);
                } catch (Exception ex) {
                    LOG.error("Failed to store genre '{}', error: {}", genreName, ex.getMessage());
                    LOG.trace("Storage error", ex);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(videoData.getStudioNames())) {
            // store new studios
            for (String studioName : videoData.getStudioNames()) {
                try {
                    this.commonDao.storeNewStudio(studioName);
                } catch (Exception ex) {
                    LOG.error("Failed to store studio '{}', error: {}", studioName, ex.getMessage());
                    LOG.trace("Storage error", ex);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(videoData.getCountryNames())) {
            // store new countries
            for (String countryName : videoData.getCountryNames()) {
                try {
                    String targetXml = CountryXmlTools.getMasterCountry(countryName);
                    this.commonDao.storeNewCountry(countryName, targetXml);
                } catch (Exception ex) {
                    LOG.error("Failed to store country '{}', error: {}", countryName, ex.getMessage());
                    LOG.trace("Storage error", ex);
                }
            }
        }

        if (MapUtils.isNotEmpty(videoData.getCertificationInfos())) {
            // store new certifications
            for (Entry<Locale,String> entry : videoData.getCertificationInfos().entrySet()) {
                try {
                    this.commonDao.storeNewCertification(entry.getKey(), entry.getValue());
                } catch (Exception ex) {
                    LOG.error("Failed to store certification '{}'-'{}', error: {}", entry.getKey(), entry.getValue(), ex.getMessage());
                    LOG.trace("Storage error", ex);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(videoData.getCreditDTOS())) {
            // store persons
            for (CreditDTO creditDTO : videoData.getCreditDTOS()) {
                try {
                    this.metadataDao.storePerson(creditDTO);
                } catch (Exception ex) {
                    LOG.error("Failed to store person '{}', error: {}", creditDTO.getName(), ex.getMessage());
                    LOG.trace("Storage error", ex);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(videoData.getBoxedSetDTOS())) {
            // store boxed sets
            for (BoxedSetDTO boxedSetDTO : videoData.getBoxedSetDTOS()) {
                try {
                    this.commonDao.storeNewBoxedSet(boxedSetDTO);
                } catch (Exception ex) {
                    LOG.error("Failed to store boxed set '{}', error: {}", boxedSetDTO.getName(), ex.getMessage());
                    LOG.trace("Storage error", ex);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(videoData.getAwardDTOS())) {
            // store award events
            for (AwardDTO awardDTO : videoData.getAwardDTOS()) {
                try {
                    this.commonDao.storeNewAward(awardDTO);
                } catch (Exception ex) {
                    LOG.error("Failed to store award '{}'-'{}', error: {}", awardDTO.getEvent(), awardDTO.getCategory(), ex.getMessage());
                    LOG.trace("Storage error", ex);
                }
            }
        }
    }

    /**
     * Store associated entities, like genres or cast.
     *
     * @param series
     */
    public void storeAssociatedEntities(Series series) {

        if (CollectionUtils.isNotEmpty(series.getGenreNames())) {
            // store new genres
            for (String genreName : series.getGenreNames()) {
                try {
                    String targetXml = GenreXmlTools.getMasterGenre(genreName);
                    this.commonDao.storeNewGenre(genreName, targetXml);
                } catch (Exception ex) {
                    LOG.error("Failed to store genre '{}', error: {}", genreName, ex.getMessage());
                    LOG.trace("Storage error", ex);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(series.getStudioNames())) {
            // store new studios
            for (String studioName : series.getStudioNames()) {
                try {
                    this.commonDao.storeNewStudio(studioName);
                } catch (Exception ex) {
                    LOG.error("Failed to store studio '{}', error: {}", studioName, ex.getMessage());
                    LOG.trace("Storage error", ex);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(series.getCountryNames())) {
            // store new countries
            for (String countryName : series.getCountryNames()) {
                try {
                    String targetXml = CountryXmlTools.getMasterCountry(countryName);
                    this.commonDao.storeNewCountry(countryName, targetXml);
                } catch (Exception ex) {
                    LOG.error("Failed to store country '{}', error: {}", countryName, ex.getMessage());
                    LOG.trace("Storage error", ex);
                }
            }
        }

        if (MapUtils.isNotEmpty(series.getCertificationInfos())) {
            // store new certifications
            for (Entry<Locale,String> entry : series.getCertificationInfos().entrySet()) {
                try {
                    this.commonDao.storeNewCertification(entry.getKey(), entry.getValue());
                } catch (Exception ex) {
                    LOG.error("Failed to store certification '{}'-'{}', error: {}", entry.getKey(), entry.getValue(), ex.getMessage());
                    LOG.trace("Storage error", ex);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(series.getBoxedSetDTOS())) {
            // store boxed sets
            for (BoxedSetDTO boxedSetDTO : series.getBoxedSetDTOS()) {
                try {
                    this.commonDao.storeNewBoxedSet(boxedSetDTO);
                } catch (Exception ex) {
                    LOG.error("Failed to store boxed set '{}', error: {}", boxedSetDTO.getName(), ex.getMessage());
                    LOG.trace("Storage error", ex);
                }
            }
        }
        
        if (CollectionUtils.isNotEmpty(series.getAwardDTOS())) {
          // store award events
          for (AwardDTO awardDTO : series.getAwardDTOS()) {
              try {
                  this.commonDao.storeNewAward(awardDTO);
              } catch (Exception ex) {
                  LOG.error("Failed to store award '{}'-'{}', error: {}", awardDTO.getEvent(), awardDTO.getCategory(), ex.getMessage());
                  LOG.trace("Storage error", ex);
              }
          }
      }

        for (Season season : series.getSeasons()) {
            for (VideoData videoData : season.getVideoDatas()) {
                this.storeAssociatedEntities(videoData);
            }
        }
    }

    @Transactional
    public void updateScannedPerson(Person person) {
        // update entity
        person.fixScannedValues();
        person.setLastScanned(new Date(System.currentTimeMillis()));
        metadataDao.updateEntity(person);

        // store artwork
        Artwork photo = person.getPhoto();
        if (photo == null) {
            photo = new Artwork();
            photo.setArtworkType(ArtworkType.PHOTO);
            photo.setPerson(person);
            photo.setStatus(StatusType.NEW);
            person.setPhoto(photo);
            this.artworkDao.saveEntity(photo);
        }

        // update artwork
        this.updateLocatedArtwork(person);
    }

    @Transactional
    public void updateScannedPersonFilmography(Person person) {
        // update entity
        metadataDao.updateEntity(person);

        if (!StatusType.DONE.equals(person.getFilmographyStatus())) {
            // filmography must have been scanned
            return;
        }

        // NOTE: participations are stored by cascade

        // holds the participations to delete
        Set<FilmParticipation> deletions = new HashSet<>();

        for (FilmParticipation filmo : person.getFilmography()) {

            FilmParticipation newFilmo = null;
            for (FilmParticipation fp : person.getNewFilmography()) {
                if (filmo.equals(fp)) {
                    newFilmo = fp;
                    break;
                }
            }

            if (newFilmo == null) {
                // actual participation should be deleted
                deletions.add(filmo);
            } else {
                // merge new participation into existing participation
                filmo.merge(newFilmo);
                // remove participation from new participations
                person.getNewFilmography().remove(filmo);
            }
        }

        // delete old participations
        person.getFilmography().removeAll(deletions);
        // store new participations
        person.getFilmography().addAll(person.getNewFilmography());
    }

    @Transactional(timeout=120)
    public void updateScannedMetaData(VideoData videoData) {
        // replace temporary done
        if (StatusType.TEMP_DONE.equals(videoData.getStatus())) {
            videoData.setStatus(StatusType.DONE);
        }

        // update entity
        videoData.setLastScanned(new Date(System.currentTimeMillis()));
        metadataDao.updateEntity(videoData);

        // update genres
        updateGenres(videoData);

        // update studios
        updateStudios(videoData);

        // update countries
        updateCountries(videoData);

        // update cast and crew
        updateCastCrew(videoData);

        // update boxed sets
        updateBoxedSets(videoData);

        // update certifications
        updateCertifications(videoData);

        // update awards
        updateAwards(videoData);

        // update artwork
        updateLocatedArtwork(videoData);
    }

    @Transactional(timeout=120)
    public void updateScannedMetaData(Series series) {
        // update entity
        series.setLastScanned(new Date(System.currentTimeMillis()));
        metadataDao.updateEntity(series);

        // update genres
        updateGenres(series);

        // update studios
        updateStudios(series);

        // update countries
        updateCountries(series);

        // update certifications
        updateCertifications(series);

        // update awards
        updateAwards(series);

        // update artwork
        updateLocatedArtwork(series);

        // update underlying seasons and episodes
        for (Season season : series.getSeasons()) {
            season.setLastScanned(series.getLastScanned());
            metadataDao.updateEntity(season);

            for (VideoData videoData : season.getVideoDatas()) {
                updateScannedMetaData(videoData);
            }
        }
    }

    /**
     * Update genres for VideoData from the database
     *
     * @param videoData
     */
    private void updateGenres(VideoData videoData) {
        if (CollectionUtils.isEmpty(videoData.getGenreNames())) {
            return;
        }

        Set<Genre> genres = new LinkedHashSet<>();
        for (String genreName : videoData.getGenreNames()) {
            Genre genre = commonDao.getGenre(genreName);
            if (genre != null) {
                genres.add(genre);
            }
        }
        videoData.setGenres(genres);
    }

    /**
     * Update genres for Series from the database
     *
     * @param series
     */
    private void updateGenres(Series series) {
        if (CollectionUtils.isEmpty(series.getGenreNames())) {
            return;
        }

        Set<Genre> genres = new LinkedHashSet<>();
        for (String genreName : series.getGenreNames()) {
            Genre genre = commonDao.getGenre(genreName);
            if (genre != null) {
                genres.add(genre);
            }
        }
        series.setGenres(genres);
    }

    /**
     * Update studios for VideoData from the database
     *
     * @param videoData
     */
    private void updateStudios(VideoData videoData) {
        if (CollectionUtils.isEmpty(videoData.getStudioNames())) {
            return;
        }

        Set<Studio> studios = new LinkedHashSet<>();
        for (String studioName : videoData.getStudioNames()) {
            Studio studio = commonDao.getStudio(studioName);
            if (studio != null) {
                studios.add(studio);
            }
        }
        videoData.setStudios(studios);
    }

    /**
     * Update studios for Series from the database
     *
     * @param series
     */
    private void updateStudios(Series series) {
        if (CollectionUtils.isEmpty(series.getStudioNames())) {
            return;
        }

        Set<Studio> studios = new LinkedHashSet<>();
        for (String studioName : series.getStudioNames()) {
            Studio studio = commonDao.getStudio(studioName);
            if (studio != null) {
                studios.add(studio);
            }
        }
        series.setStudios(studios);
    }

    /**
     * Update countries for VideoData from the database
     *
     * @param series
     */
    private void updateCountries(VideoData videoData) {
        if (CollectionUtils.isEmpty(videoData.getCountryNames())) {
            return;
        }

        Set<Country> countries = new LinkedHashSet<>();
        for (String countryName : videoData.getCountryNames()) {
            Country country = commonDao.getCountry(countryName);
            if (country != null) {
                countries.add(country);
            }
        }
        videoData.setCountries(countries);
    }

    /**
     * Update countries for Series from the database
     *
     * @param series
     */
    private void updateCountries(Series series) {
        if (CollectionUtils.isEmpty(series.getCountryNames())) {
            return;
        }

        Set<Country> countries = new LinkedHashSet<>();
        for (String countryName : series.getCountryNames()) {
            Country country = commonDao.getCountry(countryName);
            if (country != null) {
                countries.add(country);
            }
        }
        series.setCountries(countries);
    }

    /**
     * Update certifications for VideoData from the database
     *
     * @param videoData
     */
    private void updateCertifications(VideoData videoData) {
        if (MapUtils.isEmpty(videoData.getCertificationInfos())) {
            return;
        }

        Set<Certification> certifications = new LinkedHashSet<>();
        for (Entry<Locale,String> entry : videoData.getCertificationInfos().entrySet()) {
            Certification certification = commonDao.getCertification(entry.getKey(), entry.getValue());
            if (certification != null) {
                certifications.add(certification);
            }
        }
        videoData.setCertifications(certifications);
    }

    /**
     * Update certifications for Series from the database
     *
     * @param series
     */
    private void updateCertifications(Series series) {
        if (MapUtils.isEmpty(series.getCertificationInfos())) {
            return;
        }

        Set<Certification> certifications = new LinkedHashSet<>();
        for (Entry<Locale,String> entry : series.getCertificationInfos().entrySet()) {
            Certification certification = commonDao.getCertification(entry.getKey(), entry.getValue());
            if (certification != null) {
                certifications.add(certification);
            }
        }
        series.setCertifications(certifications);
    }

    /**
     * Update awards for VideoData from the database
     *
     * @param videoData
     */
    private void updateAwards(VideoData videoData) {
        if (CollectionUtils.isEmpty(videoData.getAwardDTOS())) {
            return;
        }

        List<MovieAward> deleteAwards = new ArrayList<>(videoData.getMovieAwards());

        for (AwardDTO dto : videoData.getAwardDTOS()) {
            Award award = this.commonDao.getAward(dto);
            if (award != null) {
                MovieAward movieAward = new MovieAward(videoData, award, dto.getYear());
                movieAward.setWon(dto.isWon());
                movieAward.setNominated(dto.isNominated());
                int index = videoData.getMovieAwards().indexOf(movieAward);

                if (index < 0) {
                    // new award
                    videoData.getMovieAwards().add(movieAward);
                } else {
                    // remove from deletion awards
                    movieAward = videoData.getMovieAwards().get(index);
                    deleteAwards.remove(movieAward);
                }
            }
        }
        // delete orphans
        videoData.getMovieAwards().removeAll(deleteAwards);
    }

    /**
     * Update awards for Series from the database
     *
     * @param series
     */
    private void updateAwards(Series series) {
        if (CollectionUtils.isEmpty(series.getAwardDTOS())) {
            return;
        }

        List<SeriesAward> deleteAwards = new ArrayList<>(series.getSeriesAwards());

        for (AwardDTO dto : series.getAwardDTOS()) {
            Award award = this.commonDao.getAward(dto);
            if (award != null) {
                SeriesAward seriesAward = new SeriesAward(series, award, dto.getYear());
                seriesAward.setWon(dto.isWon());
                seriesAward.setNominated(dto.isNominated());
                int index = series.getSeriesAwards().indexOf(seriesAward);

                if (index < 0) {
                    // new award
                    series.getSeriesAwards().add(seriesAward);
                } else {
                    // remove from deletion awards
                    seriesAward = series.getSeriesAwards().get(index);
                    deleteAwards.remove(seriesAward);
                }
            }
        }
        // delete orphans
        series.getSeriesAwards().removeAll(deleteAwards);
    }

    /**
     * Update boxed sets
     *
     * @param videoData
     */
    public void updateBoxedSets(VideoData videoData) {
        if (CollectionUtils.isEmpty(videoData.getBoxedSetDTOS())) {
            return;
        }

        for (BoxedSetDTO boxedSetDTO : videoData.getBoxedSetDTOS()) {

            BoxedSetOrder boxedSetOrder = null;
            for (BoxedSetOrder stored : videoData.getBoxedSets()) {
                if (StringUtils.equalsIgnoreCase(stored.getBoxedSet().getName(), boxedSetDTO.getName())) {
                    boxedSetOrder = stored;
                    break;
                }
            }

            if (boxedSetOrder == null) {
                // create new videoSet
                BoxedSet boxedSet = commonDao.getBoxedSet(boxedSetDTO.getName());
                if (boxedSet != null) {
                    boxedSetOrder = new BoxedSetOrder();
                    boxedSetOrder.setVideoData(videoData);
                    boxedSetOrder.setBoxedSet(boxedSet);
                    if (boxedSetDTO.getOrdering() != null) {
                        boxedSetOrder.setOrdering(boxedSetDTO.getOrdering());
                    }
                    
                    videoData.addBoxedSet(boxedSetOrder);
                    this.commonDao.saveEntity(boxedSetOrder);
                }
            } else {
                if (boxedSetDTO.getOrdering() == null) {
                    boxedSetOrder.setOrdering(-1);
                } else {
                    boxedSetOrder.setOrdering(boxedSetDTO.getOrdering());
                }
                this.commonDao.updateEntity(boxedSetOrder);
            }
        }
    }

    /**
     * Update boxed sets
     *
     * @param series
     */
    public void updateBoxedSets(Series series) {
        if (CollectionUtils.isEmpty(series.getBoxedSetDTOS())) {
            return;
        }

        for (BoxedSetDTO boxedSetDTO : series.getBoxedSetDTOS()) {

            BoxedSetOrder boxedSetOrder = null;
            for (BoxedSetOrder stored : series.getBoxedSets()) {
                if (StringUtils.equalsIgnoreCase(stored.getBoxedSet().getName(), boxedSetDTO.getName())) {
                    boxedSetOrder = stored;
                    break;
                }
            }

            if (boxedSetOrder == null) {
                // create new videoSet
                BoxedSet boxedSet = commonDao.getBoxedSet(boxedSetDTO.getName());
                if (boxedSet != null) {
                    boxedSetOrder = new BoxedSetOrder();
                    boxedSetOrder.setSeries(series);
                    boxedSetOrder.setBoxedSet(boxedSet);
                    if (boxedSetDTO.getOrdering() != null) {
                        boxedSetOrder.setOrdering(boxedSetDTO.getOrdering());
                    }
                    series.addBoxedSet(boxedSetOrder);
                    this.commonDao.saveEntity(boxedSetOrder);
                }
            } else {
                if (boxedSetDTO.getOrdering() == null) {
                    boxedSetOrder.setOrdering(-1);
                } else {
                    boxedSetOrder.setOrdering(boxedSetDTO.getOrdering());
                }
                this.commonDao.updateEntity(boxedSetOrder);
            }
        }
    }

    /**
     * Update cast and crew to the database.
     *
     * @param videoData
     */
    private void updateCastCrew(VideoData videoData) {
        if (CollectionUtils.isEmpty(videoData.getCreditDTOS())) {
            return;
        }

        List<CastCrew> deleteCredits = new ArrayList<>(videoData.getCredits());
        int ordering = 0; // ordering counter

        for (CreditDTO dto : videoData.getCreditDTOS()) {
            String identifier = MetadataTools.cleanIdentifier(dto.getName());
            CastCrew castCrew = this.metadataDao.getCastCrew(videoData, dto.getJobType(), identifier);

            if (castCrew == null) {
                // retrieve person
                Person person = metadataDao.getPerson(identifier);

                if (person == null) {
                    LOG.warn("Person '{}' not found, skipping", dto.getName());
                    // continue with next cast entry
                    continue;
                }
                LOG.trace("Found person '{}' for identifier '{}'", person.getName(), identifier);

                // create new association between person and video
                castCrew = new CastCrew(person, videoData, dto.getJobType());
                castCrew.setRole(StringUtils.abbreviate(dto.getRole(), 255));
                castCrew.setOrdering(ordering++);
                videoData.getCredits().add(castCrew);
            } else if (deleteCredits.contains(castCrew)) {
                // updated cast entry if not processed before
                castCrew.setRole(StringUtils.abbreviate(dto.getRole(), 255));
                castCrew.setOrdering(ordering++);
                // remove from credits to delete
                deleteCredits.remove(castCrew);
            } else if (dto.getRole() != null && StringUtils.isBlank(castCrew.getRole())) {
                // just update the role when cast member already processed
                castCrew.setRole(StringUtils.abbreviate(dto.getRole(), 255));
            }
        }
        // delete orphans
        videoData.getCredits().removeAll(deleteCredits);
    }

    private void updateLocatedArtwork(VideoData videoData) {
        if (videoData.hasModifiedSource()) {
            this.commonDao.markAsDeleted(videoData.getArtworks(), videoData.getModifiedSources());
        }
        
        if (MapUtils.isNotEmpty(videoData.getPosterURLS())) {
            Artwork artwork = videoData.getArtwork(ArtworkType.POSTER);
            updateLocatedArtwork(artwork, videoData.getPosterURLS());
        }
        if (MapUtils.isNotEmpty(videoData.getFanartURLS())) {
            Artwork artwork = videoData.getArtwork(ArtworkType.FANART);
            updateLocatedArtwork(artwork, videoData.getFanartURLS());
        }
    }

    private void updateLocatedArtwork(Series series) {
        if (series.hasModifiedSource()) {
            this.commonDao.markAsDeleted(series.getArtworks(), series.getModifiedSources());
            for (Season season : series.getSeasons()) {
                this.commonDao.markAsDeleted(season.getArtworks(), series.getModifiedSources());
                for (VideoData videoData : season.getVideoDatas()) {
                    this.commonDao.markAsDeleted(videoData.getArtworks(), series.getModifiedSources());
                }
            }
        }
        
        if (MapUtils.isNotEmpty(series.getPosterURLS())) {
            Artwork artwork = series.getArtwork(ArtworkType.POSTER);
            updateLocatedArtwork(artwork, series.getPosterURLS());
        }
        if (MapUtils.isNotEmpty(series.getFanartURLS())) {
            Artwork artwork = series.getArtwork(ArtworkType.FANART);
            updateLocatedArtwork(artwork, series.getFanartURLS());
        }
    }

    private void updateLocatedArtwork(Person person) {
        if (person.hasModifiedSource()) {
            this.commonDao.markAsDeleted(person.getPhoto(), person.getModifiedSources());
        }
        
        if (MapUtils.isNotEmpty(person.getPhotoURLS())) {
            Artwork artwork = person.getPhoto();
            updateLocatedArtwork(artwork, person.getPhotoURLS());
        }
    }

    private void updateLocatedArtwork(Artwork artwork, Map<String,String> urlMap) {
        for (Entry<String,String> entry : urlMap.entrySet()) {
            ArtworkLocated located = new ArtworkLocated();
            located.setArtwork(artwork);
            located.setSource(entry.getValue());
            located.setUrl(entry.getKey());
            located.setHashCode(ArtworkTools.getUrlHashCode(entry.getKey()));
            located.setPriority(5);
            located.setStatus(StatusType.NEW);
            
            artworkDao.saveArtworkLocated(artwork, located);
        }
    }

    @Transactional
    public void errorVideoData(Long id) {
        VideoData videoData = metadataDao.getById(VideoData.class, id);
        if (videoData != null) {
            videoData.setStatus(StatusType.ERROR);
            metadataDao.updateEntity(videoData);
        }
    }

    @Transactional
    public void errorSeries(Long id) {
        Series series = metadataDao.getById(Series.class, id);
        if (series != null) {
            series.setStatus(StatusType.ERROR);
            metadataDao.updateEntity(series);
        }
    }

    @Transactional
    public void errorPerson(Long id) {
        Person person = metadataDao.getById(Person.class, id);
        if (person != null) {
            person.setStatus(StatusType.ERROR);
            metadataDao.updateEntity(person);
        }
    }

    @Transactional
    public void errorFilmography(Long id) {
        Person person = metadataDao.getById(Person.class, id);
        if (person != null) {
            person.setFilmographyStatus(StatusType.ERROR);
            metadataDao.updateEntity(person);
        }
    }

    @Transactional
    public boolean recheckMovie(Date compareDate) {
        StringBuilder sql = new StringBuilder();
        sql.append("update VideoData vd set vd.status='UPDATED' ");
        sql.append("where vd.status not in ('NEW','UPDATED') ");
        sql.append("and (vd.lastScanned is null or vd.lastScanned<=:compareDate) ");
        sql.append("and vd.episode<0 ");

        Map<String,Object> params = Collections.singletonMap("compareDate", (Object)compareDate);
        int updated = this.commonDao.executeUpdate(sql, params);
        return (updated > 0);
    }

    @Transactional
    public boolean recheckTvShow(Date compareDate) {
        Map<String,Object> params = Collections.singletonMap("compareDate", (Object)compareDate);
        int updated = 0;
        
        StringBuilder sql = new StringBuilder();
        sql.append("update Series ser set ser.status='UPDATED' ");
        sql.append("where ser.status not in ('NEW','UPDATED') ");
        sql.append("and (ser.lastScanned is null or ser.lastScanned<=:compareDate) ");
        updated += this.commonDao.executeUpdate(sql, params);

        sql.setLength(0);
        sql.append("update Season sea set sea.status='UPDATED' ");
        sql.append("where sea.status not in ('NEW','UPDATED') ");
        sql.append("and (sea.lastScanned is null or sea.lastScanned<=:compareDate) ");
        updated += this.commonDao.executeUpdate(sql, params);

        sql.setLength(0);
        sql.append("update VideoData vd set vd.status='UPDATED' ");
        sql.append("where vd.status not in ('NEW','UPDATED') ");
        sql.append("and (vd.lastScanned is null or vd.lastScanned<=:compareDate) ");
        sql.append("and vd.episode>=0 ");
        updated += this.commonDao.executeUpdate(sql, params);
        
        return (updated > 0);
    }

    @Transactional
    public boolean recheckPerson(Date compareDate) {
        StringBuilder sql = new StringBuilder();
        sql.append("update Person p set p.status='UPDATED',p.filmographyStatus='NEW' ");
        sql.append("where p.status not in ('NEW','UPDATED') ");
        sql.append("and (p.lastScanned is null or p.lastScanned<=:compareDate) ");

        Map<String,Object> params = Collections.singletonMap("compareDate", (Object)compareDate);
        int updated = this.commonDao.executeUpdate(sql, params);
        return (updated > 0);
    }

    public void handleModifiedSources(VideoData videoData) {
        if (videoData.hasModifiedSource()) { 
            
            // mark located artwork as deleted
            this.commonDao.markAsDeleted(videoData.getArtworks(), videoData.getModifiedSources());
            // mark trailers as deleted
            this.commonDao.markAsDeleted(videoData.getTrailers());

            // clear dependencies
            videoData.getCredits().clear();
            videoData.getBoxedSets().clear();
            videoData.getCertifications().clear();
            
            // clear source based values
            for (String source : videoData.getModifiedSources()) {
                
                Iterator<MovieAward> awardIter = videoData.getMovieAwards().iterator();
                while (awardIter.hasNext()) {
                    MovieAward award = awardIter.next();
                    if (source.equals(award.getMovieAwardPK().getAward().getSourceDb())) {
                        awardIter.remove();
                    }
                }
                
                if (source.equals(videoData.getOverrideSource(OverrideFlag.GENRES))) {
                    videoData.getGenres().clear();
                }
                if (source.equals(videoData.getOverrideSource(OverrideFlag.STUDIOS))) {
                    videoData.getStudios().clear();
                }
                if (source.equals(videoData.getOverrideSource(OverrideFlag.COUNTRIES))) {
                    videoData.getCountries().clear();
                }

                // remove instance variables
                videoData.removeTitle(source);
                videoData.removeTitleOriginal(source);
                videoData.removePublicationYear(source);
                videoData.removeReleaseDate(source);
                videoData.removePlot(source);
                videoData.removeOutline(source);
                videoData.removeTagline(source);
                videoData.removeQuote(source);
                videoData.removeRating(source);
                videoData.removeTopRank();

                // remove override source at all
                videoData.removeOverrideSource(source);
            }
        }
        
        videoData.setStatus(StatusType.UPDATED);
        this.commonDao.updateEntity(videoData);
    }

    public void handleModifiedSources(Season season) {
        if (season.hasModifiedSource()) { 
            
            // mark located artwork as deleted
            this.commonDao.markAsDeleted(season.getArtworks(), season.getModifiedSources());
    
            // clear source based values
            for (String source : season.getModifiedSources()) {

                // remove instance variables
                season.removeTitle(source);
                season.removeTitleOriginal(source);
                season.removePublicationYear(source);
                season.removePlot(source);
                season.removeOutline(source);
                season.removeRating(source);

                // remove override source at all
                season.removeOverrideSource(source);
            }
    
            for (VideoData videoData : season.getVideoDatas()) {
                for (String sourceDb : season.getModifiedSources()) {
                    videoData.removeSourceDbId(sourceDb);
                }
                // merge modified sources, cause episodes may have no own source IDs
                videoData.addModifiedSources(season.getModifiedSources());
                handleModifiedSources(videoData);
            }
        }
        
        season.setStatus(StatusType.UPDATED);
        this.metadataDao.updateEntity(season);
    }

    public void handleModifiedSources(Series series) {
        if (series.hasModifiedSource()) { 
            
            // mark located artwork as deleted
            this.commonDao.markAsDeleted(series.getArtworks(), series.getModifiedSources());
            // mark trailers as deleted
            this.commonDao.markAsDeleted(series.getTrailers());

            // clear dependencies
            series.getBoxedSets().clear();
            series.getCertifications().clear();
            
            // clear source based values
            for (String source : series.getModifiedSources()) {
                
                Iterator<SeriesAward> awardIter = series.getSeriesAwards().iterator();
                while (awardIter.hasNext()) {
                    SeriesAward award = awardIter.next();
                    if (source.equals(award.getSeriesAwardPK().getAward().getSourceDb())) {
                        awardIter.remove();
                    }
                }
                
                if (source.equals(series.getOverrideSource(OverrideFlag.GENRES))) {
                    series.getGenres().clear();
                }
                if (source.equals(series.getOverrideSource(OverrideFlag.STUDIOS))) {
                    series.getStudios().clear();
                }
                if (source.equals(series.getOverrideSource(OverrideFlag.COUNTRIES))) {
                    series.getCountries().clear();
                }

                // remove instance variables
                series.removeTitle(source);
                series.removeTitleOriginal(source);
                series.removeStartYear(source);
                series.removeEndYear(source);
                series.removePlot(source);
                series.removeOutline(source);
                series.removeRating(source);

                // remove override source at all
                series.removeOverrideSource(source);
            }

            for (Season season : series.getSeasons()) {
                for (String sourceDb : series.getModifiedSources()) {
                    season.removeSourceDbId(sourceDb);
                }
                // merge modified sources, cause season may have no own source IDs
                season.addModifiedSources(series.getModifiedSources());
                handleModifiedSources(season);
            }
        }

        series.setStatus(StatusType.UPDATED);
        this.commonDao.updateEntity(series);
    }

    public void handleModifiedSources(Person person) {
        if (person.hasModifiedSource()) { 
            
            // mark located artwork as deleted
            this.commonDao.markAsDeleted(person.getPhoto(), person.getModifiedSources());
            
            // clear dependencies
            person.getFilmography().clear();
            
            // clear source based values
            for (String source : person.getModifiedSources()) {
                
                // remove instance variables
                person.removeName(source);
                person.removeFirstName(source);
                person.removeLastName(source);
                person.removeBirthDay(source);
                person.removeBirthPlace(source);
                person.removeBirthName(source);
                person.removeDeathDay(source);
                person.removeDeathPlace(source);
                person.removeBiography(source);
                
                // remove override source at all
                person.removeOverrideSource(source);
            }
        }

        person.setStatus(StatusType.UPDATED);
        person.setFilmographyStatus(StatusType.NEW);
        this.commonDao.updateEntity(person);
    }
}

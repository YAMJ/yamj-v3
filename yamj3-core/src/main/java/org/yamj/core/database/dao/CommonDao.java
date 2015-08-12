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
package org.yamj.core.database.dao;

import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.common.type.MetaDataType;
import org.yamj.common.type.StatusType;
import org.yamj.core.api.model.builder.SqlScalars;
import org.yamj.core.api.model.dto.*;
import org.yamj.core.api.options.OptionsRating;
import org.yamj.core.api.options.OptionsSingleType;
import org.yamj.core.api.wrapper.ApiWrapperList;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.award.Award;
import org.yamj.core.database.model.dto.AwardDTO;
import org.yamj.core.database.model.dto.BoxedSetDTO;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.hibernate.HibernateDao;

@Transactional
@Repository("commonDao")
public class CommonDao extends HibernateDao {

    @Cacheable(value="genre", unless="#result==null")
    public Genre getGenre(Long id) {
        return getById(Genre.class, id);
    }

    @Cacheable(value="genre", unless="#result==null")
    public Genre getGenre(String name) {
        return getByNaturalIdCaseInsensitive(Genre.class, "name", name);
    }

    public synchronized void storeNewGenre(String name, String targetXml) {
        Genre genre = this.getGenre(name);
        if (genre == null) {
            // create new genre
            genre = new Genre();
            genre.setName(name);
            genre.setTargetXml(targetXml);
            this.saveEntity(genre);
        }
    }

    public List<ApiGenreDTO> getGenres(ApiWrapperList<ApiGenreDTO> wrapper) {
        OptionsSingleType options = (OptionsSingleType) wrapper.getOptions();

        SqlScalars sqlScalars = new SqlScalars();
        sqlScalars.addScalar("name", StringType.INSTANCE);

        sqlScalars.addToSql("SELECT DISTINCT ");
        if (options.getFull()) {
            sqlScalars.addToSql("g.id as id, g.name as name, ");
            sqlScalars.addToSql("CASE ");
            sqlScalars.addToSql(" WHEN g.target_api is not null THEN g.target_api ");
            sqlScalars.addToSql(" WHEN g.target_xml is not null THEN g.target_xml ");
            sqlScalars.addToSql(" ELSE g.name ");
            sqlScalars.addToSql("END as target ");

            sqlScalars.addScalar("id", LongType.INSTANCE);
            sqlScalars.addScalar("target", StringType.INSTANCE);
        } else {
            sqlScalars.addToSql("CASE ");
            sqlScalars.addToSql(" WHEN g.target_api is not null THEN g.target_api ");
            sqlScalars.addToSql(" WHEN g.target_xml is not null THEN g.target_xml ");
            sqlScalars.addToSql(" ELSE g.name ");
            sqlScalars.addToSql("END as name ");
        }
        sqlScalars.addToSql("FROM genre g ");

        boolean addWhere = true;
        if (options.getType() != null) {
            if (MetaDataType.MOVIE == options.getType()) {
                sqlScalars.addToSql("JOIN videodata_genres vg ON g.id=vg.genre_id ");
            } else {
                sqlScalars.addToSql("JOIN series_genres sg ON g.id=sg.genre_id ");
            }
        }
        if (options.getUsed() != null && options.getUsed()) {
            sqlScalars.addToSql("WHERE (exists (select 1 from videodata_genres vg where vg.genre_id=g.id) ");
            sqlScalars.addToSql(" or exists (select 1 from series_genres sg where sg.genre_id=g.id)) ");
            addWhere = false;
        }

        sqlScalars.addToSql(options.getSearchString(addWhere));
        sqlScalars.addToSql(options.getSortString());

        return executeQueryWithTransform(ApiGenreDTO.class, sqlScalars, wrapper);
    }

    public List<ApiGenreDTO> getGenreFilename(ApiWrapperList<ApiGenreDTO> wrapper, String filename) {
        SqlScalars sqlScalars = new SqlScalars();
        sqlScalars.addToSql("SELECT g.id, g.name, ");
        sqlScalars.addToSql("CASE ");
        sqlScalars.addToSql(" WHEN target_api is not null THEN target_api ");
        sqlScalars.addToSql(" WHEN target_xml is not null THEN target_xml ");
        sqlScalars.addToSql(" ELSE name ");
        sqlScalars.addToSql("END as target ");
        sqlScalars.addToSql("FROM mediafile m, mediafile_videodata mv, videodata v, videodata_genres vg, genre g");
        sqlScalars.addToSql("WHERE m.id=mv.mediafile_id");
        sqlScalars.addToSql("AND mv.videodata_id=v.id");
        sqlScalars.addToSql("AND v.id = vg.data_id");
        sqlScalars.addToSql("AND vg.genre_id=g.id");
        sqlScalars.addToSql("AND lower(m.file_name)=:filename");

        sqlScalars.addScalar("id", LongType.INSTANCE);
        sqlScalars.addScalar("name", StringType.INSTANCE);
        sqlScalars.addScalar("target", StringType.INSTANCE);

        sqlScalars.addParameter("filename", filename.toLowerCase());

        return executeQueryWithTransform(ApiGenreDTO.class, sqlScalars, wrapper);
    }

    @Cacheable(value="studio", unless="#result==null")
    public Studio getStudio(Long id) {
        return getById(Studio.class, id);
    }

    @Cacheable(value="studio", unless="#result==null")
    public Studio getStudio(String name) {
        return getByNaturalIdCaseInsensitive(Studio.class, "name", name);
    }

    public synchronized void storeNewStudio(String name) {
        Studio studio = this.getStudio(name);
        if (studio == null) {
            // create new studio
            studio = new Studio();
            studio.setName(name);
            this.saveEntity(studio);
        }
    }

    public List<Studio> getStudios(ApiWrapperList<Studio> wrapper) {
        OptionsSingleType options = (OptionsSingleType) wrapper.getOptions();

        SqlScalars sqlScalars = new SqlScalars();
        sqlScalars.addToSql("SELECT DISTINCT stu.id as id, stu.name as name ");
        sqlScalars.addToSql("FROM studio stu ");

        boolean addWhere = true;
        if (options.getType() != null) {
            if (MetaDataType.MOVIE == options.getType()) {
                sqlScalars.addToSql("JOIN videodata_studios vs ON stu.id=vs.studio_id ");
            } else {
                sqlScalars.addToSql("JOIN series_studios ss ON stu.id=ss.studio_id ");
            }
        }
        if (options.getUsed() != null && options.getUsed()) {
            sqlScalars.addToSql("WHERE (exists (select 1 from videodata_studios vs where vs.studio_id=stu.id) ");
            sqlScalars.addToSql(" or exists (select 1 from series_studios ss where ss.studio_id=stu.id)) ");
            addWhere = false;
        }

        sqlScalars.addToSql(options.getSearchString(addWhere));
        sqlScalars.addToSql(options.getSortString());

        sqlScalars.addScalar("id", LongType.INSTANCE);
        sqlScalars.addScalar("name", StringType.INSTANCE);

        return executeQueryWithTransform(Studio.class, sqlScalars, wrapper);
    }

    @Cacheable(value="country", unless="#result==null")
    public Country getCountry(Long id) {
        return getById(Country.class, id);
    }

    @Cacheable(value="country", unless="#result==null")
    public Country getCountry(String countryCode) {
        return getByNaturalId(Country.class, "countryCode", countryCode);
    }

    public synchronized void storeNewCountry(String countryCode) {
        Country country = this.getCountry(countryCode);
        if (country == null) {
            // create new country
            country = new Country();
            country.setCountryCode(countryCode);
            this.saveEntity(country);
        }
    }

    public List<ApiCountryDTO> getCountries(ApiWrapperList<ApiCountryDTO> wrapper) {
        OptionsSingleType options = (OptionsSingleType) wrapper.getOptions();

        SqlScalars sqlScalars = new SqlScalars();
        sqlScalars.addToSql("SELECT c.id, c.country_code as countryCode ");
        sqlScalars.addToSql("FROM country c ");

        if (MetaDataType.MOVIE == options.getType()) {
            sqlScalars.addToSql("JOIN videodata_countries vc ON c.id=vc.country_id ");
        } else if (MetaDataType.SERIES == options.getType()) {
            sqlScalars.addToSql("JOIN series_countries sc ON c.id=sc.country_id ");
        }
        
        sqlScalars.addToSql("WHERE (exists (select 1 from videodata_countries vc where vc.country_id=c.id) ");
        sqlScalars.addToSql(" or exists (select 1 from series_countries sc where sc.country_id=c.id)) ");

        sqlScalars.addScalar("id", LongType.INSTANCE);
        sqlScalars.addScalar("countryCode", StringType.INSTANCE);
        sqlScalars.addToSql(options.getSortString());

        return executeQueryWithTransform(ApiCountryDTO.class, sqlScalars, wrapper);
    }

    public List<ApiCountryDTO> getCountryFilename(ApiWrapperList<ApiCountryDTO> wrapper, String filename) {
        SqlScalars sqlScalars = new SqlScalars();
        sqlScalars.addToSql("SELECT c.id, c.country_code ad countryCode ");
        sqlScalars.addToSql("FROM mediafile m, mediafile_videodata mv, videodata v, videodata_countries vc, country c ");
        sqlScalars.addToSql("WHERE m.id=mv.mediafile_id");
        sqlScalars.addToSql("AND mv.videodata_id=v.id");
        sqlScalars.addToSql("AND v.id=vc.data_id");
        sqlScalars.addToSql("AND vc.country_id=c.id");
        sqlScalars.addToSql("AND lower(m.file_name)=:filename");

        sqlScalars.addScalar("id", LongType.INSTANCE);
        sqlScalars.addScalar("countryCode", StringType.INSTANCE);
        sqlScalars.addParameter("filename", filename.toLowerCase());

        return executeQueryWithTransform(ApiCountryDTO.class, sqlScalars, wrapper);
    }

    @Cacheable(value="certification", unless="#result==null")
    public Certification getCertification(String countryCode, String certificate) {
        StringBuilder sb = new StringBuilder();
        sb.append("from Certification ");
        sb.append("where lower(country_code) = :countryCode ");
        sb.append("and lower(certificate) = :certificate ");

        Map<String, Object> params = new HashMap<>();
        params.put("countryCode", countryCode.toLowerCase());
        params.put("certificate", certificate.toLowerCase());

        return (Certification) this.findUniqueByNamedParameters(sb, params);
    }

    public synchronized void storeNewCertification(String countryCode, String certificate) {
        Certification certification = this.getCertification(countryCode, certificate);
        if (certification == null) {
            // create new certification
            certification = new Certification();
            certification.setCountryCode(countryCode);
            certification.setCertificate(certificate);
            this.saveEntity(certification);
        }
    }

    public List<ApiAwardDTO> getAwards(ApiWrapperList<ApiAwardDTO> wrapper) {
        OptionsSingleType options = (OptionsSingleType) wrapper.getOptions();
        String sortBy = options.getSortby();

        SqlScalars sqlScalars = new SqlScalars();
        sqlScalars.addToSql("SELECT DISTINCT aw.id, aw.event, aw.category, aw.sourcedb as source ");
        sqlScalars.addToSql("FROM award aw ");
        if (options.getType() != null) {
            if (MetaDataType.MOVIE == options.getType()) {
                sqlScalars.addToSql("JOIN videodata_awards va ON aw.id=va.award_id ");
            } else {
                sqlScalars.addToSql("JOIN series_awards sa ON aw.id=sa.award_id ");
            }
        }
        sqlScalars.addToSql(options.getSearchString(true));
        sqlScalars.addToSql(options.getSortString(sortBy));

        sqlScalars.addScalar("id", LongType.INSTANCE);
        sqlScalars.addScalar("event", StringType.INSTANCE);
        sqlScalars.addScalar("category", StringType.INSTANCE);
        sqlScalars.addScalar("source", StringType.INSTANCE);

        return executeQueryWithTransform(ApiAwardDTO.class, sqlScalars, wrapper);
    }

    public List<ApiCertificationDTO> getCertifications(ApiWrapperList<ApiCertificationDTO> wrapper) {
        OptionsSingleType options = (OptionsSingleType) wrapper.getOptions();

        SqlScalars sqlScalars = new SqlScalars();
        sqlScalars.addToSql("SELECT DISTINCT cert.id, cert.country_code as countryCode, cert.certificate ");

        String sortBy = options.getSortby();
        if ("certificate".equalsIgnoreCase(sortBy)) {
            sortBy = "certificate_order";
            // TODO certificate_order until now just tested with MySQL
            sqlScalars.addToSql(", CASE WHEN cast(certificate as signed)>0 THEN cast(certificate as signed) ELSE ascii(substring(lower(certificate),1,1))*1000+ascii(substring(lower(certificate),2,1)) END as certificate_order ");
        }

        sqlScalars.addToSql("FROM certification cert ");
        if (options.getType() != null) {
            if (MetaDataType.MOVIE == options.getType()) {
                sqlScalars.addToSql("JOIN videodata_certifications vc ON cert.id=vc.cert_id ");
            } else {
                sqlScalars.addToSql("JOIN series_certifications sc ON cert.id=sc.cert_id ");
            }
        }
        sqlScalars.addToSql(options.getSearchString(true));
        sqlScalars.addToSql(options.getSortString(sortBy));

        sqlScalars.addScalar("id", LongType.INSTANCE);
        sqlScalars.addScalar("countryCode", StringType.INSTANCE);
        sqlScalars.addScalar("certificate", StringType.INSTANCE);

        return executeQueryWithTransform(ApiCertificationDTO.class, sqlScalars, wrapper);
    }

    @Cacheable(value="boxset", unless="#result==null")
    public BoxedSet getBoxedSet(Long id) {
        return getById(BoxedSet.class, id);
    }

    @Cacheable(value="boxset", unless="#result==null")
    public BoxedSet getBoxedSet(String identifier) {
        return getByNaturalIdCaseInsensitive(BoxedSet.class, "identifier", identifier);
    }

    public synchronized void storeNewBoxedSet(BoxedSetDTO boxedSetDTO) {
        BoxedSet boxedSet = this.getBoxedSet(boxedSetDTO.getIdentifier());
        if (boxedSet == null) {
            // create new boxed set
            boxedSet = new BoxedSet(boxedSetDTO.getIdentifier());
            boxedSet.setName(boxedSetDTO.getName());
            if (boxedSetDTO.getSourceId() != null) {
                boxedSet.setSourceDbId(boxedSetDTO.getSource(), boxedSetDTO.getSourceId());
            }
            this.saveEntity(boxedSet);

            // create new poster artwork entry
            Artwork poster = new Artwork();
            poster.setArtworkType(ArtworkType.POSTER);
            poster.setStatus(StatusType.NEW);
            poster.setBoxedSet(boxedSet);
            this.saveEntity(poster);

            // create new fanart artwork entry
            Artwork fanart = new Artwork();
            fanart.setArtworkType(ArtworkType.FANART);
            fanart.setStatus(StatusType.NEW);
            fanart.setBoxedSet(boxedSet);
            this.saveEntity(fanart);

            // create new banner artwork entry
            Artwork banner = new Artwork();
            banner.setArtworkType(ArtworkType.BANNER);
            banner.setStatus(StatusType.NEW);
            banner.setBoxedSet(boxedSet);
            this.saveEntity(banner);
        } else if (boxedSetDTO.getSourceId() != null) {
            boxedSet.setSourceDbId(boxedSetDTO.getSource(), boxedSetDTO.getSourceId());
            this.updateEntity(boxedSet);
        }
    }

    public List<ApiRatingDTO> getRatings(ApiWrapperList<ApiRatingDTO> wrapper) {
        OptionsRating options = (OptionsRating) wrapper.getOptions();

        boolean justMovie = (MetaDataType.MOVIE == options.getType());
        boolean justSeries = (MetaDataType.SERIES == options.getType());

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT DISTINCT ");
        sb.append("grouped.type as type, ");
        sb.append("grouped.sourcedb as source ");
        if (options.getRating() == null || options.getRating()) {
            sb.append(", round(grouped.rating/10) as rating ");
        }
        sb.append("FROM ( ");

        if (!justSeries) {
            // not just series
            if (StringUtils.isBlank(options.getSource())) {
                sb.append("select distinct '");
                sb.append(MetaDataType.MOVIE.toString());
                sb.append("' as type, v1.rating, v1.sourcedb, v1.videodata_id, 2 as ordering ");
                sb.append("from videodata_ratings v1 ");
                sb.append("UNION ");
                sb.append("select distinct '");
                sb.append(MetaDataType.MOVIE.toString());
                sb.append("' as type, avg(v2.rating) as rating, 'combined' as sourcedb, v2.videodata_id, 1 as ordering ");
                sb.append("from videodata_ratings v2 ");
                sb.append("group by v2.videodata_id ");
            } else if ("combined".equalsIgnoreCase(options.getSource())) {
                sb.append("select distinct '");
                sb.append(MetaDataType.MOVIE.toString());
                sb.append("' as type, avg(v2.rating) as rating, 'combined' as sourcedb, v2.videodata_id, 1 as ordering ");
                sb.append("from videodata_ratings v2 ");
                sb.append("group by v2.videodata_id ");
            } else {
                sb.append("select distinct '");
                sb.append(MetaDataType.MOVIE.toString());
                sb.append("' as type, v1.rating, v1.sourcedb, v1.videodata_id, 2 as ordering ");
                sb.append("from videodata_ratings v1 ");
                sb.append("where v1.sourcedb='");
                sb.append(options.getSource().toLowerCase());
                sb.append("' ");
            }
        }
        if (!justMovie) {
            if (!justSeries) {
                sb.append("UNION ");
            }
            // not just movies
            if (StringUtils.isBlank(options.getSource())) {
                sb.append("select distinct '");
                sb.append(MetaDataType.SERIES.toString());
                sb.append("' as type, s1.rating, s1.sourcedb, s1.series_id, 2 as ordering ");
                sb.append("from series_ratings s1 ");
                sb.append("UNION ");
                sb.append("select distinct '");
                sb.append(MetaDataType.SERIES.toString());
                sb.append("' as type, avg(s2.rating) as rating, 'combined' as sourcedb, s2.series_id, 1 as ordering ");
                sb.append("from series_ratings s2 ");
                sb.append("group by s2.series_id");
            } else if ("combined".equalsIgnoreCase(options.getSource())) {
                sb.append("select distinct '");
                sb.append(MetaDataType.SERIES.toString());
                sb.append("' as type, avg(s2.rating) as rating, 'combined' as sourcedb, s2.series_id, 1 as ordering ");
                sb.append("from series_ratings s2 ");
                sb.append("group by s2.series_id ");
            } else {
                sb.append("select distinct '");
                sb.append(MetaDataType.SERIES.toString());
                sb.append("' as type, s1.rating, s1.sourcedb, s1.series_id, 2 as ordering ");
                sb.append("from series_ratings s1 ");
                sb.append("where s1.sourcedb='");
                sb.append(options.getSource().toLowerCase());
                sb.append("' ");
            }
        }
        sb.append(") as grouped ");

        // order by
        sb.append("order by type, grouped.ordering, source");
        if (options.getRating() == null || options.getRating()) {
            sb.append(", rating");
        }
        sb.append("DESC".equalsIgnoreCase(options.getSortdir()) ? " DESC" : " ASC");

        // add scalars
        SqlScalars sqlScalars = new SqlScalars(sb);
        sqlScalars.addScalar("type", StringType.INSTANCE);
        sqlScalars.addScalar("source", StringType.INSTANCE);
        if (options.getRating() == null || options.getRating()) {
            sqlScalars.addScalar("rating", IntegerType.INSTANCE);
        }

        return executeQueryWithTransform(ApiRatingDTO.class, sqlScalars, wrapper);
    }

    @Cacheable(value="award", unless="#result==null")
    public Award getAward(String event, String category, String source) {
        return (Award) currentSession()
                .byNaturalId(Award.class)
                .using("event", event)
                .using("category", category)
                .using("sourceDb", source)
                .load();
    }

    public synchronized void storeNewAward(AwardDTO awardDTO) {
        Award award = this.getAward(awardDTO.getEvent(), awardDTO.getCategory(), awardDTO.getSource());
        if (award == null) {
            // create new award event
            award = new Award(awardDTO.getEvent(), awardDTO.getCategory(), awardDTO.getSource());
            this.saveEntity(award);
        }
    }

    public List<Long> getSeasonVideoIds(Long id) {
        SqlScalars sqlScalars = new SqlScalars();

        sqlScalars.addToSql("SELECT DISTINCT vid.id ");
        sqlScalars.addToSql("FROM season sea, videodata vid ");
        sqlScalars.addToSql("WHERE vid.season_id=sea.id ");
        sqlScalars.addToSql("AND sea.id=:id ");

        sqlScalars.addParameter("id", id);
        sqlScalars.addScalar("id", LongType.INSTANCE);
        return executeQueryWithTransform(Long.class, sqlScalars, null);
    }

    public List<Long> getSeriesVideoIds(Long id) {
        // add scalars
        SqlScalars sqlScalars = new SqlScalars();

        sqlScalars.addToSql("SELECT DISTINCT vid.id ");
        sqlScalars.addToSql("FROM videodata vid, series ser, season sea ");
        sqlScalars.addToSql("WHERE vid.season_id=sea.id ");
        sqlScalars.addToSql("and sea.series_id=ser.id ");
        sqlScalars.addToSql("AND sea.id=:id ");

        sqlScalars.addParameter("id", id);
        sqlScalars.addScalar("id", LongType.INSTANCE);
        return executeQueryWithTransform(Long.class, sqlScalars, null);
    }


    public void markAsDeleted(List<Artwork> artworks, Set<String> sources) {
        for (Artwork artwork : artworks) {
            markAsDeleted(artwork, sources);
       }
    }

    public void markAsDeleted(Artwork artwork, Set<String> sources) {
        markAsUpdated(artwork);
        for (ArtworkLocated located : artwork.getArtworkLocated()) {
            if (located.getUrl() != null 
                && sources.contains(located.getSource())
                && !StatusType.DELETED.equals(located.getStatus()))
            {
                located.setStatus(StatusType.DELETED);
                this.updateEntity(located);
            }
        }
    }

    public void markAsDeleted(List<Trailer> trailers) {
        for (Trailer trailer : trailers) {
            markAsDeleted(trailer);
       }
    }

    public void markAsDeleted(Trailer trailer) {
        if (!StatusType.DELETED.equals(trailer.getStatus())) {
            trailer.setStatus(StatusType.DELETED);
            this.updateEntity(trailer);
        }
    }
    
    public void markAsUpdated(List<Artwork> artworks) {
        for (Artwork artwork : artworks) {
            this.markAsUpdated(artwork);
        }
    }

    public void markAsUpdated(Artwork artwork) {
        if (!StatusType.NEW.equals(artwork.getStatus()) && !StatusType.UPDATED.equals(artwork.getStatus())) {
            artwork.setStatus(StatusType.UPDATED);
            this.updateEntity(artwork);
        }
    }

    public void markAsUpdated(Person person) {
        if (!StatusType.NEW.equals(person.getStatus()) && !StatusType.UPDATED.equals(person.getStatus())) {
            person.setStatus(StatusType.UPDATED);
            person.setFilmographyStatus(StatusType.UPDATED);
            this.updateEntity(person);
        }
    }

    public void markAsUpdatedForFilmography(Person person) {
        if (!StatusType.NEW.equals(person.getFilmographyStatus()) && !StatusType.UPDATED.equals(person.getFilmographyStatus())) {
            person.setFilmographyStatus(StatusType.UPDATED);
            this.updateEntity(person);
        }
    }

    public void markAsUpdated(VideoData videoData) {
        if (!StatusType.NEW.equals(videoData.getStatus()) && !StatusType.UPDATED.equals(videoData.getStatus())) {
            videoData.setStatus(StatusType.UPDATED);
            if (videoData.isMovie()) {
                videoData.setTrailerStatus(StatusType.UPDATED);
            }
            this.updateEntity(videoData);
        }
    }

    public void markAsUpdatedForTrailers(VideoData videoData) {
        if (!StatusType.NEW.equals(videoData.getTrailerStatus()) && !StatusType.UPDATED.equals(videoData.getTrailerStatus())) {
            videoData.setTrailerStatus(StatusType.UPDATED);
            this.updateEntity(videoData);
        }
    }

    public void markAsUpdated(Season season) {
        if (!StatusType.NEW.equals(season.getStatus()) && !StatusType.UPDATED.equals(season.getStatus())) {
            season.setStatus(StatusType.UPDATED);
            this.updateEntity(season);
        }
    }

    public void markAsUpdated(Series series) {
        if (!StatusType.NEW.equals(series.getStatus()) && !StatusType.UPDATED.equals(series.getStatus())) {
            series.setStatus(StatusType.UPDATED);
            series.setTrailerStatus(StatusType.UPDATED);
            this.updateEntity(series);
        }
    }

    public void markAsUpdatedForTrailers(Series series) {
        if (!StatusType.NEW.equals(series.getTrailerStatus()) && !StatusType.UPDATED.equals(series.getTrailerStatus())) {
            series.setTrailerStatus(StatusType.UPDATED);
            this.updateEntity(series);
        }
    }
}

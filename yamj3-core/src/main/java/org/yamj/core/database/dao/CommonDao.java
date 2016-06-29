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

import static org.yamj.core.CachingNames.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.CacheMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;
import org.springframework.cache.annotation.CachePut;
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
import org.yamj.core.database.model.dto.BoxedSetDTO;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.hibernate.HibernateDao;
import org.yamj.plugin.api.model.type.ArtworkType;

@Transactional
@Repository("commonDao")
public class CommonDao extends HibernateDao {

    public List<QueueDTO> getQueueIdOnly(final String queryName, final int maxResults) {
        final List<QueueDTO> queueElements = new ArrayList<>(maxResults);
        
        try (ScrollableResults scroll = currentSession().getNamedQuery(queryName)
                .setReadOnly(true)
                .setMaxResults(maxResults)
                .scroll(ScrollMode.FORWARD_ONLY)
            )
        {
            Object[] row;
            while (scroll.next()) {
                row = scroll.get();
                queueElements.add(new QueueDTO(convertRowElementToLong(row[0])));
            }
        }
        
        return queueElements;
    }

    @Cacheable(value=DB_GENRE, key="#id", unless="#result==null")
    public Genre getGenre(Long id) {
        return getById(Genre.class, id);
    }

    @Cacheable(value=DB_GENRE, key="#name.toLowerCase()", unless="#result==null")
    public Genre getGenre(String name) {
        return getByNaturalIdCaseInsensitive(Genre.class, "name", name);
    }

    @CachePut(value=DB_GENRE, key="#name.toLowerCase()")
    public Genre saveGenre(String name, String targetXml) {
        Genre genre = new Genre();
        genre.setName(name);
        genre.setTargetXml(targetXml);
        this.saveEntity(genre);
        return genre;
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

    @Cacheable(value=DB_STUDIO, key="#id", unless="#result==null")
    public Studio getStudio(Long id) {
        return getById(Studio.class, id);
    }

    @Cacheable(value=DB_STUDIO, key="#name.toLowerCase()", unless="#result==null")
    public Studio getStudio(String name) {
        return getByNaturalIdCaseInsensitive(Studio.class, "name", name);
    }

    @CachePut(value=DB_STUDIO, key="#name.toLowerCase()")
    public Studio saveStudio(String name) {
        Studio studio = new Studio();
        studio.setName(name);
        this.saveEntity(studio);
        return studio;
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

    @Cacheable(value=DB_COUNTRY, key="#id", unless="#result==null")
    public Country getCountry(Long id) {
        return getById(Country.class, id);
    }

    @Cacheable(value=DB_COUNTRY, key="#countryCode", unless="#result==null")
    public Country getCountry(String countryCode) {
        return getByNaturalId(Country.class, "countryCode", countryCode);
    }

    @CachePut(value=DB_COUNTRY, key="#countryCode")
    public Country saveCountry(String countryCode) {
        Country country = new Country();
        country.setCountryCode(countryCode);
        this.saveEntity(country);
        return country;
    }

    public List<ApiCountryDTO> getCountries(ApiWrapperList<ApiCountryDTO> wrapper) {
        OptionsSingleType options = (OptionsSingleType) wrapper.getOptions();

        SqlScalars sqlScalars = new SqlScalars();
        sqlScalars.addToSql("SELECT DISTINCT c.id, c.country_code as countryCode ");
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

    @Cacheable(value=DB_CERTIFICATION, key="{#countryCode.toLowerCase(), #certificate.toLowerCase()}", unless="#result==null")
    public Certification getCertification(String countryCode, String certificate) {
        return (Certification) currentSession().getNamedQuery(Certification.QUERY_GET)
                .setString("countryCode", countryCode.toLowerCase())
                .setString("certificate", certificate.toLowerCase())
                .setCacheable(true)
                .setCacheMode(CacheMode.NORMAL)
                .uniqueResult();
    }

    @CachePut(value=DB_CERTIFICATION, key="{#countryCode.toLowerCase(), #certificate.toLowerCase()}")
    public Certification saveCertification(String countryCode, String certificate) {
        Certification certification = new Certification();
        certification.setCountryCode(countryCode);
        certification.setCertificate(certificate);
        this.saveEntity(certification);
        return certification;
    }

    public List<ApiAwardDTO> getAwards(ApiWrapperList<ApiAwardDTO> wrapper) {
        OptionsSingleType options = (OptionsSingleType) wrapper.getOptions();

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
        sqlScalars.addToSql(options.getSortString());

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

    @Cacheable(value=DB_BOXEDSET, key="#id", unless="#result==null")
    public BoxedSet getBoxedSet(Long id) {
        return getById(BoxedSet.class, id);
    }

    public void storeNewBoxedSet(BoxedSetDTO dto) {
        BoxedSet boxedSet = getByNaturalIdCaseInsensitive(BoxedSet.class, IDENTIFIER, dto.getIdentifier());
        
        if (boxedSet == null) {
            // create new boxed set
            boxedSet = new BoxedSet(dto.getIdentifier());
            boxedSet.setName(dto.getName());
            boxedSet.setSourceDbId(dto.getSource(), dto.getSourceId());
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
        } else if (dto.getSourceId() != null) {
            boxedSet.setSourceDbId(dto.getSource(), dto.getSourceId());
            this.updateEntity(boxedSet);
        }
        
        // set boxed set id for later use
        dto.setBoxedSetId(boxedSet.getId());
    }

    public List<ApiRatingDTO> getRatings(ApiWrapperList<ApiRatingDTO> wrapper) {
        OptionsRating options = (OptionsRating) wrapper.getOptions();

        boolean justMovie = (MetaDataType.MOVIE == options.getType()); //NOSONAR
        boolean justSeries = (MetaDataType.SERIES == options.getType()); //NOSONAR

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT DISTINCT ");
        sb.append("grouped.type as type,");
        sb.append("grouped.sourcedb as source,");
        sb.append("grouped.ordering as ordering");
        if (options.getRating() == null || options.getRating()) {
            sb.append(", round(grouped.rating/10) as rating");
        }
        sb.append(" FROM ( ");

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
        sb.append("order by type, ordering, source");
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

    @Cacheable(value=DB_AWARD, key="{#event, #category, #source}", unless="#result==null")
    public Award getAward(String event, String category, String source) {
        return currentSession()
                .byNaturalId(Award.class)
                .using("event", event)
                .using("category", category)
                .using("sourceDb", source)
                .load();
    }

    @CachePut(value=DB_AWARD, key="{#event, #category, #source}")
    public Award saveAward(String event, String category, String source) {
        Award award = new Award(event, category, source);
        this.saveEntity(award);
        return award;
    }

    public void markAsDeleted(List<Artwork> artworks, Set<String> sources) {
        for (Artwork artwork : artworks) {
            markAsDeleted(artwork, sources);
       }
    }

    public void markAsDeleted(Artwork artwork, Set<String> sources) {
        if (artwork.isNotUpdated()) {
            artwork.setStatus(StatusType.UPDATED);
            this.updateEntity(artwork);
        }
        for (ArtworkLocated located : artwork.getArtworkLocated()) {
            if (located.getUrl() != null  && sources.contains(located.getSource()) && !located.isDeleted()) {
                located.setStatus(StatusType.DELETED);
                this.updateEntity(located);
            }
        }
    }

    public void markAsDeleted(List<Trailer> trailers) {
        for (Trailer trailer : trailers) {
            if (!trailer.isDeleted()) {
                trailer.setStatus(StatusType.DELETED);
                this.updateEntity(trailer);
            }
       }
    }
}

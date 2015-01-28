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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.common.type.MetaDataType;
import org.yamj.common.type.StatusType;
import org.yamj.core.api.model.builder.SqlScalars;
import org.yamj.core.api.model.dto.ApiAwardDTO;
import org.yamj.core.api.model.dto.ApiRatingDTO;
import org.yamj.core.api.model.dto.ApiTargetDTO;
import org.yamj.core.api.options.OptionsId;
import org.yamj.core.api.options.OptionsRating;
import org.yamj.core.api.options.OptionsSingleType;
import org.yamj.core.api.wrapper.ApiWrapperList;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.award.Award;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.hibernate.HibernateDao;

@Transactional
@Repository("commonDao")
public class CommonDao extends HibernateDao {

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

    public List<ApiTargetDTO> getGenres(ApiWrapperList<ApiTargetDTO> wrapper) {
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
        } else if (options.getUsed() != null && options.getUsed()) {
            sqlScalars.addToSql("WHERE (exists (select 1 from videodata_genres vg where vg.genre_id=g.id) ");
            sqlScalars.addToSql(" or exists (select 1 from series_genres sg where sg.genre_id=g.id)) ");
            addWhere = false;
        }

        sqlScalars.addToSql(options.getSearchString(addWhere));
        sqlScalars.addToSql(options.getSortString());

        return executeQueryWithTransform(ApiTargetDTO.class, sqlScalars, wrapper);
    }

    public List<ApiTargetDTO> getGenreFilename(ApiWrapperList<ApiTargetDTO> wrapper, String filename) {
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

        sqlScalars.addParameters("filename", filename.toLowerCase());

        return executeQueryWithTransform(ApiTargetDTO.class, sqlScalars, wrapper);
    }

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
        } else if (options.getUsed() != null && options.getUsed()) {
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

    public Country getCountry(String name) {
        return getByNaturalIdCaseInsensitive(Country.class, "name", name);
    }
  
    public synchronized void storeNewCountry(String name, String targetXml) {
      Country country = this.getCountry(name);
        if (country == null) {
            // create new country
            country = new Country();
            country.setName(name);
            country.setTargetXml(targetXml);
            this.saveEntity(country);
        }
    }

    public List<ApiTargetDTO> getCountries(ApiWrapperList<ApiTargetDTO> wrapper) {
        OptionsSingleType options = (OptionsSingleType) wrapper.getOptions();
  
        SqlScalars sqlScalars = new SqlScalars();
        sqlScalars.addScalar("name", StringType.INSTANCE);
  
        sqlScalars.addToSql("SELECT DISTINCT ");
        if (options.getFull()) {
            sqlScalars.addToSql("c.id as id, c.name as name, ");
            sqlScalars.addToSql("CASE ");
            sqlScalars.addToSql(" WHEN c.target_api is not null THEN c.target_api ");
            sqlScalars.addToSql(" WHEN c.target_xml is not null THEN c.target_xml ");
            sqlScalars.addToSql(" ELSE c.name ");
            sqlScalars.addToSql("END as target ");
  
            sqlScalars.addScalar("id", LongType.INSTANCE);
            sqlScalars.addScalar("target", StringType.INSTANCE);
        } else {
            sqlScalars.addToSql("CASE ");
            sqlScalars.addToSql(" WHEN c.target_api is not null THEN c.target_api ");
            sqlScalars.addToSql(" WHEN c.target_xml is not null THEN c.target_xml ");
            sqlScalars.addToSql(" ELSE c.name ");
            sqlScalars.addToSql("END as name ");
        }
        sqlScalars.addToSql("FROM country c ");
  
        boolean addWhere = true;
        if (options.getType() != null) {
            if (MetaDataType.MOVIE == options.getType()) {
                sqlScalars.addToSql("JOIN videodata_countries vc ON c.id=vc.country_id ");
            } else {
                sqlScalars.addToSql("JOIN series_countries sc ON c.id=sc.country_id ");
            }
        } else if (options.getUsed() != null && options.getUsed()) {
            sqlScalars.addToSql("WHERE (exists (select 1 from videodata_countries vc where vc.country_id=c.id) ");
            sqlScalars.addToSql(" or exists (select 1 from series_countries sc where sc.country_id=c.id)) ");
            addWhere = false;
        }
  
        sqlScalars.addToSql(options.getSearchString(addWhere));
        sqlScalars.addToSql(options.getSortString());
  
        return executeQueryWithTransform(ApiTargetDTO.class, sqlScalars, wrapper);
    }

    public List<ApiTargetDTO> getCountryFilename(ApiWrapperList<ApiTargetDTO> wrapper, String filename) {
        SqlScalars sqlScalars = new SqlScalars();
        sqlScalars.addToSql("SELECT c.id, c.name, ");
        sqlScalars.addToSql("CASE ");
        sqlScalars.addToSql(" WHEN target_api is not null THEN target_api ");
        sqlScalars.addToSql(" WHEN target_xml is not null THEN target_xml ");
        sqlScalars.addToSql(" ELSE name ");
        sqlScalars.addToSql("END as target ");
        sqlScalars.addToSql("FROM mediafile m, mediafile_videodata mv, videodata v, videodata_countries vc, country c ");
        sqlScalars.addToSql("WHERE m.id=mv.mediafile_id");
        sqlScalars.addToSql("AND mv.videodata_id=v.id");
        sqlScalars.addToSql("AND v.id = vc.data_id");
        sqlScalars.addToSql("AND vc.country_id=c.id");
        sqlScalars.addToSql("AND lower(m.file_name)=:filename");
  
        sqlScalars.addScalar("id", LongType.INSTANCE);
        sqlScalars.addScalar("name", StringType.INSTANCE);
        sqlScalars.addScalar("target", StringType.INSTANCE);
  
        sqlScalars.addParameters("filename", filename.toLowerCase());
  
        return executeQueryWithTransform(ApiTargetDTO.class, sqlScalars, wrapper);
    }

    public Certification getCertification(String country, String certificate) {
        StringBuilder sb = new StringBuilder();
        sb.append("from Certification ");
        sb.append("where lower(country) = :country ");
        sb.append("and lower(certificate) = :certificate ");

        Map<String, Object> params = new HashMap<>();
        params.put("country", country.toLowerCase());
        params.put("certificate", certificate.toLowerCase());

        return (Certification)this.findUniqueByNamedParameters(sb, params);
    }

    public synchronized void storeNewCertification(String country, String certificate) {
        Certification certification = this.getCertification(country, certificate);
        if (certification == null) {
            // create new certification
            certification = new Certification();
            certification.setCountry(country);
            certification.setCertificate(certificate);
            this.saveEntity(certification);
        }
    }

    public List<ApiAwardDTO> getAwards(ApiWrapperList<ApiAwardDTO> wrapper) {
        OptionsId options = (OptionsId) wrapper.getOptions();
        String sortBy = options.getSortby();

        SqlScalars sqlScalars = new SqlScalars();
        sqlScalars.addToSql("SELECT id, event, category, sourcedb as source ");
        sqlScalars.addToSql("FROM award ");
        sqlScalars.addToSql(options.getSearchString(true));
        sqlScalars.addToSql(options.getSortString(sortBy));

        sqlScalars.addScalar("id", LongType.INSTANCE);
        sqlScalars.addScalar("event", StringType.INSTANCE);
        sqlScalars.addScalar("category", StringType.INSTANCE);
        sqlScalars.addScalar("source", StringType.INSTANCE);

        return executeQueryWithTransform(ApiAwardDTO.class, sqlScalars, wrapper);
    }

    public List<Certification> getCertifications(ApiWrapperList<Certification> wrapper) {
        OptionsId options = (OptionsId) wrapper.getOptions();
        String sortBy = options.getSortby();

        SqlScalars sqlScalars = new SqlScalars();
        sqlScalars.addToSql("SELECT id, country, certificate ");

        if ("certificate".equalsIgnoreCase(sortBy)) {
            sortBy = "certificate_order";
            // TODO certificate_order until now just tested with MySQL
            sqlScalars.addToSql(", CASE WHEN cast(certificate as signed)>0 THEN cast(certificate as signed) ELSE ascii(substring(lower(certificate),1,1))*1000+ascii(substring(lower(certificate),2,1)) END as certificate_order ");
        }

        sqlScalars.addToSql("FROM certification ");
        sqlScalars.addToSql(options.getSearchString(true));
        sqlScalars.addToSql(options.getSortString(sortBy));

        sqlScalars.addScalar("id", LongType.INSTANCE);
        sqlScalars.addScalar("country", StringType.INSTANCE);
        sqlScalars.addScalar("certificate", StringType.INSTANCE);

        return executeQueryWithTransform(Certification.class, sqlScalars, wrapper);
    }

    public BoxedSet getBoxedSet(String name) {
        return getByNaturalIdCaseInsensitive(BoxedSet.class, "name", name);
    }

    public synchronized void storeNewBoxedSet(String name) {
        BoxedSet boxedSet = this.getBoxedSet(name);
        if (boxedSet == null) {
            // create new boxed set
            boxedSet = new BoxedSet();
            boxedSet.setName(name);
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

    public Award getAward(String event, String category, String source) {
        return (Award)currentSession()
                .byNaturalId(Award.class)
                .using("event", event)
                .using("category", category)
                .using("sourceDb", source)
                .load();
    }
  
    public synchronized void storeNewAward(String event, String category, String source) {
      Award awardEvent = this.getAward(event, category, source);
        if (awardEvent == null) {
            // create new award event
            awardEvent = new Award(event, category, source);
            this.saveEntity(awardEvent);
        }
    }
}

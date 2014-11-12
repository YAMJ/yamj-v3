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
package org.yamj.core.database.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.api.model.builder.SqlScalars;
import org.yamj.core.api.model.dto.ApiGenreDTO;
import org.yamj.core.api.options.OptionsCommon;
import org.yamj.core.api.options.OptionsId;
import org.yamj.core.api.wrapper.ApiWrapperList;
import org.yamj.core.database.model.BoxedSet;
import org.yamj.core.database.model.Certification;
import org.yamj.core.database.model.Genre;
import org.yamj.core.database.model.Studio;
import org.yamj.core.hibernate.HibernateDao;

@Transactional
@Repository("commonDao")
public class CommonDao extends HibernateDao {

    public Genre getGenre(String name) {
        return getByNameCaseInsensitive(Genre.class, name);
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
        OptionsCommon options = (OptionsCommon) wrapper.getOptions();
        
        SqlScalars sqlScalars = new SqlScalars();
        sqlScalars.addScalar("name", StringType.INSTANCE);

        sqlScalars.addToSql("SELECT DISTINCT ");
        if (options.getFull().booleanValue()) {
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
        } else if (options.getUsed() != null && options.getUsed().booleanValue()) {
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

        sqlScalars.addParameters("filename", filename.toLowerCase());

        return executeQueryWithTransform(ApiGenreDTO.class, sqlScalars, wrapper);
    }

    public Studio getStudio(String name) {
        return getByNameCaseInsensitive(Studio.class, name);
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
        OptionsCommon options = (OptionsCommon) wrapper.getOptions();
        
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
        } else if (options.getUsed() != null && options.getUsed().booleanValue()) {
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
    
    public Certification getCertification(String country, String certificate) {
        StringBuffer sb = new StringBuffer();
        sb.append("from Certification ");
        sb.append("where lower(country) = :country ");
        sb.append("and lower(certificate) = :certificate ");
        
        Map<String, Object> params = new HashMap<String,Object>();
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

    public List<Certification> getCertifications(ApiWrapperList<Certification> wrapper) {
        OptionsId options = (OptionsId) wrapper.getOptions();
        
        SqlScalars sqlScalars = new SqlScalars();
        sqlScalars.addToSql("SELECT id, country, certificate, ");
        
        // TODO certificate_order until now just tested with MySQL
        sqlScalars.addToSql("CASE WHEN cast(certificate as signed)>0 THEN cast(certificate as signed) ELSE ascii(substring(lower(certificate),1,1))+ascii(substring(lower(certificate),2,1)) END as certificate_order ");
        
        sqlScalars.addToSql("FROM certification ");
        sqlScalars.addToSql(options.getSearchString(true));
        sqlScalars.addToSql(options.getSortString());

        sqlScalars.addScalar("id", LongType.INSTANCE);
        sqlScalars.addScalar("country", StringType.INSTANCE);
        sqlScalars.addScalar("certificate", StringType.INSTANCE);

        return executeQueryWithTransform(Certification.class, sqlScalars, wrapper);
    }

    public BoxedSet getBoxedSet(String name) {
        return getByNameCaseInsensitive(BoxedSet.class, name);
    }

    public synchronized void storeNewBoxedSet(String name) {
        BoxedSet boxedSet = this.getBoxedSet(name);
        if (boxedSet == null) {
            // create new boxed set
            boxedSet = new BoxedSet();
            boxedSet.setName(name);
            this.saveEntity(boxedSet);
        }
    }

    public List<BoxedSet> getBoxedSets(ApiWrapperList<BoxedSet> wrapper) {
        OptionsId options = (OptionsId) wrapper.getOptions();
        
        SqlScalars sqlScalars = new SqlScalars();
        sqlScalars.addToSql("SELECT id, name FROM boxed_set");
        sqlScalars.addToSql(options.getSearchString(true));
        sqlScalars.addToSql(options.getSortString());

        sqlScalars.addScalar("id", LongType.INSTANCE);
        sqlScalars.addScalar("name", StringType.INSTANCE);

        return executeQueryWithTransform(BoxedSet.class, sqlScalars, wrapper);
    }
}

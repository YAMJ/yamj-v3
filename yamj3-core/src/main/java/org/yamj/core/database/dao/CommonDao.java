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

import java.util.List;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.core.api.model.builder.SqlScalars;
import org.yamj.core.api.model.dto.ApiGenreDTO;
import org.yamj.core.api.options.OptionsGenre;
import org.yamj.core.api.options.OptionsId;
import org.yamj.core.api.wrapper.ApiWrapperList;
import org.yamj.core.database.model.BoxedSet;
import org.yamj.core.database.model.Certification;
import org.yamj.core.database.model.Genre;
import org.yamj.core.database.model.Studio;
import org.yamj.core.hibernate.HibernateDao;

@Repository("commonDao")
public class CommonDao extends HibernateDao {

    public Genre getGenre(String name) {
        return getByNameCaseInsensitive(Genre.class, name);
    }

    @Transactional
    public synchronized void storeNewGenreXML(String name, String targetXml) {
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
        OptionsGenre options = (OptionsGenre) wrapper.getOptions();
        
        SqlScalars sqlScalars = new SqlScalars();
        sqlScalars.addScalar("name", StringType.INSTANCE);

        sqlScalars.addToSql("SELECT DISTINCT ");
        if (options.getFull().booleanValue()) {
            sqlScalars.addToSql("g.id, g.name, ");
            sqlScalars.addToSql("CASE ");
            sqlScalars.addToSql(" WHEN target_api is not null THEN target_api ");
            sqlScalars.addToSql(" WHEN target_xml is not null THEN target_xml ");
            sqlScalars.addToSql(" ELSE name ");
            sqlScalars.addToSql("END as target ");
            
            sqlScalars.addScalar("id", LongType.INSTANCE);
            sqlScalars.addScalar("target", StringType.INSTANCE);
        } else {
            sqlScalars.addToSql("CASE ");
            sqlScalars.addToSql(" WHEN target_api is not null THEN target_api ");
            sqlScalars.addToSql(" WHEN target_xml is not null THEN target_xml ");
            sqlScalars.addToSql(" ELSE name ");
            sqlScalars.addToSql("END as name ");
        }
        sqlScalars.addToSql("FROM genre g ");
        if (options.getUsed() != null && options.getUsed().booleanValue()) {
            sqlScalars.addToSql("WHERE (exists (select 1 from videodata_genres vg where vg.genre_id=id) ");
            sqlScalars.addToSql(" or exists (select 1 from series_genres sg where sg.genre_id=id)) ");
            sqlScalars.addToSql(options.getSearchString(false));
        } else {
            sqlScalars.addToSql(options.getSearchString(true));
        }
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

    public List<Certification> getCertifications(ApiWrapperList<Certification> wrapper) {
        OptionsId options = (OptionsId) wrapper.getOptions();
        
        SqlScalars sqlScalars = new SqlScalars();
        sqlScalars.addToSql("SELECT id, certification_text AS certification, country");
        sqlScalars.addToSql("FROM certification");
        sqlScalars.addToSql(options.getSearchString(true));
        sqlScalars.addToSql(options.getSortString());

        sqlScalars.addScalar("id", LongType.INSTANCE);
        sqlScalars.addScalar("certification", StringType.INSTANCE);
        sqlScalars.addScalar("country", StringType.INSTANCE);

        return executeQueryWithTransform(Certification.class, sqlScalars, wrapper);
    }

    public BoxedSet getBoxedSet(String name) {
        return getByNameCaseInsensitive(BoxedSet.class, name);
    }

    @Transactional
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
        sqlScalars.addToSql("SELECT id, name");
        sqlScalars.addToSql("FROM boxed_set");
        sqlScalars.addToSql(options.getSearchString(true));
        sqlScalars.addToSql(options.getSortString());

        sqlScalars.addScalar("id", LongType.INSTANCE);
        sqlScalars.addScalar("name", StringType.INSTANCE);

        return executeQueryWithTransform(BoxedSet.class, sqlScalars, wrapper);
    }

    public Studio getStudio(String name) {
        return getByNameCaseInsensitive(Studio.class, name);
    }

    @Transactional
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
        OptionsId options = (OptionsId) wrapper.getOptions();
        SqlScalars sqlScalars = new SqlScalars();
        sqlScalars.addToSql("SELECT id, name");
        sqlScalars.addToSql("FROM studio");
        sqlScalars.addToSql(options.getSearchString(true));
        sqlScalars.addToSql(options.getSortString());

        sqlScalars.addScalar("id", LongType.INSTANCE);
        sqlScalars.addScalar("name", StringType.INSTANCE);

        return executeQueryWithTransform(Studio.class, sqlScalars, wrapper);
    }
}
